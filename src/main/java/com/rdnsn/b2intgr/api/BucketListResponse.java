package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class BucketListResponse {

    @JsonProperty
    protected List<B2Bucket> buckets;


    public BucketListResponse() {

    }

    public List<B2Bucket> getBuckets() {
        return buckets;
    }

    public BucketListResponse setBuckets(List<B2Bucket> buckets) {
        this.buckets = buckets;
        return this;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }

}
