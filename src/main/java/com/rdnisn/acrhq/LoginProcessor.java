package com.rdnisn.acrhq;

import static com.rdnisn.acrhq.RemoteStorageAPI.getHttp4Proto;
import static com.rdnisn.acrhq.RemoteStorageAPI.http4Suffix;

import java.io.ByteArrayOutputStream;
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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdnisn.acrhq.CloudFSProcessor.Verb;

public class LoginProcessor extends CloudFSProcessor implements Processor {
	protected Logger log = LoggerFactory.getLogger(getClass());

	final private String authenticationUrl;
	final private ObjectMapper objectMapper;
	final private String accIdAndAppKey;

	
	public LoginProcessor(ObjectMapper objectMapper, String aUrl, String accIdAndAppKey) {
		this.authenticationUrl = aUrl;
		this.objectMapper = objectMapper;
		this.accIdAndAppKey = accIdAndAppKey;
	}
	
	private String getAuthToken(CamelContext context) {
		
		String ans = null;
		
		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

			final HttpGet request = new HttpGet(authenticationUrl);
			request.setHeader("Authorization", "Basic " + accIdAndAppKey);
			
			HttpResponse response = httpclient.execute(request);
			
			if (response.getStatusLine().getStatusCode() == 200) {
				ByteArrayOutputStream buf = new ByteArrayOutputStream();
				response.getEntity().writeTo(buf);
				ans = objectMapper.readValue(buf.toString("UTF-8"), B2Response.class).getAuthorizationToken(); 
				log.debug("Received authorizationToken: " + ans);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ans;
	}
	
	private AuthResponse doB2Auth(final CamelContext context) {
		AuthResponse auth = null;
		ProducerTemplate b2producerTemplate = context.createProducerTemplate();

		final Message responseOut = b2producerTemplate.send(http4Suffix(getHttp4Proto(authenticationUrl)), new Processor() {
			public void process(Exchange webExch) throws Exception {
				log.debug(String.format("Authentication request sentTo(authenticationUrl=%s)", authenticationUrl));
				webExch.getIn().removeHeaders("*");
				webExch.getIn().setHeader("Authorization", "Basic " + accIdAndAppKey);
			}
		}).getOut();
		
		log.debug(responseOut.getHeader(Exchange.HTTP_RESPONSE_TEXT, String.class));
		
		try {
			b2producerTemplate.stop();
			if ( responseOut.getHeader(Exchange.HTTP_RESPONSE_TEXT, Integer.class) == 200 ) {
				auth = objectMapper.readValue(responseOut.getBody(String.class), AuthResponse.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return auth;
	}
	
	@Override
	public void process(Exchange exchange) {
		exchange.getOut().copyFrom(exchange.getIn());

		AuthResponse authResponse = getReply(exchange, Verb.authorizeService, AuthResponse.class);
		
		if ( authResponse == null || authResponse.isExpired() ) {
			
			log.info("Authentication required!");

			authResponse = doB2Auth(exchange.getContext());
			
			// Set NEW authResponse
			setReply(exchange, Verb.authorizeService, authResponse);
			
			log.debug("Received AuthorizationToken: " + authResponse.getAuthorizationToken());
		}
		else {
			log.info("Not renewing Authentication");
			log.info("Existing Token: " + authResponse.getAuthorizationToken());
		}
	}
}
