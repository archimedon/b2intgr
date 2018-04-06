package com.rdnsn.b2intgr;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.net.URI;
import java.net.URISyntaxException;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class Neo4JConfiguration {


    @JsonProperty
    private String password;

    @JsonProperty
    private String urlString;

    @JsonProperty
    private String username;


    public Neo4JConfiguration() { }

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

    public void setUrlString(String urlString)
    {
        if (urlString.indexOf("@") > 1) {
            try {
                URI uri = new URI(urlString);
                String[] tmp = uri.getUserInfo().split(":");
                setUsername(tmp[0]);
                setPassword(tmp[1]);
                /*
                (String scheme,
               String userInfo, String host, int port,
               String path, String query, String fragment)
                 */
                this.urlString = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null).toString();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        else {
            this.urlString = urlString;
        }
    }

    public String getUsername() {
        return username;
    }

    public Neo4JConfiguration setUsername(String username) {
        this.username = username;
        return this;
    }
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }
}
