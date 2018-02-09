package com.rdnsn.b2intgr.processor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdnsn.b2intgr.Constants;
import com.rdnsn.b2intgr.api.AuthResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthAgent implements AggregationStrategy {

    private static Logger log = LoggerFactory.getLogger(AuthAgent.class);

    private static AuthResponse authResponse = null;

	private final ObjectMapper objectMapper;
	private final HttpGet request;

	public AuthAgent(String remoteAuthenticationUrl, String basicAuthHeader, ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.request = this.init(remoteAuthenticationUrl, basicAuthHeader);
	}

	private HttpGet init(String authUrl, String basicAuthHeader) {
		HttpGet aReq = new HttpGet(authUrl);
		aReq.setHeader(Constants.AUTHORIZATION, "Basic " + basicAuthHeader);
		return aReq;
	}
    boolean open = true;

    synchronized public AuthResponse getAuthResponse() {

		if (isExpired()) {
            getAuthorization();
            open = false;
            log.info("Auth Token Updated: {}", authResponse);
        }
		return authResponse;
	}

    synchronized private AuthResponse getAuthorization() {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();

            HttpResponse response = httpclient.execute(request);
            response.getEntity().writeTo(buf);
//            String json = buf.toString(Constants.UTF_8);
            authResponse = objectMapper.readValue(buf.toString(Constants.UTF_8), AuthResponse.class);
//            log.info("Auth String:\n{}", json);
            log.info("AuthObject:\n{}", authResponse);

        } catch (IOException e) {
            e.printStackTrace();
        }

		return this.authResponse;
	}

//    private void setAuthResponse(AuthResponse auth){
//        this.authResponse = auth;
//    }

	@Override
	public Exchange aggregate(Exchange original, Exchange resource) {
		if (original == null) {
		    log.debug("IS NULL orig");
            // the first time we only have the new exchange
            return resource;
        }

        final AuthResponse auth = resource.getIn().getBody(AuthResponse.class);
		// Set on In() and Out() becu

        original.getIn().setHeader(Constants.AUTH_RESPONSE, auth);
        original.getIn().setHeader(Constants.AUTHORIZATION, auth.getAuthorizationToken());

        if (original.getPattern().isOutCapable()) {
            original.getOut().setBody( original.getIn().getBody());
            original.getOut().setHeader(Constants.AUTH_RESPONSE, auth);
            original.getOut().setHeader(Constants.AUTHORIZATION, auth.getAuthorizationToken());
	    }
	    return original;
	}

    public boolean isExpired() {
        return authResponse == null || (utcInSecs() - authResponse.getLastmod()) >= Constants.B2_TOKEN_TTL;
    }

    private long utcInSecs() {
        return new Date().getTime() / 1000;
    }

    public String getApiUrl() {
        return getAuthResponse().getApiUrl();
    }
}

