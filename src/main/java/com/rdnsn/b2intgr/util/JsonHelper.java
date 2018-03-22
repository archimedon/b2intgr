package com.rdnsn.b2intgr.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Message;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class JsonHelper {

    public static <T> T coerceClass(final ObjectMapper objectMapper, final Message rsrcIn, Class<T> type) {
        T obj = null;
        String string = rsrcIn.getBody(String.class);
        try {
            obj = objectMapper.readValue(string, type);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("err parsing: " + string);
            throw new RuntimeException(e.getCause());
        }
        return obj;
    }


    public static <T> T coerceClass(final ObjectMapper objectMapper, final String string, Class<T> type) {
        T obj = null;
        try {
            obj = objectMapper.readValue(string, type);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("err parsing: " + string);
            throw new RuntimeException(e.getCause());
        }
        return obj;
    }

    public static String objectToString(final ObjectMapper objectMapper, final Object t) {
        String string = null;
        try {
            string = objectMapper.writeValueAsString(t);
        } catch (JsonProcessingException e) {
            System.err.println("err parsing: " + string);
            throw new RuntimeException(e.getCause());
        }
        return string;
    }

    public static String sha1(ByteBuffer byteBuffer) {
        String ans = null;

        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            messageDigest.update(byteBuffer.array());
            try (Formatter formatter = new Formatter()) {
                for (final byte b : messageDigest.digest()) {
                    formatter.format("%02x", b);
                }
                ans = formatter.toString();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return ans;
    }


    public static String sha1(final File file) {
        String ans = null;
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");

            try(InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                final byte[] buffer = new byte[1024];
                for (int read = 0; (read = is.read(buffer)) != -1;) {
                    messageDigest.update(buffer, 0, read);
                }
            }
            // Convert the byte to hex format
            try (Formatter formatter = new Formatter()) {
                for (final byte b : messageDigest.digest()) {
                    formatter.format("%02x", b);
                }
                ans = formatter.toString();
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return ans;
    }

}
