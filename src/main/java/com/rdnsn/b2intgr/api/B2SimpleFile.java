package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class B2SimpleFile implements B2BaseFile {

    @JsonProperty
    protected String fileId;

    @JsonProperty
    protected String fileName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    protected String downloadUrl;


    public B2SimpleFile(){
        super();
    }

    public B2SimpleFile(B2BaseFile file){
        this();
        this.setFileName(file.getFileName())
            .setFileId(file.getFileId())
            .setDownloadUrl(file.getDownloadUrl());
    }

    public String getFileId() {
        return fileId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public B2SimpleFile setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        return this;
    }

    public B2SimpleFile setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public B2SimpleFile setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        B2SimpleFile that = (B2SimpleFile) o;

        return fileId.equals(that.fileId);
    }

    @Override
    public int hashCode() {
        return fileId.hashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }
}
