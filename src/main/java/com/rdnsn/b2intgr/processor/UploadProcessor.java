package com.rdnsn.b2intgr.processor;

import static com.rdnsn.b2intgr.route.ZRouteBuilder.getHttp4Proto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

import com.rdnsn.b2intgr.api.ErrorObject;
import com.rdnsn.b2intgr.exception.B2BadRequestException;
import com.rdnsn.b2intgr.exception.UploadException;
import com.rdnsn.b2intgr.util.JsonHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.rdnsn.b2intgr.CloudFSConfiguration;
import com.rdnsn.b2intgr.util.Constants;
import com.rdnsn.b2intgr.api.AuthResponse;
import com.rdnsn.b2intgr.api.GetUploadUrlResponse;
import com.rdnsn.b2intgr.api.UploadFileResponse;
import com.rdnsn.b2intgr.model.UserFile;
import com.rdnsn.b2intgr.route.ZRouteBuilder;



public class UploadProcessor implements Processor {

	protected static final Logger log = LoggerFactory.getLogger(UploadProcessor.class);
	
	private final ObjectMapper objectMapper;
	private final CloudFSConfiguration serviceConfig;
//	private final String bucketMap;
    
	public UploadProcessor(CloudFSConfiguration serviceConfig, ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.serviceConfig = serviceConfig;
	}

    private GetUploadUrlResponse doPreamble(final ProducerTemplate producer, AuthResponse remoteAuth, final String buckectId) throws IOException {


        return objectMapper.readValue(
            producer.send( getHttp4Proto(remoteAuth.resolveGetUploadUrl()) + ZRouteBuilder.HTTP4_PARAMS, (Exchange exchange) -> {
                exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
                exchange.getIn().setHeader(Constants.AUTHORIZATION, remoteAuth.getAuthorizationToken());
                exchange.getIn().setBody(JsonHelper.objectToString(objectMapper, ImmutableMap.<String, String>of("bucketId", buckectId)));
            }).getOut().getBody(String.class),
            GetUploadUrlResponse.class);
    }

	@Override
	public void process(Exchange exchange) throws B2BadRequestException, UploadException {

        final UserFile userFile = exchange.getIn().getBody(UserFile.class);
		final AuthResponse remoteAuth = exchange.getIn().getHeader(Constants.AUTH_RESPONSE, AuthResponse.class);

		final ProducerTemplate producer = exchange.getContext().createProducerTemplate();
		final GetUploadUrlResponse uploadAuth;
        try {
            uploadAuth = doPreamble(producer, remoteAuth, userFile.getBucketId());
        } catch (Exception e) {
            throw ZRouteBuilder.makeBadRequestException(e, exchange, "Problems receiving UploadAuthorization" , 403);
        }

        final File file = Paths.get(userFile.getFilepath()).toFile();

        String sha1 = JsonHelper.sha1(file);

        userFile.setSha1(sha1);

        if (log.isDebugEnabled()) {
            Integer ctr = null;

            if (null == (ctr = exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, Integer.class))) {
                ctr = 0;
            }

            log.debug("Redelivery counter: " + ctr);

            if (Pattern.matches("^\\d{4}.+?\\..+", file.getName())
                && ctr < serviceConfig.getMaximumRedeliveries() - 1
            ) {
                sha1 = sha1 + 'e';
                userFile.setSha1(sha1);
                log.debug("pattern matches: '{}'", file.getName());
                log.debug("Flipped '{}' sha: {}", file.getName(), userFile.getSha1());
            }
        }

        final Message responseOut = producer.send(getHttp4Proto(uploadAuth.getUploadUrl()) + "?throwExceptionOnFailure=false&okStatusCodeRange=100", innerExchg -> {
            innerExchg.getIn().setHeaders(buildParams(userFile, uploadAuth.getAuthorizationToken()));
            innerExchg.getIn().setBody(file);
        }).getOut();

        final Integer code = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);

		log.info("HTTP_RESPONSE_CODE:{ '{}' XBzFileName: '{}'}", code, userFile.getRelativePath());

		if (code != null && HttpStatus.SC_OK == code) {
			final String downloadUrl =  String.format("%s/file/%s/%s",
                remoteAuth.getDownloadUrl(),
                    // TODO: Change to bucketName
                    userFile.getBucketId(),
                userFile.getRelativePath());

			log.info("Completed: '{}'", downloadUrl);

            UploadFileResponse uploadResponse = coerceClass(responseOut, UploadFileResponse.class);

            log.debug("uploadResponse: {}", uploadResponse);

            uploadResponse.setDownloadUrl(downloadUrl);
            exchange.getOut().copyFromWithNewBody(responseOut, uploadResponse);
            exchange.getOut().setHeader(Constants.USER_FILE, userFile);

		}
		else {
            ErrorObject errorObject =  coerceClass(responseOut, ErrorObject.class);
            log.error("errorObject: {} ", errorObject);
            userFile.setError(errorObject);
            throw new UploadException("Response code fail (" + code + ") File '" + userFile.getRelativePath() +"' not uploaded" );
		}
	}

    public <T> T coerceClass(Message rsrcIn, Class<T> type) {
		return JsonHelper.coerceClass(objectMapper, rsrcIn, type);
    }

    private Map<String, Object> buildParams(UserFile userFile, String authtoken) {

        return ImmutableMap.<String, Object> builder()
            .put(Exchange.HTTP_METHOD, HttpMethods.POST)
            .put(Constants.X_BZ_FILE_NAME, userFile.getRelativePath())
            .put(Constants.X_BZ_CONTENT_SHA1, userFile.getSha1())
            .put(Exchange.CONTENT_LENGTH, Long.toString(userFile.getSize()))
            .put(Exchange.CONTENT_TYPE, userFile.getContentType())
            .put(Constants.AUTHORIZATION, authtoken)
            .put(Constants.X_BZ_INFO_AUTHOR, userFile.getAuthor())
            .build();
    }

	/**
	 * Only used in testing. To force an error response from Backblaze.
	 * Triggered by file names that start with 4 numbers
	 *
	 * @param sha1
	 * @param exchange
	 * @param file
	 */
	private void corruptAHash(String sha1, Exchange exchange, File file) {
		int ctr = exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, Integer.class);
		log.info("Redelivery counter: " + ctr);

		if (Pattern.matches("^[\\d{3}\\d+].*" , file.getName())
				&& ctr < serviceConfig.getMaximumRedeliveries() - 1)
		{
			sha1 = sha1 + 'e';
			log.info("Flipped it: {}", sha1);
		}
	}
}
