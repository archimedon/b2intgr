package com.rdnsn.b2intgr.api;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractListResponse extends B2ReadsError implements ReadsError {

    protected Function<B2BaseFile, String> makeDownloadUrl = x -> x.getDownloadUrl();


    public abstract List<? extends B2BaseFile> getFiles();

    public <T extends AbstractListResponse> T setMakeDownloadUrl(Function<B2BaseFile, String> makeDownloadUrl) {
        this.makeDownloadUrl = makeDownloadUrl;
        getFiles().forEach(fdat -> fdat.setDownloadUrl(makeDownloadUrl.apply(fdat)));
        return (T) this;
    }
}

