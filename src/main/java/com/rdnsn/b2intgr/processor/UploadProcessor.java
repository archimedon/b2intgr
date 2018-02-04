package com.rdnsn.b2intgr.processor;

import static com.rdnsn.b2intgr.RemoteStorageConfiguration.getHttp4Proto;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.rdnsn.b2intgr.CloudFSConfiguration;
import com.rdnsn.b2intgr.Constants;
import com.rdnsn.b2intgr.api.AuthResponse;
import com.rdnsn.b2intgr.api.GetUploadUrlResponse;
import com.rdnsn.b2intgr.api.UploadFileResponse;
import com.rdnsn.b2intgr.model.UserFile;
import com.rdnsn.b2intgr.route.ZRouteBuilder;



public class UploadProcessor extends BaseProcessor {

	protected static final Logger log = LoggerFactory.getLogger(UploadProcessor.class);
	
	private final ObjectMapper objectMapper;
	private final CloudFSConfiguration serviceConfig;
	private String bucketMap;
    
	public UploadProcessor(CloudFSConfiguration serviceConfig, ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.serviceConfig = serviceConfig;
		try {
			this.bucketMap = objectMapper.writeValueAsString(ImmutableMap.of("bucketId", serviceConfig.getRemoteBucketId()));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	private GetUploadUrlResponse doPreamble(final ProducerTemplate producer, String authdUploadUrl, final String authtoken) throws JsonParseException, JsonMappingException, IOException {

		return objectMapper.readValue(producer.send(getHttp4Proto(authdUploadUrl) + ZRouteBuilder.HTTP4_PARAMS, innerExchg -> {
			innerExchg.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
			innerExchg.getIn().setHeader(Constants.AUTHORIZATION, authtoken);
			innerExchg.getIn().setBody(this.bucketMap);
		}).getOut().getBody(String.class), GetUploadUrlResponse.class);				
	}
	
	@Override
	public void process(Exchange exchange) throws Exception {
		
		final UserFile userFile = exchange.getIn().getBody(UserFile.class);
		final AuthResponse remoteAuth = exchange.getIn().getHeader(Constants.AUTH_RESPONSE, AuthResponse.class);

		final ProducerTemplate producer = exchange.getContext().createProducerTemplate();
		final GetUploadUrlResponse uploadAuth =
				doPreamble(producer, remoteAuth.resolveGetUploadUrl(), remoteAuth.getAuthorizationToken());
		
		final String authdUploadUrl =
				getHttp4Proto(uploadAuth.getUploadUrl()) + ZRouteBuilder.HTTP4_PARAMS;
		
		final File file = userFile.getFilepath().toFile();
		
		final Message responseOut = producer.send(authdUploadUrl, innerExchg -> {
			final Message postMessage = innerExchg.getIn();
			postMessage.setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
			
			log.info("\"Upload\":{ \"{}\": \"{}\"}", Constants.X_BZ_FILE_NAME, userFile.getName());
			
			postMessage.setHeader(Constants.X_BZ_FILE_NAME, userFile.getName());
			
			String sha1 = sha1(file);
//			if (log.isDebugEnabled()) {
//				corruptSomeHashes(sha1, exchange, file);
//			}
			postMessage.setHeader(Constants.X_BZ_CONTENT_SHA1, sha1);
			postMessage.setHeader(Exchange.CONTENT_LENGTH, file.length() + "");
			postMessage.setHeader(Exchange.CONTENT_TYPE, userFile.getContentType());
			postMessage.setHeader(Constants.AUTHORIZATION, uploadAuth.getAuthorizationToken());
			postMessage.setHeader(Constants.X_BZ_INFO_AUTHOR, "unknown");
			postMessage.setBody(file);
		}).getOut();
		
		producer.stop();
		final Integer code = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
		final String response = responseOut.getBody(String.class);

		log.info("HTTP_RESPONSE_CODE: '{}' XBzFileName: '{}'", code, userFile.getName());

		if (HttpStatus.SC_OK == code) {
			final String downloadUrl =  String.format("%s/file/%s/%s",
					remoteAuth.getDownloadUrl(), serviceConfig.getRemoteStorageConf().getBucketName(), userFile.getName());
			
			log.info("Completed: '{}'", downloadUrl);

			try {
				UploadFileResponse uploadResponse = objectMapper.readValue(response, UploadFileResponse.class);
				exchange.getOut().copyFromWithNewBody(responseOut, ImmutableList.of(BeanUtils.describe(uploadResponse)));
				exchange.getOut().setHeader(Constants.DOWNLOAD_URL, downloadUrl);
			} catch (Exception e) {
				throw new UploadException(e);
			}
		}
		else {
			throw new UploadException("Response code fail (" + code + ") File '" + userFile.getName() +"' not uploaded" );
		}
	}

	/**
	 * Only used in testing. To force an error response from Backblaze.
	 * Triggered by file names that start with 4 numbers
	 *
	 * @param sha1
	 * @param exchange
	 * @param file
	 */
	private void corruptSomeHashes(String sha1, Exchange exchange, File file) {
		
		if (Pattern.matches("^[\\d{3}\\d+].*" , file.getName())
				&& exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, Integer.class) < 1)
		{
			sha1 = sha1 + 'e';
		}
	}

	private String sha1(final File file) {
		String ans = null;
		try {
			final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");

			try(InputStream is = new BufferedInputStream(new FileInputStream(file))) {
				final byte[] buffer = new byte[1024];
				for (int read = 0; (read = is.read(buffer)) != -1;) {
					messageDigest.update(buffer, 0, read);
				}                                                                                                         
			}
			// Convert the byte to hex format
			try (Formatter formatter = new Formatter()) {
				for (final byte b : messageDigest.digest()) {
					formatter.format("%02x", b);
				}
				ans = formatter.toString();
			}
		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return ans;
	}
}