package com.rdnsn.b2intgr.api;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;


@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListFilesResponse extends AbstractListResponse<B2FileItem> {

    @JsonProperty
    protected String nextFileName;

    public ListFilesResponse() {
        super();
    }

    public ListFilesResponse(List<B2FileItem> files) {
        super(files);
    }

    public String getNextFileName() {
        return nextFileName;
    }

//
//    public List<B2FileItem> getFiles()
//    {
//        return (List<B2FileItem>) super.getFiles();
//    }
//
    public ListFilesResponse setNextFileName(String nextFileName) {
        this.nextFileName = nextFileName;
        return this;
    }
}

