package com.rdnsn.b2intgr.model;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rdnsn.b2intgr.api.ErrorObject;
import com.rdnsn.b2intgr.processor.UploadProcessor;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.builder.ToStringStyle;

import static com.rdnsn.b2intgr.util.JsonHelper.sha1;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserFile implements Comparable<UserFile>, java.io.Serializable {

    @JsonIgnore
    private ErrorObject error;

    @JsonIgnore
    private String filepath;

    @JsonProperty
    String bucketId;

    @JsonProperty
    private Long transientId;

    @JsonProperty
    private String relativePath;

    @JsonProperty
    private String author;

    @JsonProperty
    private final Map<String, String> meta;

    @JsonProperty
    private String contentType;

    @JsonProperty
    private String sha1;

    @JsonProperty
    protected String downloadUrl;


    public UserFile() {
        super();
        this.meta = (new ImmutableMap.Builder<String, String>()).build();
    }

    public UserFile(File file) {
        this();
        this.setFilepath(file.getAbsolutePath());
        this.sha1 = sha1(file);
    }

    public UserFile(Path filePath) {
        this();
        this.setFilepath(filePath.toString());
        this.sha1 = sha1(filePath.toFile());
    }

    public UserFile(String filepath) {
        this();
        this.setFilepath(filepath);
        this.sha1 = sha1(new File(filepath));
    }

    public String getAuthor() {
        return author;
    }

    public String getBucketId() {
        return bucketId;
    }

    public UserFile setBucketId(String bucketId) {
        this.bucketId = bucketId;
        return this;
    }

    public UserFile setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public UserFile setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        return this;
    }

    public ErrorObject getError() {
        return error;
    }

    public UserFile setError(ErrorObject error) {
        this.error = error;
        return this;
    }

    public Long getTransientId() {
        return transientId;
    }

    public UserFile setTransientId(Long transientId) {
        this.transientId = transientId;
        return this;
    }

    public long getSize() {
        return Paths.get(filepath).toFile().length();
    }

    public String getSha1() {
        return this.sha1;
    }

    public UserFile setSha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public UserFile setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public UserFile setFilepath(String filepath) {
        this.filepath = filepath;
        return this;
    }

    public String getFilepath() {
        return this.filepath;
    }

    public String getRelativePath() {
        return this.relativePath;
    }

    public UserFile setRelativePath(String path) {
        this.relativePath = path;
        return this;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }

    @Override
    public int compareTo(UserFile other) {
        return this.filepath.compareTo(other.getFilepath());
    }
}
