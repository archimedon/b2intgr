package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractListResponse extends B2ReadsError implements ReadsError {

//    @JsonProperty
//    protected List<? extends B2BaseFile> files;

    protected Function<B2BaseFile, String> makeDownloadUrl = x -> x.getDownloadUrl();


    public abstract List<? extends B2BaseFile> getFiles();

    public <T extends AbstractListResponse> T setMakeDownloadUrl(Function<B2BaseFile, String> makeDownloadUrl) {
        this.makeDownloadUrl = makeDownloadUrl;
        getFiles().forEach(fdat -> fdat.setDownloadUrl(makeDownloadUrl.apply(fdat)));
        return (T) this;
    }
}

