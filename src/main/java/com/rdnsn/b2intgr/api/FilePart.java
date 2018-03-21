package com.rdnsn.b2intgr.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static com.rdnsn.b2intgr.util.JsonHelper.sha1;

@SuppressWarnings("deprecation")
@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilePart  implements Comparable<FilePart> {

    @JsonIgnore
    private ByteBuffer data;

    @JsonProperty
    private long start;

//    private int bufSize;

    @JsonProperty
    private boolean unread = true;

    @JsonIgnore
    private FileChannel fileChannel;

    //X-Bz-Part-Number
    // A number from 1 to 10000.
    // The parts uploaded for one file must have contiguous numbers, starting with 1.
    @JsonProperty
    private int partNumber;

    // Content-Length
    // The number of bytes in the file being uploaded.
    // Note that this header is required; you cannot leave it out and just use chunked encoding.
    // The minimum size of every part but the last one is 5MB.
    // When sending the SHA1 checksum at the end, the Content-Length should be
    // set to the size of the file plus the 40 bytes of hex checksum.
    @JsonProperty
    private long contentLength;

    // The SHA1 checksum of the this part of the file. B2 will check this when the part is uploaded, to make sure that the data arrived correctly.
    // The same SHA1 checksum must be passed to b2_finish_large_file.
    // X-Bz-Content-Sha1
    @JsonProperty
    private String contentSha1;

    //  Authorization -   An upload authorization token, from b2_get_upload_part_url. The token must have the writeFiles capability.
    @JsonProperty
    private String authorizationToken;

    @JsonProperty
    private String fileId;

    @JsonProperty
    private String uploadUrl;

    public FilePart() {
        super();
    }

    public FilePart(final FileChannel fileChannel, long start, int bufSize, int partNo) {
        this();
        this.data = ByteBuffer.allocate(bufSize);
        this.setFileChannel(fileChannel);
        this.setStart(start);
        this.setPartNumber(partNo);

        // reset after readstream()
        this.setContentLength(bufSize);
    }

    public ByteBuffer getData() throws IOException {
        if (unread) {
            this.contentLength = fileChannel.read(data, start);
            unread = false;
        }
        data.rewind();
        return data;
    }

    private boolean isUnread() {
        return unread;
    }

    public FileChannel getFileChannel() {
        return fileChannel;
    }

    public FilePart setFileChannel(FileChannel fileChannel) {
        this.fileChannel = fileChannel;
        return this;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public FilePart setPartNumber(int partNumber) {
        this.partNumber = partNumber;
        return this;
    }

    public long getContentLength() {
        return contentLength;
    }

    private FilePart setContentLength(long contentLength) {
        this.contentLength = contentLength;
        return this;
    }

    public String getContentSha1() {
        if (StringUtils.isEmpty(contentSha1)) {
            try {
                contentSha1 = sha1(this.getData());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return contentSha1;
    }

    private FilePart setContentSha1(String contentSha1) {
        this.contentSha1 = contentSha1;
        return this;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    public FilePart setAuthorizationToken(String authorizationToken) {
        this.authorizationToken = authorizationToken;
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public FilePart setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public FilePart setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
        return this;
    }
    public FilePart done() {
        this.contentLength = 0;
        data.clear();
        return this;
    }

    public long getStart() {
        return start;
    }

    public FilePart setStart(long start) {
        this.start = start;
        return this;
    }
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }

    @Override
    public int compareTo(FilePart other) {
        return Integer.compare(this.partNumber, other.getPartNumber());
    }

//    public int getBufSize() {
//        return bufSize;
//    }
//
//    public FilePart setBufSize(int bufSize) {
//        this.bufSize = bufSize;
//        return this;
//    }
}
