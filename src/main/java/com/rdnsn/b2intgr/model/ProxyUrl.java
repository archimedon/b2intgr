package com.rdnsn.b2intgr.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxyUrl {

    @JsonProperty
    private String actual;

    @JsonProperty
    private boolean b2Complete = false;

    @JsonProperty
    private String bucketId;

    @JsonProperty
    private String bucketType;

    @JsonProperty
    private String contentType;

    // Can be used as key
	@JsonProperty
    private String proxy;

    // Can be used as key
    @JsonProperty
    private String sha1;

    @JsonProperty
    private Long size;

    @JsonProperty
    private String fileId;

    // The ID from Neo but, not reliable longterm
    @JsonIgnore
    private transient Long transientId;


    public ProxyUrl() {
        super();
    }

    public ProxyUrl(String sha1) {
        this();
        this.sha1 = sha1;
	}

    public ProxyUrl(String proxy, String sha1) {
        this(sha1);
        this.proxy = proxy;
	}

    public String getActual() {
        return actual;
    }

    public ProxyUrl setActual(String actual) {
        this.actual = actual;
        return this;
    }

    public boolean isB2Complete() {
        return b2Complete;
    }

    public ProxyUrl setB2Complete(boolean b2Complete) {
        this.b2Complete = b2Complete;
        return this;
    }

    public String getBucketId() {
        return bucketId;
    }

    public ProxyUrl setBucketId(String bucketId) {
        this.bucketId = bucketId;
        return this;
    }

    public String getBucketType() {
        return bucketType;
    }

    public ProxyUrl setBucketType(String bucketType) {
        this.bucketType = bucketType;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public ProxyUrl setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public ProxyUrl setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getProxy() {
        return proxy;
    }

    public ProxyUrl setProxy(String proxy) {
        this.proxy = proxy;
        return this;
    }

    public String getSha1() {
        return sha1;
    }

    public ProxyUrl setSha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public ProxyUrl setSize(Long size) {
        this.size = size;
        return this;
    }

    public Long getTransientId() {
        return transientId;
    }

    public ProxyUrl setTransientId(Long transientId) {
        this.transientId = transientId;
        return this;
    }

    public String toString() {
        ReflectionToStringBuilder.setDefaultStyle(ToStringStyle.JSON_STYLE);
        return ReflectionToStringBuilder.toStringExclude(this, new String[]{"transientId"});
    }

    public String toCypherJson() {
        ReflectionToStringBuilder.setDefaultStyle(new CypherJsonToStringStyle());
        return ReflectionToStringBuilder.toStringExclude(this, new String[]{"transientId"});
    }
}

