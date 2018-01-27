package com.rdnsn.b2intgr.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadData {

	@JsonProperty
	private List<UserFile> files;
	
	@JsonProperty
	private Map<String, String> form;
	
	public UploadData() {
		this(new ArrayList<UserFile>(), new HashMap<String, String>());
	}

	public UploadData(Map<String, String> form) {
		this(new ArrayList<UserFile>(), form);
	}
	
	public UploadData(List<UserFile> files, Map<String, String> form) {
		super();
		this.files = files;
		this.form = form;
	}
	
	public List<UserFile> getFiles() {
		return files;
	}

	public Map<String, String> getForm() {
		return form;
	}

	public void setForm(Map<String, String> form) {
		this.form = form;
	}

	public void setFiles(List<UserFile> files) {
		this.files = files;
	}
	
	public void addFile(UserFile file) {
		this.files.add(file);
	}
	
	public void putFormField(String nm, String val) {
		this.form.put(nm, val);
	}
	
	public UserFile getUserFile(Path nm) {
		return this.files.get(this.files.indexOf(nm));
	}
	
	public UserFile getUserFile(String nm) {
		return this.files.get(this.files.indexOf(Paths.get(nm)));
	}
	
}
