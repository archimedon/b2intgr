package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractListResponse<E extends B2BaseFile> extends B2ReadsError implements ReadsError {

    @JsonIgnore
    protected Function<E, String> makeDownloadUrl = null; //x -> x.getDownloadUrl() == null ? "/" + x.getFileName() : x.getDownloadUrl();

    @JsonIgnore
    protected boolean beenRun = false;

    @JsonProperty
    protected List<E> files;


    public AbstractListResponse() {
        super();
        files = new ArrayList<E>();
    }

    public AbstractListResponse(List<E> files) {
        this();
        this.files = files;
    }

    /**
     * Get the file list
     *
     * @return List of files of the specified type, or <tt>null</tt> if list is empty.
     */
    public List<E> getFiles() {
        if (!beenRun )
            applyUrlFormat();
        return files;
    }

    public <T extends AbstractListResponse<E>> T setFiles(List<E> files) {
        this.files = files;
        return (T) this;
    }

    private void applyUrlFormat() {
        if (makeDownloadUrl!= null && files != null) {
            files.forEach(fileDat -> {
                fileDat.setDownloadUrl( makeDownloadUrl.apply(fileDat));
            });
            beenRun = true;
        }
    }

    /**
     * Updates the downloadUrl of the list's fileObject.
     *
     * @param makeDownloadUrl a function to build the url
     * @param <T> this object
     * @return a string representing the URL
     */
    public <T extends AbstractListResponse<E>> T setMakeDownloadUrl(Function<E, String> makeDownloadUrl) {
        this.makeDownloadUrl = makeDownloadUrl;
        return (T) this;
    }

}

