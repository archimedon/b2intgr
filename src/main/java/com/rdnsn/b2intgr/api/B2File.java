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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getAuthor() {
        return author;
    }

    // "fileInfo": { "author": "unknown"},
    @JsonProperty(value="fileInfo", access=JsonProperty.Access.WRITE_ONLY)
    public void setAuthor(Map<String, String> fileInfo) {
        if (fileInfo != null) {
            this.author = fileInfo.get("author");
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(long uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
