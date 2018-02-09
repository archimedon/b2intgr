package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class B2ReadsError implements ReadsError {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    protected ErrorObject error = new ErrorObject();

    public B2ReadsError() {
        super();
    }

    public B2ReadsError setError(ErrorObject error) {
        this.error = error;
        return this;
    }

    @JsonSetter(value = "code")
    public B2ReadsError setCode(String code) {
        mandatoryErrorObject().setCode(code);
        return this;
    }

    @JsonSetter(value = "status")
    public B2ReadsError setStatus(Integer status) {
        mandatoryErrorObject().setStatus(status);
        return this;
    }

    @JsonSetter(value = "message")
    public B2ReadsError setMessage(String message) {
        mandatoryErrorObject().setMessage(message);
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getCode() {
        return (String) (error == null ? null : error.getCode());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Integer getStatus() {
        return (Integer) (error == null ? null : error.getStatus());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getMessage() {
        return (String) (error == null ? null : error.getMessage());
    }

    @JsonIgnore
    public ReadsError mandatoryErrorObject(){
        if (error == null) {
            error = new ErrorObject();
        }
        return error;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
