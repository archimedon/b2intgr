package com.rdnsn.b2intgr.processor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
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

import com.fasterxml.jackson.databind.ObjectMapper;
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
    
	@Override
	public void process(Exchange exchange) {
		final Message IN = exchange.getIn();
		
		UserFile userFile = IN.getHeader("userFile", UserFile.class);
		GetUploadUrlResponse uploadAuth = IN.getHeader("uploadAuth", GetUploadUrlResponse.class);
		AuthResponse remoteAuth = IN.getHeader("remoteAuth", AuthResponse.class);

		final File file = userFile.getFilepath().toFile();
		
		final String remoteFilen = userFile.getName();
		
		log.debug("File-Name: {}", remoteFilen);
		log.debug("Content-Type: {}", userFile.getContentType());
		
		log.debug("Content-Length: {}", file.length());
//		InputStream fin = new FileInputStream(file);
	    
		Message responseOut = exchange.getContext().createProducerTemplate()
		.send(getHttp4Proto(uploadAuth.getUploadUrl()), innerExchg -> {
			final Message httpPost = innerExchg.getIn();
			httpPost.setBody(file);
			httpPost.setHeader("Authorization", uploadAuth.getAuthorizationToken());
			httpPost.setHeader("Content-Type", userFile.getContentType());
			httpPost.setHeader("X-Bz-Content-Sha1", "do_not_verify");
			httpPost.setHeader("X-Bz-File-Name", remoteFilen);
			httpPost.setHeader("X-Bz-Info-Author", "unknown");
			httpPost.setHeader("Content-Length", file.length() + "");
		}).getOut();
		
		try {
			UploadFileResponse uploadResponse = objectMapper.readValue(responseOut.getBody(String.class), UploadFileResponse.class);
			exchange.getOut().copyFromWithNewBody(responseOut, null);
			exchange.getOut().setHeader("userFile", userFile);
			
			String downloadUrl = remoteAuth.getDownloadUrl();
			String url = String.format("%s/file/%s/%s", downloadUrl, serviceConfig.getRemoteStorageConf().getBucketName(), remoteFilen);
			System.err.println(url);
			exchange.getOut().setHeader("downloadUrl", url);
			exchange.getOut().setBody(BeanUtils.describe(uploadResponse));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

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
