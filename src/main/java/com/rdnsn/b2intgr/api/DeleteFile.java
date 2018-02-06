package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
 public class DeleteFile {

    @JsonProperty
    private String fileId; // "4_h4a48fe8875c6214145260818_f000000000000472a_d20140104_m032022_c001_v0000123_t0104"

    @JsonProperty
    private String fileName;

    public String getFileId() {
        return fileId;
    }

    public DeleteFile setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public DeleteFile setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
