package com.rdnsn.b2intgr.api;

public interface ReadsError {
    public String getMessage();
    public Integer getStatus();
    public String getCode();
    public ReadsError setMessage(String message);
    public ReadsError setStatus(Integer status);
    public ReadsError setCode(String code);
}
