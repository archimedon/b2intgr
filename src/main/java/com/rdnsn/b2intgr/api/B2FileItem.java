package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Map;


@SuppressWarnings("deprecation")
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class B2FileItem extends B2SimpleFile {

    @JsonProperty
    protected String contentType; // "image/jpeg"

    @JsonProperty
    protected String action; // folder | upload - where 'upload' indicates it's a file

    @JsonProperty
    protected Map<String, Object> fileInfo;

    @JsonProperty
    protected long size;

    @JsonProperty
    protected long uploadTimestamp;

    public String getContentType() {
        return contentType;
    }

    public B2FileItem setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public String getAction() {
        return action;
    }

    public B2FileItem setAction(String action) {
        this.action = action;
        return this;
    }

    public long getSize() {
        return size;
    }

    public B2FileItem setSize(long size) {
        this.size = size;
        return this;
    }

    public long getUploadTimestamp() {
        return uploadTimestamp;
    }

    public B2FileItem setUploadTimestamp(long uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
        return this;
    }

    public Map<String, Object> getFileInfo() {
        return fileInfo;
    }

    public B2FileItem setFileInfo(Map<String, Object> fileInfo) {
        this.fileInfo = fileInfo;
        return this;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }

}
