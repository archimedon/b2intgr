package com.rdnsn.b2intgr.api;

import java.util.Map;

public class StartLargeUploadResponse {
	private String accountId;
	private String bucketId;
	private String contentType;
	private String fileId;
	private Map<String, Object> fileInfo;
	private String fileName;
	private String uploadTimestamp;

    public StartLargeUploadResponse() {
    }

    public String getAccountId() {
        return accountId;
    }

    public StartLargeUploadResponse setAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public String getBucketId() {
        return bucketId;
    }

    public StartLargeUploadResponse setBucketId(String bucketId) {
        this.bucketId = bucketId;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public StartLargeUploadResponse setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public StartLargeUploadResponse setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public Map<String, Object> getFileInfo() {
        return fileInfo;
    }

    public StartLargeUploadResponse setFileInfo(Map<String, Object> fileInfo) {
        this.fileInfo = fileInfo;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public StartLargeUploadResponse setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getUploadTimestamp() {
        return uploadTimestamp;
    }

    public StartLargeUploadResponse setUploadTimestamp(String uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
        return this;
    }
}
