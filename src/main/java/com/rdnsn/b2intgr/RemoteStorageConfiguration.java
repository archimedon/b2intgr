package com.rdnsn.b2intgr;

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

	@NotNull
	@JsonProperty
	private String accountId = "a374f8e3e263";

	@NotNull
	@JsonProperty
	private String authenticationUrl = "https://api.backblazeb2.com/b2api/v1/b2_authorize_account";

	@NotNull
	@JsonProperty
	private String applicationKey = "0012091458045a46b01b14df849c659aebb820a53c";

	
	
	/**
	 * "buckets": [
    {
        "accountId": "30f20426f0b1",
        "bucketId": "4a48fe8875c6214145260818",
        "bucketInfo": {},
        "bucketName" : "Kitten-Videos",
        "bucketType": "allPrivate",
        "lifecycleRules": []
    }, ... ]
	 */
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
		return this.bucket.get("bucketName");
	}

	public String getBucketId() {
		return this.bucket.get("bucketId");
	}
	
}
