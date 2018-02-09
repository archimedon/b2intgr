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
public class ListFilesResponse extends AbstractListResponse {

    @JsonProperty
    protected String nextFileName;

    @JsonProperty
    protected List<B2FileItem> files;


    public ListFilesResponse() {
        super();
    }

    public ListFilesResponse(List<B2FileItem> files) {
        this();
        setFiles(files);
    }


    public String getNextFileName() {
        return nextFileName;
    }


    public List<B2FileItem> getFiles() {
        return files;
    }

    public ListFilesResponse setFiles(List<B2FileItem> files) {
//        if (files !=null &&  ! (files.get(0) instanceof B2FileItem) ) {
//            this.files = files.stream().map(g -> new B2FileItem(g)).collect(Collectors.toList());
//        }
//        else {
//        }
        this.files = files;

        return this;
    }

    public ListFilesResponse setNextFileName(String nextFileName) {
        this.nextFileName = nextFileName;
        return this;
    }
}

