package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.Map;


@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class B2File {

    @JsonProperty
    private String contentType; // "image/jpeg"

    @JsonProperty
    private String action; // folder | upload - where 'upload' indicates it's a file

    @JsonProperty
    private String fileId;

    // @see Setter
    private String author;

    @JsonProperty
    private String fileName;

    @JsonProperty
    private long size;

    @JsonProperty
    private long uploadTimestamp;

    public String getContentType() {
        return contentType;
    }

    public B2File setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public String getAction() {
        return action;
    }

    public B2File setAction(String action) {
        this.action = action;
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public B2File setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public String getFileName() {
        return fileName;
    }

    public B2File setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public long getSize() {
        return size;
    }

    public B2File setSize(long size) {
        this.size = size;
        return this;
    }

    public long getUploadTimestamp() {
        return uploadTimestamp;
    }

    public B2File setUploadTimestamp(long uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
        return this;
    }

    // "fileInfo": { "author": "unknown"},
    @JsonProperty(value="fileInfo", access=JsonProperty.Access.WRITE_ONLY)
    public B2File setAuthor(Map<String, String> fileInfo) {
        if (fileInfo != null) {
            this.author = fileInfo.get("author");
        }
        return this;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
