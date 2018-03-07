package com.rdnsn.b2intgr.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
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
	String proxy;

    @JsonProperty
    String actual;

    @JsonProperty
    String sha1;

    @JsonProperty
	Long size;

    @JsonIgnore
	transient Long transientId;

	@JsonProperty
	boolean b2Complete = false;

    @JsonProperty
    String contentType;


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

    public Long getTransientId() {
        return transientId;
    }

    public ProxyUrl setTransientId(Long transientId) {
        this.transientId = transientId;
        return this;
    }

    public String getSha1() {
        return sha1;
    }

    public ProxyUrl setSha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }

    public String getProxy() {
        return proxy;
    }

    public ProxyUrl setProxy(String proxy) {
        this.proxy = proxy;
        return this;
    }

    public String getActual() {
        return actual;
    }

    public ProxyUrl setActual(String actual) {
        this.actual = actual;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public ProxyUrl setSize(Long size) {
        this.size = size;
        return this;
    }

    public boolean isB2Complete() {
        return b2Complete;
    }

    public ProxyUrl setB2Complete(boolean b2Complete) {
        this.b2Complete = b2Complete;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public ProxyUrl setContentType(String contentType) {
        this.contentType = contentType;
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
class CypherJsonToStringStyle extends ToStringStyle {

    private static final long serialVersionUID = 1L;

    private static final String FIELD_NAME_QUOTE = "\"";

    /**
     * <p>
     * Constructor.
     * </p>
     *
     * <p>
     * Use the static constant rather than instantiating.
     * </p>
     */
    CypherJsonToStringStyle() {
        super();

        this.setUseClassName(false);
        this.setUseIdentityHashCode(false);

        this.setContentStart("{");
        this.setContentEnd("}");

        this.setArrayStart("[");
        this.setArrayEnd("]");

        this.setFieldSeparator(",");
        this.setFieldNameValueSeparator(":");

        this.setNullText("null");

        this.setSummaryObjectStartText("\"<");
        this.setSummaryObjectEndText(">\"");

        this.setSizeStartText("\"<size=");
        this.setSizeEndText(">\"");
    }

    @Override
    public void append(final StringBuffer buffer, final String fieldName,
                       final Object[] array, final Boolean fullDetail) {

        if (fieldName == null) {
            throw new UnsupportedOperationException(
                    "Field names are mandatory when using CypherJsonToStringStyle");
        }
        if (!isFullDetail(fullDetail)){
            throw new UnsupportedOperationException(
                    "FullDetail must be true when using CypherJsonToStringStyle");
        }

        super.append(buffer, fieldName, array, fullDetail);
    }

    @Override
    public void append(final StringBuffer buffer, final String fieldName, final long[] array,
                       final Boolean fullDetail) {

        if (fieldName == null) {
            throw new UnsupportedOperationException(
                    "Field names are mandatory when using CypherJsonToStringStyle");
        }
        if (!isFullDetail(fullDetail)){
            throw new UnsupportedOperationException(
                    "FullDetail must be true when using CypherJsonToStringStyle");
        }

        super.append(buffer, fieldName, array, fullDetail);
    }

    @Override
    public void append(final StringBuffer buffer, final String fieldName, final int[] array,
                       final Boolean fullDetail) {

        if (fieldName == null) {
            throw new UnsupportedOperationException(
                    "Field names are mandatory when using CypherJsonToStringStyle");
        }
        if (!isFullDetail(fullDetail)){
            throw new UnsupportedOperationException(
                    "FullDetail must be true when using CypherJsonToStringStyle");
        }

        super.append(buffer, fieldName, array, fullDetail);
    }

    @Override
    public void append(final StringBuffer buffer, final String fieldName,
                       final short[] array, final Boolean fullDetail) {

        if (fieldName == null) {
            throw new UnsupportedOperationException(
                    "Field names are mandatory when using CypherJsonToStringStyle");
        }
        if (!isFullDetail(fullDetail)){
            throw new UnsupportedOperationException(
                    "FullDetail must be true when using CypherJsonToStringStyle");
        }

        super.append(buffer, fieldName, array, fullDetail);
    }

    @Override
    public void append(final StringBuffer buffer, final String fieldName, final byte[] array,
                       final Boolean fullDetail) {

        if (fieldName == null) {
            throw new UnsupportedOperationException(
                    "Field names are mandatory when using CypherJsonToStringStyle");
        }
        if (!isFullDetail(fullDetail)){
            throw new UnsupportedOperationException(
                    "FullDetail must be true when using CypherJsonToStringStyle");
        }

        super.append(buffer, fieldName, array, fullDetail);
    }

    @Override
    public void append(final StringBuffer buffer, final String fieldName, final char[] array,
                       final Boolean fullDetail) {

        if (fieldName == null) {
            throw new UnsupportedOperationException(
                    "Field names are mandatory when using CypherJsonToStringStyle");
        }
        if (!isFullDetail(fullDetail)){
            throw new UnsupportedOperationException(
                    "FullDetail must be true when using CypherJsonToStringStyle");
        }

        super.append(buffer, fieldName, array, fullDetail);
    }

    @Override
    public void append(final StringBuffer buffer, final String fieldName,
                       final double[] array, final Boolean fullDetail) {

        if (fieldName == null) {
            throw new UnsupportedOperationException(
                    "Field names are mandatory when using CypherJsonToStringStyle");
        }
        if (!isFullDetail(fullDetail)){
            throw new UnsupportedOperationException(
                    "FullDetail must be true when using CypherJsonToStringStyle");
        }

        super.append(buffer, fieldName, array, fullDetail);
    }

    @Override
    public void append(final StringBuffer buffer, final String fieldName,
                       final float[] array, final Boolean fullDetail) {

        if (fieldName == null) {
            throw new UnsupportedOperationException(
                    "Field names are mandatory when using CypherJsonToStringStyle");
        }
        if (!isFullDetail(fullDetail)){
            throw new UnsupportedOperationException(
                    "FullDetail must be true when using CypherJsonToStringStyle");
        }

        super.append(buffer, fieldName, array, fullDetail);
    }

    @Override
    public void append(final StringBuffer buffer, final String fieldName,
                       final boolean[] array, final Boolean fullDetail) {

        if (fieldName == null) {
            throw new UnsupportedOperationException(
                    "Field names are mandatory when using CypherJsonToStringStyle");
        }
        if (!isFullDetail(fullDetail)){
            throw new UnsupportedOperationException(
                    "FullDetail must be true when using CypherJsonToStringStyle");
        }

        super.append(buffer, fieldName, array, fullDetail);
    }

    @Override
    public void append(final StringBuffer buffer, final String fieldName, final Object value,
                       final Boolean fullDetail) {

        if (fieldName == null) {
            throw new UnsupportedOperationException(
                    "Field names are mandatory when using CypherJsonToStringStyle");
        }
        if (!isFullDetail(fullDetail)){
            throw new UnsupportedOperationException(
                    "FullDetail must be true when using CypherJsonToStringStyle");
        }

        super.append(buffer, fieldName, value, fullDetail);
    }

    @Override
    protected void appendDetail(final StringBuffer buffer, final String fieldName, final char value) {
        appendValueAsString(buffer, String.valueOf(value));
    }

    @Override
    protected void appendDetail(final StringBuffer buffer, final String fieldName, final Object value) {

        if (value == null) {
            appendNullText(buffer, fieldName);
            return;
        }

        if (value instanceof String || value instanceof Character) {
            appendValueAsString(buffer, value.toString());
            return;
        }

        if (value instanceof Number || value instanceof Boolean) {
            buffer.append(value);
            return;
        }

        final String valueAsString = value.toString();
        if (isJsonObject(valueAsString) || isJsonArray(valueAsString)) {
            buffer.append(value);
            return;
        }

        appendDetail(buffer, fieldName, valueAsString);
    }

    private boolean isJsonArray(final String valueAsString) {
        return valueAsString.startsWith(getArrayStart())
                && valueAsString.startsWith(getArrayEnd());
    }

    private boolean isJsonObject(final String valueAsString) {
        return valueAsString.startsWith(getContentStart())
                && valueAsString.endsWith(getContentEnd());
    }

    /**
     * Appends the given String in parenthesis to the given StringBuffer.
     *
     * @param buffer the StringBuffer to append the value to.
     * @param value the value to append.
     */
    private void appendValueAsString(final StringBuffer buffer, final String value) {
        buffer.append('"').append(value).append('"');
    }

    @Override
    protected void appendFieldStart(final StringBuffer buffer, final String fieldName) {

        if (fieldName == null) {
            throw new UnsupportedOperationException(
                    "Field names are mandatory when using CypherJsonToStringStyle");
        }

        super.appendFieldStart(buffer, fieldName);
    }

    /**
     * <p>
     * Ensure <code>Singleton</code> after serialization.
     * </p>
     *
     * @return the singleton
     */
    private Object readResolve() {
        return ToStringStyle.JSON_STYLE;
    }

}

