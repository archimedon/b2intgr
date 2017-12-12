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
	private Integer absoluteMinimumPartSize;
	
	@JsonProperty
	private String apiUrl;
	
	@JsonProperty
	private String authorizationToken;
	
	@JsonProperty
	private String downloadUrl;
	
	@JsonProperty
	private Integer minimumPartSize;
	
	@JsonProperty
	private Integer recommendedPartSize;

	
	public B2Response() {}
	
	public String toString () {
		return "Code: %s, Status: %s, message %s".format(code, status.toString(), message);
	}
	
	public String getCode() {
		return code;
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
