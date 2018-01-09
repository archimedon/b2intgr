package com.rdnsn.b2intgr;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.Header;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/*
request.setHeader("Authorization", OperationAuthToken);
request.setHeader("Content-Type", "image/jpeg");
// request.setHeader("Content-Length", upfile.length() + "");
request.setHeader("X-Bz-Content-Sha1", sha1(userFile));
request.setHeader("X-Bz-File-Name", userFile.getName());
*/
public class MultipartAgent {

	final private String theUrl;
	final private Map<String, String> headers;
	
	public MultipartAgent(String theUrl, Map<String, String> headers) {
		this.theUrl = theUrl;
		this.headers = headers;
	}

	public String doPost(final File file) {
		
		String reply = null;
		
		final HttpPost request = new HttpPost(this.theUrl);
		if (file != null) {
			request.setEntity(
			  MultipartEntityBuilder.create().addBinaryBody(file.getName(), file).build()
			);
		}
		if (headers != null) {
			headers.entrySet().forEach(ent -> request.setHeader(ent.getKey(), ent.getValue()));
		}
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			
			HttpResponse response = httpclient.execute(request);
			java.io.ByteArrayOutputStream buf = new ByteArrayOutputStream();
			response.getEntity().writeTo(buf);
			reply = buf.toString("UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return reply;
	}
	
	public HttpResponse doRawPost(final File file) {
		
		HttpResponse reply = null;
		
		final HttpPost request = new HttpPost(this.theUrl);
		if (file != null) {
			request.setEntity(
					MultipartEntityBuilder.create().addBinaryBody(file.getName(), file).build()
					);
		}
		if (headers != null) {
			headers.entrySet().forEach(ent -> request.setHeader(ent.getKey(), ent.getValue()));
		}
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			
			reply = httpclient.execute(request);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return reply;
	}


	public String doGet() {
		String reply = null;
		
		final HttpGet request = new HttpGet(this.theUrl);
		if (headers != null) {
			headers.entrySet().forEach(ent -> request.setHeader(ent.getKey(), ent.getValue()));
		}
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			
			HttpResponse response = httpclient.execute(request);
			java.io.ByteArrayOutputStream buf = new ByteArrayOutputStream();
			response.getEntity().writeTo(buf);
			reply = buf.toString("UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return reply;
	}

	public void addFile(File userFile) {
		
	}
	
	public static String sha1(final File file) throws NoSuchAlgorithmException, IOException {
		final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");

		try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
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
			return formatter.toString();
		}
	}

}
