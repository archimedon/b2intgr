package com.rdnsn.b2intgr.model;

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
//
	@JsonProperty
	String proxy;

    @JsonProperty
    String actual;

    @JsonProperty
    String sha1;

    @JsonProperty
	Long size;

	@JsonProperty
	boolean b2Complete = false;

    @JsonProperty
    String contentType;

    public ProxyUrl(String proxy, String sha1) {
        super();
        this.proxy = proxy;
        this.sha1 = sha1;
	}

    public String getSha1() {
        return sha1;
    }

    public ProxyUrl setSha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }

    public String getProxy() {
        return proxy;
    }

    public ProxyUrl setProxy(String proxy) {
        this.proxy = proxy;
        return this;
    }

    public String getActual() {
        return actual;
    }

    public ProxyUrl setActual(String actual) {
        this.actual = actual;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public ProxyUrl setSize(Long size) {
        this.size = size;
        return this;
    }

    public boolean isB2Complete() {
        return b2Complete;
    }

    public ProxyUrl setB2Complete(boolean b2Complete) {
        this.b2Complete = b2Complete;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public ProxyUrl setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

//    @Override
    public String toString() {
        return String.format("{ " +
            "proxy: '%s', " +
            "actual: '%s', " +
            "b2Complete: %b " +
            "}",
            proxy, actual, b2Complete
        );
    }

//    public String toString() {
//        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
//    }

}
