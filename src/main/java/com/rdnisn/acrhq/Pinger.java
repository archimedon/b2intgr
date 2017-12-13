package com.rdnisn.acrhq;

import java.util.Base64;
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

public class Pinger {
    final private static Log log = LogFactory.getLog(Pinger.class);

	
	
	final static Pattern pattern = Pattern.compile("(https{0,1})(.+)");
	
	final private CamelContext context;
	final private String authenticationUrl;
	final private ObjectMapper objectMapper;
	
	private B2Response authResponse;
	
	public Pinger(CamelContext ctx, ObjectMapper objectMapper, String aUrl) {
		this.context = ctx;
		this.authenticationUrl = mkHttp4B2(aUrl);
		this.objectMapper = objectMapper;
		log.info("authenticationUrl: " + authenticationUrl);
	}

	public B2Response authenticate(String token) {
		log.info("authenticating...");
		System.err.println("authenticating...");

		ProducerTemplate template = context.createProducerTemplate();
		System.err.println("createProducerTemplate...");
	
		Exchange myExch = template.send(authenticationUrl, new Processor() {
		public void process(Exchange exchange) throws Exception {
			
			System.err.println("process...");
			final Message IN = exchange.getIn();

//			final Message OUT = exchange.getOut();
			final String responseStr = IN.getBody(String.class);

			IN.removeHeaders("*");
			IN.setHeader("Authorization", "Basic " + token);

			authResponse = objectMapper.readValue(responseStr, B2Response.class);
//			IN.getHeaders().entrySet().forEach(entry -> map.put(entry.getKey(), entry.getValue() + ""));
			System.err.println("authResponse getMessage " + authResponse.getMessage());
			System.err.println("authResponse getStatus " + authResponse.getStatus());
			System.err.println("authResponse " + authResponse);
//
//			
//			Message out = myExch.getOut();
//			int responseCode = out.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
//			
//			System.err.println("responseCode: " + responseCode);
//			System.err.println("obod: " + out.getBody(String.class));
		}
	});
	
	return authResponse;
	}
	
	private String mkHttp4B2(String url) {
		String str = url;
		Matcher m = pattern.matcher(url);
		if (m.find()) {
			str = m.replaceFirst("$1" + "4$2"); 
		}
		return str + "?okStatusCodeRange=100-800&throwExceptionOnFailure=false"
		+ "&disableStreamCache=true&transferException=true&useSystemProperties=true";
	}


	public String getAuthenticationUrl() {
		return authenticationUrl;
	}



	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}



	public B2Response getAuthResponse() {
		return authResponse;
	}
}

/*


Exchange exch = context.createProducerTemplate().send(authenticationUrl, new Processor() {
	
	public void process(Exchange exchange) throws Exception {
		
		final Message IN = exchange.getIn();
		final Message OUT = exchange.getOut();

		final String responseStr = IN.getBody(String.class);
	    byte[] body = responseStr.getBytes();

		exchange.getIn().removeHeaders("*");
		exchange.getIn().setHeader("Authorization", "Basic " + token);
		
		authResponse = objectMapper.readValue(responseStr, B2Response.class);
//		IN.getHeaders().entrySet().forEach(entry -> map.put(entry.getKey(), entry.getValue() + ""));
		System.err.println("authResponse getMessage " + authResponse.getMessage());
		System.err.println("authResponse getStatus " + authResponse.getStatus());
		System.err.println("authResponse " + authResponse);
//		String json = objectMapper.writeValueAsString(authResponse);
//		System.err.println(json);
		OUT.setBody(responseStr);
		IN.setBody(responseStr);
		
		
		final Map<String, String> store = new HashMap<String, String>();
		
		IN.getHeaders().entrySet().forEach(entry ->
		{
			store.put(entry.getKey(), entry.getValue() + "");
		});

		
//		exchange.getIn().setHeader(Exchange.HTTP_QUERY, constant("hl=en&q=activemq"));
//		final Map<String, String> store = new HashMap<String, String>();
//		store.put("request body", exchange.getIn().getBody(String.class));
//		
//		exchange.getIn().getHeaders().entrySet().forEach(entry ->
//		{
//			store.put(entry.getKey(), entry.getValue() + "");
//		});				
	}
});
*/