package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteFilesResponse extends AbstractListResponse<FileResponse> {


    public DeleteFilesResponse() {
        super();
    }

    public DeleteFilesResponse(List<FileResponse> files) {
        super(files);
        super.beenRun = true;
    }

    @JsonIgnore
    public DeleteFileResponse getFile(String fileId) {
        int idx = (files == null)
            ? -1 : files.indexOf(new FileResponse().setFileId(fileId));

        return (idx > 0)
            ? (DeleteFileResponse) files.get(idx) : null;
    }

    public DeleteFilesResponse updateFile(FileResponse file) {
        if (files == null)
            files = new ArrayList();

        int idx = files.indexOf(file);

        if (idx > 0)
            files.set(idx, file);
        else
            files.add(file);

        return this;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
