package com.rdnsn.b2intgr.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Message;

import java.io.IOException;

public class JsonHelper {

    public static <T> T coerceClass(final ObjectMapper objectMapper, final Message rsrcIn, Class<T> type) {
        T obj = null;
        try {
            String string = rsrcIn.getBody(String.class);
            obj = objectMapper.readValue(string, type);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getCause());
        }
        return obj;
    }

    public static String objectToString(final ObjectMapper objectMapper, final Object t) {
        String out = null;
        try {
            out = objectMapper.writeValueAsString(t);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getCause());
        }
        return out;
    }

}
