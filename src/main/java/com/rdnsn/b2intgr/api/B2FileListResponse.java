package com.rdnsn.b2intgr.api;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;


@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class B2FileListResponse extends B2ResponseBase {

    @JsonProperty
    private List<B2File> files;

    @JsonProperty
    private String nextFileName;

    public List<B2File> getFiles() {
        return files;
    }

    public B2FileListResponse setFiles(List<B2File> files) {
        this.files = files;
        return this;
    }

    public String getNextFileName() {
        return nextFileName;
    }

    public B2FileListResponse setNextFileName(String nextFileName) {
        this.nextFileName = nextFileName;
        return this;
    }
}

