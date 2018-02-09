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
public class DeleteFilesResponse extends AbstractListResponse {

    @JsonIgnore
    final private Map<String, FileResponse> fileAT;


    public DeleteFilesResponse() {
        super();
        this.fileAT = new HashMap<String, FileResponse>();
    }

    public DeleteFilesResponse(List<? extends B2BaseFile> files) {
        this();
        this.setFiles(files);
    }

    @JsonGetter(value = "files")
    public List<B2SimpleFile> getFiles() {
        return new ArrayList(fileAT.values());
    }

    @JsonSetter(value = "files")
    public DeleteFilesResponse setFiles(List<? extends B2BaseFile> files) {
        files.forEach(fdat -> {
            fileAT.put(fdat.getFileId(), new FileResponse(fdat));
        });

        return this;
    }

    public Map<String, FileResponse> getFileAT() {
        return fileAT;
    }

    @JsonIgnore
    public FileResponse getFile(String fileId) {
        return fileAT.get(fileId);
    }

    public DeleteFilesResponse updateFile(FileResponse file) {

        fileAT.put(file.getFileId(), file);
        return this;
    }

//    public DeleteFilesResponse setFiles(Collection<FileResponse> files) {
//
//        files.forEach(deleteFile -> fileAT.put(deleteFile.getFileId(), deleteFile));
//        return this;
//    }
    // DeleteFileResponse

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
