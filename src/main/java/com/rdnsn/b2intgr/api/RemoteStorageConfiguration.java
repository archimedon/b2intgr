package com.rdnsn.b2intgr.api;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize()
public class RemoteStorageConfiguration {
	
	public static final Pattern httpPattern = Pattern.compile("(https{0,1})(.+)");

	@NotNull
	@JsonProperty
	private String accountId = "a374f8e3e263";

	@NotNull
	@JsonProperty
	private String authenticationUrl = "https://api.backblazeb2.com/b2api/v1/b2_authorize_account";

	@NotNull
	@JsonProperty
	private String applicationKey = "0012091458045a46b01b14df849c659aebb820a53c";

	@NotNull
	@JsonProperty
	private Map<String, String> bucket = new HashMap<String, String>();


	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getAuthenticationUrl() {
		return authenticationUrl;
	}

	public void setAuthenticationUrl(String authenticationUrl) {
		this.authenticationUrl = authenticationUrl;
	}

	public String getApplicationKey() {
		return applicationKey;
	}

	public void setApplicationKey(String applicationKey) {
		this.applicationKey = applicationKey;
	}

	public Map<String, String> getBucket() {
		return bucket;
	}

	public void setBucket(Map<String, String> bucket) {
		this.bucket = bucket;
	}

	public String getBucketName() {
		return this.bucket.get("name");
	}

	public String getBucketId() {
		return this.bucket.get("id");
	}
	
	public static String getHttp4Proto(String url) {
		String str = url;
		Matcher m = httpPattern.matcher(url);
		if (m.find()) {
			str = m.replaceFirst("$1" + "4$2"); 
		}
		return str;
	}
	
	public static String http4Suffix(String url) {
		return url + "?okStatusCodeRange=100-999&throwExceptionOnFailure=true"
		+ "&disableStreamCache=false";
//		+ "&transferException=false&"
//		+ "useSystemProperties=true";
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
