package com.rdnsn.b2intgr.api;

import java.util.Map;

public class UploadFileResponse extends B2FileItem {
	
	private String accountId;
	private String bucketId;

	public String getAccountId() {
		return accountId;
	}

	public UploadFileResponse setAccountId(String accountId) {
		this.accountId = accountId;
		return this;
	}

	public String getBucketId() {
		return bucketId;
	}

	public UploadFileResponse setBucketId(String bucketId) {
		this.bucketId = bucketId;
		return this;
	}
}
