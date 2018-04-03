package com.rdnsn.b2intgr.api;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;


@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListFilesResponse extends AbstractListResponse<B2FileItem> {

    @JsonProperty
    private String bucketId;

    @JsonProperty
    protected String nextFileName;

    @JsonProperty
    protected String nextFileId;

    public ListFilesResponse() {
        super();
    }

    public ListFilesResponse(List<B2FileItem> files) {
        super(files);
    }

    public String getBucketId() {
        return bucketId;
    }

    public ListFilesResponse setBucketId(String bucketId) {
        this.bucketId = bucketId;
        return this;
    }

    public String getNextFileName() {
        return nextFileName;
    }

    public ListFilesResponse setNextFileName(String nextFileName) {
        this.nextFileName = nextFileName;
        return this;
    }

    public String getNextFileId() {
        return nextFileId;
    }

    public ListFilesResponse setNextFileId(String nextFileId) {
        this.nextFileId = nextFileId;
        return this;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }

}

