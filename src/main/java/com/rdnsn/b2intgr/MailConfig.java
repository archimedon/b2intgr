package com.rdnsn.b2intgr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize()
public class MailConfig {

    @NotNull
    @JsonProperty
    private String password = "r3664370n";

    @NotNull
    @JsonProperty
    private String host = "smtp.gmail.com";

    @JsonProperty
    private int port = 465;

    @NotNull
    @JsonProperty
    private String username = "a2zlangmgr@gmail.com";

    @NotNull
    @JsonProperty
    private String recipients = "ronniedz1s@yahoo.com";


    public MailConfig() {
    }

    public int getPort() {
        return port;
    }

    public MailConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public String getRecipients() {
        return recipients;
    }

    public MailConfig setRecipients(String recipients) {
        this.recipients = recipients;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public MailConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public MailConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }
}
