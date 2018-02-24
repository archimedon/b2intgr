package com.rdnsn.b2intgr;

import java.io.File;
import java.util.Base64;

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
    private RemoteStorageConfiguration remoteStorageConf;

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
    private String docRoot = "/tmp/appRoot";

    @NotNull
    @JsonProperty
    private String protocol = "http";

    @NotNull
    @JsonProperty
    private String customSeparator = "\\^";

    @NotNull
    @JsonProperty
    private String adminEmail = "ronniedz@gmail.com";

    @NotNull
    @JsonProperty
    private int maximumRedeliveries = 4;

    @NotNull
    @JsonProperty
    private int redeliveryDelay = 5000; // milliseconds

    @NotNull
    @JsonProperty
    private int poolSize = 5; // number of clients in pool

    @NotNull
    @JsonProperty
    private int maxPoolSize = 10; // max number of clients in pool


    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public int getMaximumRedeliveries() {
        return maximumRedeliveries;
    }

    public void setMaximumRedeliveries(int maximumRedeliveries) {
        this.maximumRedeliveries = maximumRedeliveries;
    }

    public int getRedeliveryDelay() {
        return redeliveryDelay;
    }

    public void setRedeliveryDelay(int redeliveryDelay) {
        this.redeliveryDelay = redeliveryDelay;
    }

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

    public String getCustomSeparator() {
        return customSeparator;
    }

    public void setCustomSeparator(String customSeparator) {
        this.customSeparator = customSeparator;
    }

    public String getContextUri() {
        return contextUri;
    }

    public void setContextUri(String contextUri) {
        this.contextUri = contextUri;
    }

    public String getRemoteBucketId() {
        return remoteStorageConf.getBucketId();
    }

    public String getRemoteBucketName() {
        return remoteStorageConf.getBucketName();
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

    public RemoteStorageConfiguration getRemoteStorageConf() {
        return remoteStorageConf;
    }

    public void setRemoteStorageConf(RemoteStorageConfiguration remoteStorage) {
        this.remoteStorageConf = remoteStorage;
    }

    public String getBasicAuthHeader() {
        return Base64.getEncoder()
                .encodeToString((getRemoteAccountId() + ":" + getRemoteApplicationKey()).getBytes());
    }
}
