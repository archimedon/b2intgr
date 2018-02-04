package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListFilesRequest {

//    @JsonProperty(access=JsonProperty.Access.WRITE_ONLY)
//    //JsonProperty
//    private String nextFileName = null;

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

    public  ListFilesRequest() {
        super();
    }

//    public String getNextFileName() {
//        return nextFileName;
//    }

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }

//    public void setNextFileName(String nextFileName) {
//        this.nextFileName = nextFileName;
//    }

    public String getStartFileName() {
        return startFileName;
    }

    public void setStartFileName(String startFileName) {
        this.startFileName = startFileName;
    }

    public Integer getMaxFileCount() {
        return maxFileCount;
    }

    public void setMaxFileCount(Integer maxFileCount) {
        this.maxFileCount = maxFileCount;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}

