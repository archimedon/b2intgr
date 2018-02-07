package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class B2ResponseBase {
	
	@JsonProperty
	protected String code;
	
	@JsonProperty
	protected Integer status;
	
	@JsonProperty
	protected String message;

    public String getCode() {
        return code;
    }

    public B2ResponseBase setCode(String code) {
        this.code = code;
        return this;
    }

    public Integer getStatus() {
        return status;
    }

    public B2ResponseBase setStatus(Integer status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public B2ResponseBase setMessage(String message) {
        this.message = message;
        return this;
    }

    public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

}
