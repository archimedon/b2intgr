package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadFileResponse extends B2FileItem {

    @JsonProperty
	private String accountId;

    @JsonProperty
	private String bucketId;

    @JsonProperty
	private String contentSha1;

	@JsonProperty
	private Long transientId;


	public Long getTransientId() {
		return transientId;
	}

	public UploadFileResponse setTransientId(Long transientId) {
		this.transientId = transientId;
		return this;
	}

	public String getContentSha1() {
		return contentSha1;
	}

	public UploadFileResponse setContentSha1(String contentSha1) {
		this.contentSha1 = contentSha1;
		return this;
	}

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
