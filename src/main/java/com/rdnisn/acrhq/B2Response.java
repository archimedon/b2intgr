package com.rdnisn.acrhq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class B2Response {
	
	@JsonProperty
	private String code;
	
	@JsonProperty
	private Integer status;
	
	@JsonProperty
	private String message;
	
	@JsonProperty
	private String accountId;
	
	@JsonProperty
	private String bucketId;
	
	@JsonProperty
	private Integer absoluteMinimumPartSize;
	
	@JsonProperty
	private String apiUrl;
	
	@JsonProperty
	private String authorizationToken;
	
	@JsonProperty
	private String downloadUrl;
	
	@JsonProperty
	private String uploadUrl;
	
	@JsonProperty
	private Integer minimumPartSize;
	
	@JsonProperty
	private Integer recommendedPartSize;

	
	public B2Response() {}
	
	public String toString () {
		return "B2Response:\n"
				+ "apiUrl: " + apiUrl + "\n" 
				+ "downloadUrl: " + downloadUrl + "\n" 
				+ "uploadUrl: " + uploadUrl + "\n" 
				+ "bucketId: " + bucketId + "\n" 
				+ "authorizationToken: " + authorizationToken + "\n" 
				+ "accountId: " + accountId + "\n" 
				+ "Code: " + code;
	}
	
	public String getCode() {
		return code;
	}

	public String getBucketId() {
		return bucketId;
	}

	public void setBucketId(String bucketId) {
		this.bucketId = bucketId;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getAccountId() {
		return accountId;
	}
	
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	
	public Integer getAbsoluteMinimumPartSize() {
		return absoluteMinimumPartSize;
	}
	
	public void setAbsoluteMinimumPartSize(Integer absoluteMinimumPartSize) {
		this.absoluteMinimumPartSize = absoluteMinimumPartSize;
	}
	
	public String getApiUrl() {
		return apiUrl;
	}
	
	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}
	
	public String getAuthorizationToken() {
		return authorizationToken;
	}
	
	public void setAuthorizationToken(String authorizationToken) {
		this.authorizationToken = authorizationToken;
	}
	
	public String getUploadUrl() {
		return uploadUrl;
	}

	public void setUploadUrl(String uploadUrl) {
		this.uploadUrl = uploadUrl;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}
	
	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}
	
	public Integer getMinimumPartSize() {
		return minimumPartSize;
	}
	
	public void setMinimumPartSize(Integer minimumPartSize) {
		this.minimumPartSize = minimumPartSize;
	}
	
	public Integer getRecommendedPartSize() {
		return recommendedPartSize;
	}
	
	public void setRecommendedPartSize(Integer recommendedPartSize) {
		this.recommendedPartSize = recommendedPartSize;
	}
}
