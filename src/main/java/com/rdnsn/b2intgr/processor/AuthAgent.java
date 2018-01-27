package com.rdnsn.b2intgr.processor;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.rdnsn.b2intgr.Constants;
import com.rdnsn.b2intgr.api.AuthResponse;

public class AuthAgent implements AggregationStrategy {
	
	private static AuthResponse auth;
	private final MultipartAgent ma;
	private final ObjectMapper objectMapper;
	
	public AuthAgent(String remoteAuthenticationUrl, String basicAuthHeader, ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		ma = new MultipartAgent(
			remoteAuthenticationUrl,
			ImmutableMap.of(Constants.AUTHORIZATION, "Basic " + basicAuthHeader)
		);
		System.out.println("Retrieved Initial Token:\n" + getRemoteAuth());
	}
	
	synchronized public AuthResponse getRemoteAuth() {
		if (auth == null) {
			try {
				auth = objectMapper.readValue(ma.doGet(), AuthResponse.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return auth;
	}
	
	public Exchange aggregate(Exchange original, Exchange resource) {
		if (original == null) {
            // the first time we only have the new exchange
            return resource;
        }
		auth = resource.getIn().getBody(AuthResponse.class);
		original.getIn().setHeader(Constants.AUTH_RESPONSE, auth);
		original.getIn().setHeader(Constants.AUTHORIZATION, auth.getAuthorizationToken());
		
	    if (original.getPattern().isOutCapable()) {
	        original.getOut().setHeader(Constants.AUTH_RESPONSE, auth);
	    }
	    return original;
	}
}
