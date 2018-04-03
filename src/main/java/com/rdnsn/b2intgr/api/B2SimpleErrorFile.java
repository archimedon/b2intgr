package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

@SuppressWarnings("deprecation")
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class B2SimpleErrorFile extends B2ReadsError implements B2BaseFile {

    @JsonProperty
    protected String fileId;

    @JsonProperty
    protected String bucketId;

    @JsonProperty
    protected String fileName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    protected String downloadUrl;


    public B2SimpleErrorFile(){
        super();
    }

    public B2SimpleErrorFile(B2BaseFile file){
        this();
        this.setFileName(file.getFileName())
            .setFileId(file.getFileId())
            .setDownloadUrl(file.getDownloadUrl());
    }

    @Override
    public String getBucketId() {
        return bucketId;
    }

    public B2SimpleErrorFile setBucketId(String bucketId) {
        this.bucketId = bucketId;
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public B2SimpleErrorFile setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        return this;
    }

    public B2SimpleErrorFile setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public B2SimpleErrorFile setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
