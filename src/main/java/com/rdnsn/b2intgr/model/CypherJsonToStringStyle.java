package com.rdnsn.b2intgr.model;

import org.apache.commons.lang3.builder.ToStringStyle;

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
