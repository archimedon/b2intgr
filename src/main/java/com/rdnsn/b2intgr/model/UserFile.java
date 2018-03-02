package com.rdnsn.b2intgr.model;

import java.nio.file.Path;
import java.util.Map;

import com.rdnsn.b2intgr.processor.UploadProcessor;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.builder.ToStringStyle;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserFile implements Comparable<UserFile>, java.io.Serializable {
	
	private Path filepath;

	private Long transientId;

	private String url;
	
	private final Map<String, String> meta;
	
	private String contentType;

	private String sha1;



	public UserFile() {
		super();
        this.meta = (new ImmutableMap.Builder<String, String>()).build();
	}
	
	public UserFile(Path filepath) {
        super();
        this.setFilepath(filepath);
        this.meta = (new ImmutableMap.Builder<String, String>()).build();
	}

//
//	public UserFile(Path filepath, String fldname) {
//		this(filepath, fldname, null);
//	}

//	public UserFile(Path filepath, String fldname, Map<String, String> meta) {
//		super();
//		this.filepath = filepath;
//		this.url = fldname;
//		this.meta = meta;
//	}

    @JsonProperty
    public Long getTransientId() {
        return transientId;
    }

    public UserFile setTransientId(Long transientId) {
        this.transientId = transientId;
        return this;
    }

    @JsonProperty
	public String getSha1() { return this.sha1; }

	@JsonProperty
	public String getContentType() {
		return contentType;
	}

	public UserFile setContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	@JsonProperty
	public Path getFilepath() { return filepath; }

	public UserFile setFilepath(Path filepath) {
		this.filepath = filepath;
        this.sha1 = UploadProcessor.sha1(filepath.toFile());
        return this;
    }

	@JsonProperty
	public String getUrl() {
		return url;
	}

	public UserFile setUrl(String name) {
	    this.url = name;
        return this;
	}

	@JsonProperty
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
