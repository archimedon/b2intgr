package com.rdnsn.b2intgr;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class RemoteStorageConfiguration {

	@NotNull
	@JsonProperty
	private String accountId;

	@NotNull
	@JsonProperty
	private String authenticationUrl; // "https://api.backblazeb2.com/b2api/v1/b2_authorize_account"

	@NotNull
	@JsonProperty
	private String applicationKey;



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
    // Only used in testing.
    // BucketId and/or Name should be sent in request where required
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

	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
	}

}
