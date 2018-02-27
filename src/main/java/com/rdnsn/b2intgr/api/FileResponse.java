package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileResponse extends B2SimpleErrorFile implements ReadsError, B2BaseFile {

    public FileResponse() {
        super();
    }

    public FileResponse(B2BaseFile fdat) {
        this();
        this.setFileId(fdat.getFileId())
            .setFileName(fdat.getFileName())
            .setDownloadUrl(fdat.getDownloadUrl());
    }
}
