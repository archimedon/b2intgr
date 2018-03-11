package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DirectoryAccessRequest {

    @JsonProperty
	String bucketId;

    @JsonProperty
	String fileNamePrefix;

    @JsonProperty
	String b2ContentDisposition;

    @JsonProperty
	int validDurationInSeconds;

    public DirectoryAccessRequest() {
    }

    public String getBucketId() {
        return bucketId;
    }

    public DirectoryAccessRequest setBucketId(String bucketId) {
        this.bucketId = bucketId;
        return this;
    }

    public String getFileNamePrefix() {
        return fileNamePrefix;
    }

    public DirectoryAccessRequest setFileNamePrefix(String fileNamePrefix) {
        this.fileNamePrefix = fileNamePrefix;
        return this;
    }

    public String getB2ContentDisposition() {
        return b2ContentDisposition;
    }

    public DirectoryAccessRequest setB2ContentDisposition(String b2ContentDisposition) {
        this.b2ContentDisposition = b2ContentDisposition;
        return this;
    }

    public int getValidDurationInSeconds() {
        return validDurationInSeconds;
    }

    public DirectoryAccessRequest setValidDurationInSeconds(int validDurationInSeconds) {
        this.validDurationInSeconds = validDurationInSeconds;
        return this;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }
}
