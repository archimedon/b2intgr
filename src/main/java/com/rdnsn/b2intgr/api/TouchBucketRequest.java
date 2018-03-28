package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;


@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class TouchBucketRequest  {

    @JsonProperty
    private String accountId;

    @JsonProperty
    private String bucketId;

    @JsonProperty
    private String bucketType;

    public TouchBucketRequest() {

    }

    public String getAccountId() {
        return accountId;
    }

    public TouchBucketRequest setAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public String getBucketId() {
        return bucketId;
    }

    public TouchBucketRequest setBucketId(String bucketId) {
        this.bucketId = bucketId;
        return this;
    }

    public String getBucketType() {
        return bucketType;
    }

    public TouchBucketRequest setBucketType(String bucketType) {
        this.bucketType = bucketType;
        return this;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }

}
