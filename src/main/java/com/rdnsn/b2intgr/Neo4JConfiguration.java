package com.rdnsn.b2intgr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize()
public class Neo4JConfiguration {


    @NotNull
    @JsonProperty
    private String password = "reggae";

    @NotNull
    @JsonProperty
    private String urlString = "bolt://localhost:7687";

    @NotNull
    @JsonProperty
    private String username = "neo4j";

    public Neo4JConfiguration() {
    }

    public String getPassword() {
        return password;
    }

    public Neo4JConfiguration setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getUrlString() {
        return urlString;
    }

    public void setUrlString(String urlString) {
        this.urlString = urlString;
    }

    public String getUsername() {
        return username;
    }

    public Neo4JConfiguration setUsername(String username) {
        this.username = username;
        return this;
    }
}
