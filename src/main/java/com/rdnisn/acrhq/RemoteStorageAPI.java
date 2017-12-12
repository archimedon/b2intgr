package com.rdnisn.acrhq;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize()
public class RemoteStorageAPI {

	@NotNull
	@JsonProperty
	private String accountId = "a374f8e3e263";

	@NotNull
	@JsonProperty
	private String authenticationUrl = "https://api.backblazeb2.com/b2api/v1/b2_authorize_account";

	@NotNull
	@JsonProperty
	private String applicationKey = "0012091458045a46b01b14df849c659aebb820a53c";


	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getAuthenticationUrl() {
		return authenticationUrl;
	}

	public void setAuthenticationUrl(String authenticationUrl) {
		this.authenticationUrl = authenticationUrl;
	}

	public String getApplicationKey() {
		return applicationKey;
	}

	public void setApplicationKey(String applicationKey) {
		this.applicationKey = applicationKey;
	}

}
