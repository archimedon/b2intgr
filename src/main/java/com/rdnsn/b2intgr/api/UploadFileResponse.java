package com.rdnsn.b2intgr.api;

import java.util.Map;

public class UploadFileResponse extends B2ResponseBase {
	
	private String accountId;
	private String action;
	private String bucketId;
	private String contentLength;
	private String contentSha1;
	private String contentType;
	private String fileId;
	private Map<String, Object> fileInfo;
	private String fileName;
	private Long uploadTimestamp;
	
	public String getAccountId() {
		return accountId;
	}
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getBucketId() {
		return bucketId;
	}
	public void setBucketId(String bucketId) {
		this.bucketId = bucketId;
	}
	public String getContentLength() {
		return contentLength;
	}
	public void setContentLength(String contentLength) {
		this.contentLength = contentLength;
	}
	public String getContentSha1() {
		return contentSha1;
	}
	public void setContentSha1(String contentSha1) {
		this.contentSha1 = contentSha1;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public String getFileId() {
		return fileId;
	}
	public void setFileId(String fileId) {
		this.fileId = fileId;
	}
	public Map<String, Object> getFileInfo() {
		return fileInfo;
	}
	public void setFileInfo(Map<String, Object> fileInfo) {
		this.fileInfo = fileInfo;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public Long getUploadTimestamp() {
		return uploadTimestamp;
	}
	public void setUploadTimestamp(Long uploadTimestamp) {
		this.uploadTimestamp = uploadTimestamp;
	}

	
}
