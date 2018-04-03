package com.rdnsn.b2intgr;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A bare bones HTTP client using low-level java URLConnection API.
 *
 * Only 'POST' files implemented
 *
 */
public class BasicHttpClient {

    private static final String charset = "UTF-8";
    private static final String CR = "\r\n";
    private static final String prefix =  "--";
    private static final String boundary = UUID.randomUUID().toString().replaceAll("\\W", "").substring(0, 24);
    private static final String boundaryStr = prefix + boundary;

    // Header template for each Payload
    private final static String contentDispositionTmplt = "content-disposition: %s; name=\"%s\"; filename=\"%s\"";

    private List<Pair<File, String>> files = new LinkedList<Pair<File, String>>();

    private HttpURLConnection http;


    public BasicHttpClient(String uploadUrl) throws MalformedURLException {
        super();
        try {
            this.http = (HttpURLConnection) new URL(uploadUrl).openConnection();

            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setDoInput(true);
            http.setRequestProperty("Accept", "*/*");
            http.setRequestProperty("Cache-control", "no-cache");
            http.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary + "");
            // Enable streaming mode with default settings
            // http.setFixedLengthStreamingMode(len);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BasicHttpClient addInput(File file, String fieldName) {
        this.files.add(ImmutablePair.of(file, fieldName));
        return this;
    }

    private void endPayload(PrintWriter writer) {
        // Finish
        writer.append(boundaryStr).append("--").append(CR).flush();
        writer.close();
    }

    /**
     * Sets buf size to zero which implies Streaming
     *
     * @return self
     */
    public BasicHttpClient streamData() {
        http.setChunkedStreamingMode(0);
        return this;
    }

    public String post() throws IOException {

        try( OutputStream out = http.getOutputStream() ) {

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);

            files.forEach(field -> {

                final File file = field.getLeft();
                final String fieldName = field.getRight();

//                try (FileInputStream fileStream = new FileInputStream(file)) {
                try  {

                    // Write Payload
                    writer.append(boundaryStr);
                    writer.append(CR);
                    writer.append(makePayloadHeader(fieldName, URLEncoder.encode(file.getName(), charset)));
                    writer.append(CR);
                    writer.append(CR);
                    writer.flush();

                    // Write file
                    Files.copy(file.toPath(), out); out.flush();

                    /* Alternative ways to write: */

                    // putBuffer(out, fileStream);
                    // putByteArray(out, fileStream).flush();
                    // writeBytes(out, fileStream).flush();
                    // fileStream.close();

                    writer.append(CR).flush();
                } catch (IOException fne) {

                }
            });

            endPayload(writer);
        };

        StringBuilder strbuf = new StringBuilder();

        // checks server's status code first
        int status = http.getResponseCode();

        if (status == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                strbuf.append(line);
            }
            reader.close();
            http.disconnect();
        } else {
            throw new IOException("Server returned failure status: " + status + "\n" + strbuf.toString());
        }
        return strbuf.toString();
    }

    private String makePayloadHeader(String fieldName, String filename) {
        StringBuilder buf = new StringBuilder(String.format(contentDispositionTmplt, "form-data", fieldName, filename));
        String contentype = URLConnection.guessContentTypeFromName(filename);
        if (contentype != null) {
            buf.append(CR).append("Content-Type: " + contentype);
        }
        return buf.toString();
    }

    /**
     * 1.
     * A Mode of writing data to connection
     *
     */
    private OutputStream putBuffer(OutputStream out, InputStream fins) {
        BufferedOutputStream baout = new BufferedOutputStream(out);
        try {
            baout.write(org.apache.commons.io.IOUtils.toByteArray(fins));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
     * 2.
     * A Mode of writing data to connection
     *
     */
    private OutputStream putByteArray(OutputStream out, InputStream fins) throws IOException {
        out.write(org.apache.commons.io.IOUtils.toByteArray(fins));
        return out;
    }

    /**
     * 3.
     * A Mode of writing data to connection
     *
     */
    private OutputStream writeBytes(OutputStream out,InputStream in) throws IOException {

        byte[] buffer = new byte[4096];
        for (int n = 0; n >= 0; n = in.read(buffer)) {
            out.write(buffer, 0, n);
        }
        return out;
    }
}

/**
 Produces:

 -------------------------------
 Headers:
 -------------------------------
 Accept	                 [asterisk/asterisk]
 author					 testUser
 breadcrumbId			 ID-Ronalds-MacBook-Pro-local-1522704418614-0-109
 bucketId				 2ab327a44f788e635ef20613
 Cache-control			 no-cache
 CamelHttpMethod			 POST
 CamelHttpUri			 http://localhost:8080/cloudfs/api/v1/uptest/2ab327a44f788e635ef20613/testUser/testing
 CamelRestletRequest		 POST http://localhost:8080/cloudfs/api/v1/uptest/2ab327a44f788e635ef20613/testUser/testing HTTP/1.1
 CamelRestletResponse	 HTTP/1.1 - OK (200) - The request has succeeded
 Connection				 keep-alive
 Content-type			 multipart/form-data; boundary=7c9b17fc30594572b98b23e3
 destDir					 testing
 Host					 localhost:8080
 org.restlet.startTime	 1522712109122
 Transfer-encoding		 chunked
 User-agent				 Java/1.8.0_112
 */

/**
 -------------------------------
 Body
 -------------------------------
 --7c9b17fc30594572b98b23e3
 content-disposition: form-data; name="test/text/2"; filename="1024b-file.txt"
 Content-Type: text/plain

 0000000000000000000000000000000
 1111111111111111111111111111111
 2222222222222222222222222222222
 3333333333333333333333333333333
 4444444444444444444444444444444
 5555555555555555555555555555555
 6666666666666666666666666666666
 7777777777777777777777777777777
 8888888888888888888888888888888
 9999999999999999999999999999999
 aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
 bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
 ccccccccccccccccccccccccccccccc
 ddddddddddddddddddddddddddddddd
 eeeeeeeeeeeeeeeeeeeeeeeeeeeeeee
 fffffffffffffffffffffffffffffff
 0000000000000000000000000000000
 1111111111111111111111111111111
 2222222222222222222222222222222
 3333333333333333333333333333333
 4444444444444444444444444444444
 5555555555555555555555555555555
 6666666666666666666666666666666
 7777777777777777777777777777777
 8888888888888888888888888888888
 9999999999999999999999999999999
 aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
 bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
 ccccccccccccccccccccccccccccccc
 ddddddddddddddddddddddddddddddd
 eeeeeeeeeeeeeeeeeeeeeeeeeeeeeee
 fffffffffffffffffffffffffffffff

 --7c9b17fc30594572b98b23e3
 content-disposition: form-data; name="test/image/3"; filename="52b-img.gif"
 Content-Type: image/gif

 GIF89a.... (binary stuff)
 
 --7c9b17fc30594572b98b23e3--


 -------------------------------

 **/
