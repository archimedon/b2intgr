package com.rdnisn.acrhq;

import java.io.File;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdnisn.acrhq.CloudFSProcessor.Verb;

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
public class Pinger extends CloudFSProcessor implements Processor {
	
    protected Logger log = LoggerFactory.getLogger(getClass());
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
		B2Response auth = (B2Response) authenticate();
		setReply(exchange, Verb.authorizeService, auth);
		setReply(exchange, Verb.authToken, auth.getAuthorizationToken());
//		log.debug("Pinger-AuthRep: " + authResponse);
//		Object obj = exchange.getIn().getBody();
//		if (obj != null) {
//			exchange.getOut().setBody(obj);
//			log.debug("Pinger-HASBODY: Ywah");
//		}
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
		log.info("Check Authentication status.??");

		if (force || lastmod <= 0 || noHaveToken() ) {
			log.info("Authentication required!");

			final ProducerTemplate b2producerTemplate = context.createProducerTemplate();
			
			final Message responseOut = b2producerTemplate.send(authenticationUrl, new Processor() {
				public void process(Exchange exchange) throws Exception {
					log.debug(String.format("Authentication request sentTo(authenticationUrl=%s)", authenticationUrl));
					exchange.getIn().removeHeaders("*");
					exchange.getIn().setHeader("Authorization", "Basic " + token);
				}
			}).getOut();

			int		responseCode = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
		
			log.debug("responseCode " + responseCode);

			try {
				authResponse = objectMapper.readValue(responseOut.getBody(String.class), B2Response.class);
				log.debug("Received auth token: " + authResponse.getAuthorizationToken());
//				lastmod = hasToken() ? new Date().getTime() : -1;
				lastmod = authResponse.getStatus() == null && authResponse.getAuthorizationToken() != null ? utcInSecs() : -1;
				log.debug("lastmod " + lastmod);
				b2producerTemplate.stop();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			log.info("Not renewing Authentication");
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
