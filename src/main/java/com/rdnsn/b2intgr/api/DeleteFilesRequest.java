package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.List;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteFilesRequest {

    @JsonProperty
    private List<DeleteFile> files;

    public List<DeleteFile> getFiles() {
        return files;
    }

    public DeleteFilesRequest setFiles(List<DeleteFile> files) {
        this.files = files;
        return this;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
