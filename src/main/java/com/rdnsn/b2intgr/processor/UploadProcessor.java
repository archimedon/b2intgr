package com.rdnsn.b2intgr.processor;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.rdnsn.b2intgr.CloudFSConfiguration;
import com.rdnsn.b2intgr.api.AuthResponse;
import com.rdnsn.b2intgr.api.GetUploadUrlResponse;
import com.rdnsn.b2intgr.api.UploadFileResponse;
import com.rdnsn.b2intgr.route.UserFile;

import static com.rdnsn.b2intgr.api.RemoteStorageConfiguration.getHttp4Proto;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.net.HttpURLConnection;
import java.net.URL;



public class UploadProcessor implements Processor {

	protected static final Logger log = LoggerFactory.getLogger(UploadProcessor.class);
	private static final String UTF_8 = StandardCharsets.UTF_8.toString();
	private final ObjectMapper objectMapper;
	private final CloudFSConfiguration serviceConfig;
    
	public UploadProcessor(CloudFSConfiguration serviceConfig, ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.serviceConfig = serviceConfig;
	}
	
	private String makeUploadReqData() {
		String uplReqData = null;
		try {
			uplReqData = objectMapper.writeValueAsString(ImmutableMap.of("bucketId", "2ab327a44f788e635ef20613"));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return uplReqData;
	}

	private GetUploadUrlResponse doPreamble(final ProducerTemplate producer, String authdUploadUrl, final String authtoken) throws JsonParseException, JsonMappingException, IOException {
		final String uploadUrl = getHttp4Proto(authdUploadUrl);
		System.err.println("uploadUrl: " + uploadUrl);

		final String json = producer.send(uploadUrl, innerExchg -> {
			innerExchg.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
			innerExchg.getIn().setHeader(HttpHeaders.AUTHORIZATION, authtoken);
			innerExchg.getIn().setBody(makeUploadReqData());
		}).getOut().getBody(String.class);
		
		System.err.println("json: " + json);

		return objectMapper.readValue(json, GetUploadUrlResponse.class);				
	}
	
	@Override
	public void process(Exchange exchange) throws Exception {
		
		final UserFile userFile = exchange.getIn().getHeader("userFile", UserFile.class);
		final AuthResponse remoteAuth = exchange.getIn().getHeader("remoteAuth", AuthResponse.class);
		final String downloadUrlBase = remoteAuth.getDownloadUrl();

		final ProducerTemplate producer = exchange.getContext().createProducerTemplate();
		
		final GetUploadUrlResponse uploadAuth = doPreamble(producer, remoteAuth.resolveGetUploadUrl(), remoteAuth.getAuthorizationToken());		
		final String authdUploadUrl = getHttp4Proto(uploadAuth.getUploadUrl());
		
		final String XBzFileName = userFile.getName();
		final File file = userFile.getFilepath().toFile();
		
		final String downloadUrl = String.format("%s/file/%s/%s", downloadUrlBase, serviceConfig.getRemoteStorageConf().getBucketName(), XBzFileName);
		String url = authdUploadUrl;// + "?throwExceptionOnFailure=true&disableStreamCache=true&transferException=false";
		
		String composite = "authdUploadUrl: " + authdUploadUrl + "\n";
	
		final Message responseOut = producer.send(url, innerExchg -> {
				final Message postMessage = innerExchg.getIn();
				
				postMessage.setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
				postMessage.setHeader("authdUploadUrl", authdUploadUrl);
				
				postMessage.setHeader("X-Bz-File-Name", XBzFileName);
				String sha1 = sha1(file);
				if (
						Pattern.matches("^[\\dax].*" , file.getName())
						&& exchange.getIn().getHeader("CamelRedeliveryCounter", Integer.class) < 2
					) {
						sha1 = sha1 + 'e';
				}
				postMessage.setHeader("X-Bz-Content-Sha1", sha1);

				postMessage.setHeader(Exchange.CONTENT_LENGTH, file.length() + "");
				postMessage.setHeader(Exchange.CONTENT_TYPE, userFile.getContentType());
				postMessage.setHeader(HttpHeaders.AUTHORIZATION, uploadAuth.getAuthorizationToken());
				postMessage.setHeader("X-Bz-Info-Author", "unknown");
				postMessage.setBody(file);
				System.err.println(composite + "getHeaders: " + postMessage.getHeaders());

//					
//				System.err.println("authdUploadUrl: " + innerExchg.getIn().getHeader("authdUploadUrl"));
//				
//				final String line1 = "Exchange.HTTP_METHOD: " + innerExchg.getIn().getHeader(Exchange.HTTP_METHOD);
//				System.err.println(line1);
//				final String line2 = "Exchange.CONTENT_LENGTH: " + innerExchg.getIn().getHeader(Exchange.CONTENT_LENGTH);
//				System.err.println(line2);
//				
//				final String line3 = "Filen: " + userFile.getFilepath().getFileName();
//				System.err.println(line3);
//				
//				
//				innerExchg.getIn().removeHeader("authdUploadUrl");
//				innerExchg.getIn().removeHeader("remoteAuth");
//				innerExchg.getIn().removeHeader("uploadAuth");
//				innerExchg.getIn().removeHeader("userFile");
			}).getOut();
		
			
//			System.err.println("responseOut.getHeaders: " + responseOut.getHeaders());
			
			Integer code = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
			System.err.println("HTTP_RESPONSE_CODE: " + code + "\nFileName: '" + XBzFileName);
			if (code != null && code == 200) {
				try {
					UploadFileResponse uploadResponse = objectMapper.readValue(responseOut.getBody(String.class), UploadFileResponse.class);
					exchange.getOut().copyFromWithNewBody(responseOut, ImmutableList.of(BeanUtils.describe(uploadResponse)));

					exchange.getOut().setBody( ImmutableList.of(BeanUtils.describe(uploadResponse)));
				} catch (IOException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					throw new UploadException(e);
				}
			}
			else {
				throw new UploadException("Response code not OK (" + code + ") File '" + XBzFileName +"' not uploaded" );
			}
			producer.stop();
	}

	
	
	

	private static String sha1(final File file) {
		String ans = null;
		try {
			final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");

			InputStream is = new BufferedInputStream(new FileInputStream(file));
			final byte[] buffer = new byte[1024];
			for (int read = 0; (read = is.read(buffer)) != -1;) {
				messageDigest.update(buffer, 0, read);
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


	
	
	
	
	
	
	
	
//	
//	
//	postMessage.setHeader("uploadAuth", uploadAuth);
//	// postBody.setHeader("uploadAuthToken", uploadAuth.getAuthorizationToken());
//	postMessage.setHeader("authdUploadUrl", authdUploadUrl);
//	// postBody.setHeader(Exchange.HTTP_URI,
//	// postBody.setHeader(Exchange.HTTP_URI,
//	// uploadAuth.getUploadUrl().replaceFirst("https:", ""));
//	postMessage.setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
//	// postBody.setHeader(Exchange.CONTENT_ENCODING, "gzip" );
//	postMessage.setHeader(Exchange.CONTENT_LENGTH, file.length() + "");
//	postMessage.setHeader(Exchange.CONTENT_TYPE, userFile.getContentType());
//	postMessage.setHeader(HttpHeaders.AUTHORIZATION, uploadAuth.getAuthorizationToken());
//	// postBody.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
//	// postBody.setHeader(Exchange.HTTP_CHARACTER_ENCODING, "iso-8859-1");
//	if (postMessage.getHeader("cnt") == null && Pattern.matches("^[\\dax].*" , file.getName())) {
//		postMessage.setHeader("cnt", Boolean.TRUE);
//		postMessage.setHeader("X-Bz-Content-Sha1", sha1(file) + 'e');
//	} else {
//		postMessage.setHeader("X-Bz-Content-Sha1", sha1(file));
//	}
////	postBody.setHeader("X-Bz-Content-Sha1", sha1(file));
//	postMessage.setHeader("X-Bz-File-Name", remoteFilen);
//	postMessage.setHeader("X-Bz-Info-Author", "unknown");
////	postBody.setBody(file);

	
	
//	.enrich(getHttp4Proto(authAgent.getRemoteAuth().resolveGetUploadUrl()), (original, resource) -> {
//		try {
//			final Message IN = original.getIn();
//			final GetUploadUrlResponse uploadAuth = objectMapper.readValue(resource.getIn().getBody(String.class),
//					GetUploadUrlResponse.class);
//
//			final UserFile userFile = IN.getHeader("userFile", UserFile.class);
//
//			final File file = userFile.getFilepath().toFile();
//			final String remoteFilen = userFile.getName();
//
//			log.debug("File-Name: {}", remoteFilen);
//			log.debug("Content-Type: {}", userFile.getContentType());
//			log.debug("Content-Length: {}", file.length());
//
//			String authdUploadUrl = uploadAuth.getUploadUrl();
//			// String authdUploadUrl = getHttp4Proto("https://www.google.com");
//			log.debug("uploadAuth: {}", authdUploadUrl);
//
//			IN.setHeader("uploadAuth", uploadAuth);
//			// IN.setHeader("uploadAuthToken", uploadAuth.getAuthorizationToken());
//			IN.setHeader("authdUploadUrl", authdUploadUrl);
//			// IN.setHeader(Exchange.HTTP_URI,
//			// IN.setHeader(Exchange.HTTP_URI,
//			// uploadAuth.getUploadUrl().replaceFirst("https:", ""));
//			IN.setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
//			// IN.setHeader(Exchange.CONTENT_ENCODING, "gzip" );
//			IN.setHeader(Exchange.CONTENT_LENGTH, file.length() + "");
//			IN.setHeader(Exchange.CONTENT_TYPE, userFile.getContentType());
//			IN.setHeader(HttpHeaders.AUTHORIZATION, uploadAuth.getAuthorizationToken());
//			// IN.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
//			// IN.setHeader(Exchange.HTTP_CHARACTER_ENCODING, "iso-8859-1");
//			if (IN.getHeader("cnt") == null && Pattern.matches("^[\\dax].*" , file.getName())) {
//				IN.setHeader("cnt", Boolean.TRUE);
//				IN.setHeader("X-Bz-Content-Sha1", sha1(file) + 'e');
//			} else {
//				IN.setHeader("X-Bz-Content-Sha1", sha1(file));
//			}
////			IN.setHeader("X-Bz-Content-Sha1", sha1(file));
//			IN.setHeader("X-Bz-File-Name", remoteFilen);
//			IN.setHeader("X-Bz-Info-Author", "unknown");
//			IN.setBody(file);
//		} catch (IOException e) {
//			if (original.getPattern().isOutCapable()) {
//				original.getOut().setBody(e);
//			}
//		}
//		return original;
//}

	
	
	
	
	
	
	
	
	
//	private String composeDownloadUrl(AuthResponse remoteAuth, UploadFileResponse uploadResponse) {
//		return String.format("%s/file/%s/%s", remoteAuth.getDownloadUrl(), serviceConfig.getRemoteStorageConf().getBucketName(), uploadResponse.getFileName());
//	}
//
//
//	static public String myInputStreamReader(InputStream in) throws IOException {
//	    InputStreamReader reader = new InputStreamReader(in);
//	    StringBuilder sb = new StringBuilder();
//	    int c = reader.read();
//	    while (c != -1) {
//	        sb.append((char)c);
//	        c = reader.read();
//	    }
//	    reader.close();
//	    return sb.toString();
//	}
}
//String uploadUrl = ""; // Provided by b2_get_upload_url
//String uploadAuthorizationToken = ""; // Provided by b2_get_upload_url
//String fileName = ""; // The name of the file you are uploading
//String contentType = ""; // The content type of the file
//String sha1 = ""; // SHA1 of the file you are uploading
//byte[] fileData;
//HttpURLConnection connection = null;
//String json = null;
//try {
//    URL url = new URL(uploadUrl);
//    connection = (HttpURLConnection)url.openConnection();
//    connection.setRequestMethod("POST");
//    connection.setRequestProperty("Authorization", uploadAuthorizationToken);
//    connection.setRequestProperty("Content-Type", contentType);
//    connection.setRequestProperty("X-Bz-File-Name", fileName);
//    connection.setRequestProperty("X-Bz-Content-Sha1", sha1);
//    connection.setDoOutput(true);
//    DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
//    writer.write(fileData);
//    String jsonResponse = myInputStreamReader(connection.getInputStream());
//    System.out.println(jsonResponse);
//} catch (Exception e) {
//    e.printStackTrace();
//} finally {
//    connection.disconnect();
//}
