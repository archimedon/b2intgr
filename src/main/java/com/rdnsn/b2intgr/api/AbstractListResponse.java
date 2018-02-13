package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractListResponse<E extends B2BaseFile> extends B2ReadsError implements ReadsError {

    protected Function<E, String> makeDownloadUrl = x -> x.getDownloadUrl();

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

    //JsonGetter(value = "files")
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

    //JsonSetter(value = "files")
    public <T extends AbstractListResponse<E>> T setFiles(List<E> files) {
        this.files = files;
        return (T) this;
    }

    private void applyUrlFormat() {
        if (files != null) {
            files.forEach(fileDat -> fileDat.setDownloadUrl(makeDownloadUrl.apply(fileDat)));
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
        if (files != null)
            applyUrlFormat();
        return (T) this;
    }

}

