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

    public void setFiles(List<B2File> files) {
        this.files = files;
    }

    public String getNextFileName() {
        return nextFileName;
    }

    public void setNextFileName(String nextFileName) {
        this.nextFileName = nextFileName;
    }
}

