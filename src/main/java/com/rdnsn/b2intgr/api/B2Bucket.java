package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;
import java.util.Map;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class B2Bucket {

    @JsonProperty
	String accountId;

    @JsonProperty
	String bucketId;

    @JsonProperty
	Map bucketInfo;

    @JsonProperty
	String bucketName;

    @JsonProperty
	String bucketType;

    @JsonProperty
    List<String> corsRules;

    @JsonProperty
    List<String> lifecycleRules;

    @JsonProperty
	int revision;

    public B2Bucket() {  }

    public String getAccountId() {
        return accountId;
    }

    public B2Bucket setAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public String getBucketId() {
        return bucketId;
    }

    public B2Bucket setBucketId(String bucketId) {
        this.bucketId = bucketId;
        return this;
    }

    public Map getBucketInfo() {
        return bucketInfo;
    }

    public B2Bucket setBucketInfo(Map bucketInfo) {
        this.bucketInfo = bucketInfo;
        return this;
    }

    public String getBucketName() {
        return bucketName;
    }

    public B2Bucket setBucketName(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    public String getBucketType() {
        return bucketType;
    }

    public B2Bucket setBucketType(String bucketType) {
        this.bucketType = bucketType;
        return this;
    }

    public List<String> getCorsRules() {
        return corsRules;
    }

    public B2Bucket setCorsRules(List<String> corsRules) {
        this.corsRules = corsRules;
        return this;
    }

    public List<String> getLifecycleRules() {
        return lifecycleRules;
    }

    public B2Bucket setLifecycleRules(List<String> lifecycleRules) {
        this.lifecycleRules = lifecycleRules;
        return this;
    }

    public int getRevision() {
        return revision;
    }

    public B2Bucket setRevision(int revision) {
        this.revision = revision;
        return this;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }
}
