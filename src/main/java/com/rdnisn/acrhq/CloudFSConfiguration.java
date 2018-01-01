package com.rdnisn.acrhq;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize()
public class CloudFSConfiguration {
	
	/**
	 * Configure the Remote server
	 */
	@NotNull
    @JsonProperty
	private RemoteStorageAPI remoteStorageConf;
    
	@NotNull
	@JsonProperty
	private String host = "localhost";

	@NotNull
	@JsonProperty
	private int port = 8080;
	
	@NotNull
	@JsonProperty
	private String contextUri = "cloudfs/api";
	
	@NotNull
	@JsonProperty
	private String docRoot = "/Users/ronalddennison/eclipse-workspace/acrhq";
	
	@NotNull
	@JsonProperty
	private String protocol = "http";
	
	
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getDocRoot() {
		return docRoot;
	}

	public void setDocRoot(String docRoot) {
		this.docRoot = docRoot;
	}

	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public String getContextUri() {
		return contextUri;
	}
	
	public void setContextUri(String contextUri) {
		this.contextUri = contextUri;
	}
	
	public String getRemoteAccountId() {
		return remoteStorageConf.getAccountId();
	}
	
	public String getRemoteApplicationKey() {
		return remoteStorageConf.getApplicationKey();
	}
	
	public String getRemoteAuthenticationUrl() {
		return remoteStorageConf.getAuthenticationUrl();
	}
	
	public RemoteStorageAPI getRemoteStorageConf() {
		return remoteStorageConf;
	}
	
	public void setRemoteStorageConf(RemoteStorageAPI remoteStorage) {
		this.remoteStorageConf = remoteStorage;
	}
}
