package com.rdnsn.b2intgr.api;



public class PartFile {

    //X-Bz-Part-Number
    // A number from 1 to 10000.
    // The parts uploaded for one file must have contiguous numbers, starting with 1.
    private int partNumber;

    // Content-Length
    // The number of bytes in the file being uploaded.
    // Note that this header is required; you cannot leave it out and just use chunked encoding.
    // The minimum size of every part but the last one is 5MB.
    // When sending the SHA1 checksum at the end, the Content-Length should be
    // set to the size of the file plus the 40 bytes of hex checksum.
    private long contentLength;

    // The SHA1 checksum of the this part of the file. B2 will check this when the part is uploaded, to make sure that the data arrived correctly.
    // The same SHA1 checksum must be passed to b2_finish_large_file.
    // X-Bz-Content-Sha1
    private String contentSha1;

    //  Authorization -   An upload authorization token, from b2_get_upload_part_url. The token must have the writeFiles capability.
	private String authorizationToken;
	private String fileId;
	private String uploadUrl;

    public PartFile() {
        super();
    }

    public int getPartNumber() {
        return partNumber;
    }

    public PartFile setPartNumber(int partNumber) {
        this.partNumber = partNumber;
        return this;
    }

    public long getContentLength() {
        return contentLength;
    }

    public PartFile setContentLength(long contentLength) {
        this.contentLength = contentLength;
        return this;
    }

    public String getContentSha1() {
        return contentSha1;
    }

    public PartFile setContentSha1(String contentSha1) {
        this.contentSha1 = contentSha1;
        return this;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    public PartFile setAuthorizationToken(String authorizationToken) {
        this.authorizationToken = authorizationToken;
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public PartFile setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public PartFile setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
        return this;
    }
}
