package com.rdnisn.acrhq;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

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

	
	
	
    
	private static long lastmod = 0;
	//	final private static final long TTL = 10;
	final private static long TTL = 12 * 60 * 58;
	

	
	public boolean isExpired() {
		// TODO Auto-generated method stub
		return false;
	}

	
	
	private boolean noToken() {
		return	
			StringUtils.isBlank(authorizationToken) || ( utcInSecs() - lastmod) >= TTL;
	}
	

	private long utcInSecs() {
		return new Date().getTime() / 1000;
	}


}
