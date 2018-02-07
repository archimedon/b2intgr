package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteFilesResponse extends DeleteFilesRequest {

    final private Map<String, DeleteFile> fileAT;

    public DeleteFilesResponse() {
        super();
        this.fileAT = new HashMap<String, DeleteFile>();
    }

    public DeleteFilesResponse(Collection<DeleteFile> files) {
        this();
        this.setFiles(files);
    }

    public Map<String, DeleteFile> getFileAT() {
        return fileAT;
    }

    public DeleteFile getFile(String fileId) {
        return fileAT.get(fileId);
    }

    public DeleteFilesResponse updateFile(DeleteFile file) {

        fileAT.put(file.getFileId(), file);
        return this;
    }

    public DeleteFilesResponse setFiles(Collection<DeleteFile> files) {
        files.forEach(deleteFile -> fileAT.put(deleteFile.getFileId(), deleteFile));
        return this;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
