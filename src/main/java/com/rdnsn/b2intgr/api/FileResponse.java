package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileResponse extends B2SimpleErrorFile implements ReadsError, B2BaseFile {


    public FileResponse(B2BaseFile fdat) {
        this();
        this.setFileId(fdat.getFileId())
            .setFileName(fdat.getFileName())
            .setDownloadUrl(fdat.getDownloadUrl());
    }

    public FileResponse() {
        super();
    }

//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    public ErrorObject getError() {
//        return error;
//    }

//    public FileResponse setError(ErrorObject error) {
//        this.error = error;
//        return this;
//    }


}
