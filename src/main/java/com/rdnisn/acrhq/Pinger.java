package com.rdnisn.acrhq;

import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/*
ACCOUNT_ID=... # Comes from your account page on the Backblaze web site
		ACCOUNT_AUTHORIZATION_TOKEN=... # Comes from the b2_authorize_account call
		curl \
		    -H "Authorization: ${ACCOUNT_AUTHORIZATION_TOKEN}" \
		    -d "{\"accountId\": \"$ACCOUNT_ID\", \"bucketTypes\": [\"allPrivate\",\"allPublic\"]}" \
		    "${API_URL}/b2api/v1/b2_list_buckets"    

		{
		  "buckets": [
		    {
		      "accountId": "a374f8e3e263",
		      "bucketId": "2ab327a44f788e635ef20613",
		      "bucketInfo": {},
		      "bucketName": "b2public",
		      "bucketType": "allPublic",
		      "corsRules": [],
		      "lifecycleRules": [],
		      "revision": 2
		    },

*/
public class Pinger implements Processor {
	
    final private static Log log = LogFactory.getLog(Pinger.class);
	private static final long TTL = 10;
//	private static final long TTL = 12 * 60 * 58;
	final private CamelContext context;
	final private String authenticationUrl;
	final private String token;
	final private ObjectMapper objectMapper;
	
	private B2Response authResponse;
	private static long lastmod = 0;
	
	public Pinger(CamelContext ctx, ObjectMapper objectMapper, String aUrl, String token) {
		this.context = ctx;
		this.authenticationUrl = aUrl;
		this.objectMapper = objectMapper;
		this.token = token;
		log.info("authenticationUrl: " + authenticationUrl);
	}
	
	@Override
	public void process(Exchange exchange) {
		exchange.getOut().copyFrom(exchange.getIn());
		authResponse = authenticate();
		exchange.getOut().setBody(authResponse);
		exchange.getOut().setHeader("Authorization", authResponse.getAuthorizationToken());
//		exchange.getOut().setHeader("apiUrl", authResponse.getApiUrl());
//		exchange.getOut().setHeader("downloadUrl", authResponse.getDownloadUrl());
//		exchange.getOut().setHeader("authorizationToken", authResponse.getAuthorizationToken());
//		exchange.getOut().setHeader("accountId", authResponse.getAccountId());
	}

	public void setAuthResponse(B2Response authResponse) {
		this.authResponse = authResponse;
	}

	private boolean hasToken() {
		return ! noHaveToken();
	}
	
	private boolean noHaveToken() {
		return	
			authResponse == null	
			|| authResponse.getAuthorizationToken() == null
			|| ( utcInSecs() - lastmod) >= TTL;
	}
	
	public B2Response authenticate() {
		return authenticate(false);
	}
	
	public B2Response authenticate(boolean force) {
		log.info("authenticating...");
		System.err.println("Check auth stat");

		if (force || lastmod <= 0 || noHaveToken() ) {
			System.err.println("authenticating...\n@: " + authenticationUrl);

			final Message responseOut = context.createProducerTemplate().send(authenticationUrl, new Processor() {
				public void process(Exchange exchange) throws Exception {
					System.err.println("process...");
					exchange.getIn().removeHeaders("*");
					exchange.getIn().setHeader("Authorization", "Basic " + token);
				}
			}).getOut();
			
			String	responseBody = responseOut.getBody(String.class);
			int		responseCode = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
		
			System.err.println("responseCode " + responseCode);

			try {
				authResponse = objectMapper.readValue(responseBody, B2Response.class);
//				lastmod = hasToken() ? new Date().getTime() : -1;
				lastmod = authResponse.getStatus() == null && authResponse.getAuthorizationToken() != null ? utcInSecs() : -1;
				System.err.println("lastmod " + lastmod);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			System.err.println("authResponse getStatus " + authResponse.getStatus());
			System.err.println("authResponse token " + authResponse.getAuthorizationToken());
		}
		return authResponse;
	}


	private long utcInSecs() {
		return new Date().getTime() / 1000;
	}

	public B2Response getAuthResponse() {
		return authResponse;
	}
}
