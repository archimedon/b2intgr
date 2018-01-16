package com.rdnsn.b2intgr.api;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponse extends B2ResponseBase {
	
	@JsonProperty
	private Integer absoluteMinimumPartSize;
		
	@JsonProperty
	private String accountId;
	
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

	public Integer getAbsoluteMinimumPartSize() {
		return absoluteMinimumPartSize;
	}

	public void setAbsoluteMinimumPartSize(Integer absoluteMinimumPartSize) {
		this.absoluteMinimumPartSize = absoluteMinimumPartSize;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
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

	public String resolveGetUploadUrl() {
		return apiUrl + "/b2api/v1/b2_get_upload_url";
	}
}