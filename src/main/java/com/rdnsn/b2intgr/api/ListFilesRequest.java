package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListFilesRequest {

    @JsonProperty
    private Integer maxFileCount = 100;

    @JsonProperty
    private String prefix = "";

    @JsonProperty
    private String delimiter = "/";

    @JsonProperty
    private String startFileName = null;

    @JsonProperty
    private String bucketId;

    public ListFilesRequest() {
        super();
    }

    public Integer getMaxFileCount() {
        return maxFileCount;
    }

    public ListFilesRequest setMaxFileCount(Integer maxFileCount) {
        this.maxFileCount = maxFileCount;
        return this;
    }

    public String getPrefix() {
        return prefix;
    }

    public ListFilesRequest setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public ListFilesRequest setDelimiter(String delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    public String getStartFileName() {
        return startFileName;
    }

    public ListFilesRequest setStartFileName(String startFileName) {
        this.startFileName = startFileName;
        return this;
    }

    public String getBucketId() {
        return bucketId;
    }

    public ListFilesRequest setBucketId(String bucketId) {
        this.bucketId = bucketId;
        return this;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }

}

