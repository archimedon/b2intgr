package com.rdnsn.b2intgr.model;

import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserFile implements Comparable<UserFile>, java.io.Serializable {
	
	@JsonProperty
	private Path filepath;
	
	@JsonProperty
	private String name;
	
	@JsonProperty
	private final Map<String, String> meta;
	
	@JsonProperty
	private String contentType;
	
	public UserFile() {
		super();
		meta = (new ImmutableMap.Builder<String, String>()).build();
	}
	
	public UserFile(Path filepath) {
		this(filepath, "", (new ImmutableMap.Builder<String, String>()).build());
	}
	
	public UserFile(Path filepath, String fldname) {
		this(filepath, fldname, null);
	}

	public UserFile(Path filepath, String fldname, Map<String, String> meta) {
		super();
		this.filepath = filepath;
		this.name = fldname;
		this.meta = meta;
	}
	
	public void setFilepath(Path filepath) {
		this.filepath = filepath;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Path getFilepath() {
		return filepath;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getMeta() {
		return meta;
	}

	@Override
	public int compareTo(UserFile other) {
		return this.filepath.compareTo(other.getFilepath());
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
