package com.rdnsn.b2intgr.api;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponse {

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

    private long lastmod = 0;

    public Integer getAbsoluteMinimumPartSize() {
        return absoluteMinimumPartSize;
    }

    public void setAbsoluteMinimumPartSize(Integer absoluteMinimumPartSize) {
        this.absoluteMinimumPartSize = absoluteMinimumPartSize;
    }

    public AuthResponse() {
        this.setLastmod(new Date().getTime() / 1000);
    }

    public long getLastmod() {  return lastmod; }

    private void setLastmod(long lastmod) { this.lastmod = lastmod; }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getApiUrl() { return apiUrl; }

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

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
