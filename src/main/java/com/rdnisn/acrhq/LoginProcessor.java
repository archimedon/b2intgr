package com.rdnisn.acrhq;

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

	//	final private static final long TTL = 10;
	final private static long TTL = 12 * 60 * 58;
	
	final private String authenticationUrl;
	final private ObjectMapper objectMapper;
	final private String accIdAndAppKey;
    
	private static String authorizationToken;
	private static long lastmod = 0;

	
	public LoginProcessor(CamelContext context, ObjectMapper objectMapper, String aUrl, String accIdAndAppKey) {
		this.authenticationUrl = aUrl;
		this.objectMapper = objectMapper;
		this.accIdAndAppKey = accIdAndAppKey;
		this.authorizationToken = getAuthToken(context);
		this.lastmod = utcInSecs();

		log.info("authenticationUrl: " + authenticationUrl);
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
	
	@Override
	public void process(Exchange exchange) {
		exchange.getOut().copyFrom(exchange.getIn());

		if (lastmod <= 0 || noToken() ) {
			log.info("Authentication required!");

			final ProducerTemplate b2producerTemplate = exchange.getContext().createProducerTemplate();
			
			final Message responseOut = b2producerTemplate.send(authenticationUrl, new Processor() {
				public void process(Exchange webExch) throws Exception {
					log.debug(String.format("Authentication request sentTo(authenticationUrl=%s)", authenticationUrl));
					webExch.getIn().removeHeaders("*");
					webExch.getIn().setHeader("Authorization", "Basic " + accIdAndAppKey);
				}
			}).getOut();

			int	responseCode = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
		
			log.debug("responseCode " + responseCode);
			try {
				B2Response authResponse = objectMapper.readValue(responseOut.getBody(String.class), B2Response.class);
				b2producerTemplate.stop();
				log.debug("Received auth accIdAndAppKey: " + authResponse.getAuthorizationToken());
//				lastmod = hasToken() ? new Date().getTime() : -1;
				lastmod = authResponse.getStatus() == null && authResponse.getAuthorizationToken() != null ? utcInSecs() : -1;
				log.debug("lastmod " + lastmod);
				setReply(exchange, Verb.authorizeService, authResponse);
				setReply(exchange, Verb.authToken, authResponse.getAuthorizationToken());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			log.info("Not renewing Authentication");
			log.info("Existing key is: " + LoginProcessor.authorizationToken);
		}
		
//		log.debug("LoginProcessor-AuthRep: " + authResponse);
//		Object obj = exchange.getIn().getBody();
//		if (obj != null) {
//			exchange.getOut().setBody(obj);
//			log.debug("LoginProcessor-HASBODY: Ywah");
//		}
//		exchange.getOut().setHeader("apiUrl", authResponse.getApiUrl());
//		exchange.getOut().setHeader("downloadUrl", authResponse.getDownloadUrl());
//		exchange.getOut().setHeader("authorizationToken", authResponse.getAuthorizationToken());
//		exchange.getOut().setHeader("accountId", authResponse.getAccountId());
	}
	
	private boolean noToken() {
		return	
			StringUtils.isBlank(authorizationToken) || ( utcInSecs() - lastmod) >= TTL;
	}
	

	private long utcInSecs() {
		return new Date().getTime() / 1000;
	}

	public String getAuthorizationToken() {
		return authorizationToken;
	}
}
