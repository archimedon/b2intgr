package com.rdnsn.b2intgr.api;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

public class B2ResponseBase {
	
	@JsonProperty
	private String code;
	
	@JsonProperty
	private Integer status;
	
	@JsonProperty
	private String message;
	
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

}
