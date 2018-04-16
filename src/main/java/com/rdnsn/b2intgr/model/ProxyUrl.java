package com.rdnsn.b2intgr.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import com.rdnsn.b2intgr.exception.DAOException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxyUrl {

    @JsonProperty
    private String actual;

    @JsonProperty
    private boolean b2Complete = false;

    @JsonProperty
    private String bucketId;

    @JsonProperty
    private String bucketType;

    @JsonProperty
    private String contentType;

    // Can be used as key
	@JsonProperty
    private String proxy;

    // Can be used as key
    @JsonProperty
    private String sha1;

    @JsonProperty
    private Long size;

    @JsonProperty
    private String fileId;

    // The ID from Neo but, not reliable longterm
    @JsonIgnore
    private transient Long transientId;

    class QueryBuilder {
        StringBuilder buf = new StringBuilder();

        boolean isEmpty() {
            return buf.length() == 0;
        }
        public StringBuilder append(String str) {
            if (! isEmpty()) buf.append(" AND ");
            buf.append(str);
            return buf;
        }
        public String toString() {
            return buf.toString();
        }
    }

// initial response:
// {sha1=5ae142f61dc516132c96defa015fde36209b5b24, actual=null, proxy=testing/68b-img/68b-img.gif,
// b2Complete=false, bucketType=null, size=68, bucketId=2ab327a44f788e635ef20613, contentType=image/gif,
// fileId=null}
    public String genIdCondition(String alias) throws DAOException{

        QueryBuilder q = new QueryBuilder();

        if (transientId != null) {
            q.append(String.format("ID(%s)=%d", alias, transientId));
        }
        else {
            if (fileId != null) {
                q.append(String.format("%s.fileId=\"%s\"", alias, fileId));
            } else if (StringUtils.isNotBlank(bucketId) && ( StringUtils.isNotBlank(sha1) ||  StringUtils.isNotBlank(proxy) )) {
                q.append(String.format("%s.bucketId=\"%s\"", alias, bucketId));
                if ( StringUtils.isNotBlank(sha1)) {
                    q.append(String.format("%s.sha1=\"%s\"", alias, sha1));
                }
                else if ( StringUtils.isNotBlank(proxy)) {
                    q.append(String.format("%s.proxy=\"%s\"", alias, proxy));
                }
            }
            else {
                throw new DAOException("Require an ID (`fileID`| `transientId`) or a unique composite that includes 'bucketId'");
            }
        }
        return q.toString();
    }

    public ProxyUrl() {
        super();
    }

    public ProxyUrl(String sha1) {
        this();
        this.sha1 = sha1;
	}

    public ProxyUrl(String proxy, String sha1) {
        this(sha1);
        this.proxy = proxy;
	}

    public String getActual() {
        return actual;
    }

    public ProxyUrl setActual(String actual) {
        this.actual = actual;
        return this;
    }

    public boolean isB2Complete() {
        return b2Complete;
    }

    public ProxyUrl setB2Complete(boolean b2Complete) {
        this.b2Complete = b2Complete;
        return this;
    }

    public String getBucketId() {
        return bucketId;
    }

    public ProxyUrl setBucketId(String bucketId) {
        this.bucketId = bucketId;
        return this;
    }

    public String getBucketType() {
        return bucketType;
    }

    public ProxyUrl setBucketType(String bucketType) {
        this.bucketType = bucketType;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public ProxyUrl setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public ProxyUrl setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getProxy() {
        return proxy;
    }

    public ProxyUrl setProxy(String proxy) {
        this.proxy = proxy;
        return this;
    }

    public String getSha1() {
        return sha1;
    }

    public ProxyUrl setSha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public ProxyUrl setSize(Long size) {
        this.size = size;
        return this;
    }

    public Long getTransientId() {
        return transientId;
    }

    public ProxyUrl setTransientId(Long transientId) {
        this.transientId = transientId;
        return this;
    }

    public String toString() {
        ReflectionToStringBuilder.setDefaultStyle(ToStringStyle.JSON_STYLE);
        return ReflectionToStringBuilder.toStringExclude(this, new String[]{"transientId"});
    }

    public String toCypherJson() {
        ReflectionToStringBuilder.setDefaultStyle(new CypherJsonToStringStyle());
        return ReflectionToStringBuilder.toStringExclude(this, new String[]{"transientId"});
    }
}

