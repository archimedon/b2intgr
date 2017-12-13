package com.rdnisn.acrhq;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AuthNProcessor implements Processor {
    final Log log = LogFactory.getLog(AuthNProcessor.class);
	
	private B2Response authResponse = null;
	final private ObjectMapper objectMapper;
	
	public AuthNProcessor(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void process(Exchange exchange) {
		System.err.println("store ");
		final Message IN = exchange.getIn();
		final Message OUT = exchange.getOut();

		final String responseStr = exchange.getIn().getBody(String.class);
	    byte[] body = responseStr.getBytes();
	    
		System.err.println("responseStr " + responseStr);
		try {
			ObjectMapper oper = new ObjectMapper();
//			Map<String, String> map = objectMapper.readValue(responseStr, new TypeReference<Map<String,String>>(){});
			authResponse = objectMapper.readValue(body, B2Response.class);
//			IN.getHeaders().entrySet().forEach(entry -> map.put(entry.getKey(), entry.getValue() + ""));
			System.err.println("authResponse getMessage " + authResponse.getMessage());
			System.err.println("authResponse getStatus " + authResponse.getStatus());
			System.err.println("authResponse " + authResponse);
//			String json = objectMapper.writeValueAsString(authResponse);
//			System.err.println(json);
			OUT.setBody(responseStr);
			IN.setBody(responseStr);
			
			
			final Map<String, String> store = new HashMap<String, String>();
			
			IN.getHeaders().entrySet().forEach(entry ->
			{
				store.put(entry.getKey(), entry.getValue() + "");
			});
			
			System.err.println("store " + store);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println(e);
		}


		
		
	}

	public B2Response getAuthResponse() {
		return authResponse;
	}

	public void setAuthResponse(B2Response authResponse) {
		this.authResponse = authResponse;
	}

}
