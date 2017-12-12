package com.rdnisn.acrhq;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.restlet.RestletConstants;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restlet.Request;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * A Camel Java8 DSL Router
 */
public class ZRouteBuilder extends RouteBuilder {
	
    final private static Log log = LogFactory.getLog(ZRouteBuilder.class);

	final private CloudFSConfiguration serviceConfig;
	final private ObjectMapper objectMapper;
	final private AuthNProcessor authNProcessor; 

	public ZRouteBuilder(ObjectMapper objectMapper, String configFile) throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		super();
		this.objectMapper = objectMapper;
		this.authNProcessor = new AuthNProcessor(objectMapper);
		this.serviceConfig = 
		objectMapper.readValue(new FileInputStream(configFile == null ?  "config.json" : configFile), CloudFSConfiguration.class);
		
		// enable Jackson json type converter
		getContext().getProperties().put("CamelJacksonEnableTypeConverter", "true");
		// allow Jackson json to convert to pojo types also (by default jackson only
		// converts to String and other simple types)
		getContext().getProperties().put("CamelJacksonTypeConverterToPojo", "true");
	}

	 
    public void processMultiPart(final Exchange exchange) throws Exception {
        File filePayload = null;
        Object obj = exchange.getIn().getMandatoryBody();
    }

    String getHttp4Proto(String url) {
		String str = url;
		Matcher m = Pattern.compile("(https{0,1})(.+)").matcher(url);
		if (m.find()) {
			str = m.replaceFirst("$1" + "4$2"); 
		}
		
		return str;
	}

	/**
	 * Routes ...
	 */
	public void configure() {

		onException(HttpOperationFailedException.class)
        .onWhen(exchange -> {
          HttpOperationFailedException
              exe = exchange.getException(HttpOperationFailedException.class);
          return exe.getStatusCode() > 204;
        })
        .log("HTTP exception handled")
        .handled(true)
        //.continued(true)
        .setBody(constant("There will be blood"));
		
//		final BiFunction<String, Map<String, Object>, String > getB2URL = (String restVerb,  Map<String, Object> params) -> {
//			return  "";
//		};

//		class AuthResponse {
//			String accountId;
//			Integer absoluteMinimumPartSize;
//			String apiUrl;
//			String authorizationToken;
//			String downloadUrl;
//			Integer minimumPartSize;
//			Integer recommendedPartSize;
//		}
		
		// Confirm connection to B2
		String headerForAuthorizeAccount = Base64.getEncoder().encodeToString(
			(serviceConfig.getRemoteAccountId() + ":" + serviceConfig.getRemoteApplicationKey()).getBytes()
		);
		String authConsEndPt = getHttp4Proto(serviceConfig.getRemoteAuthenticationUrl());
		
		
		System.err.println(authConsEndPt);
//        ProducerTemplate template = getContext().createProducerTemplate();
//
//		Exchange exchange = template.send(authConsEndPt, new Processor() {
//			public void process(Exchange exchange) throws Exception {
//				exchange.getIn().removeHeaders("*");
//				exchange.getIn().setHeader("Authorization", constant("Basic " + headerForAuthorizeAccount));
////				exchange.getIn().setHeader(Exchange.HTTP_QUERY, constant("hl=en&q=activemq"));
////				final Map<String, String> store = new HashMap<String, String>();
////				store.put("request body", exchange.getIn().getBody(String.class));
////				
////				exchange.getIn().getHeaders().entrySet().forEach(entry ->
////				{
////					store.put(entry.getKey(), entry.getValue() + "");
////				});				
//			}
//		});
//		Message out = exchange.getOut();
//		int responseCode = out.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
//		
//		System.err.println("responseCode: " + responseCode);
//		System.err.println("obod: " + out.getBody(String.class));
//		
			 
		final Processor echoP = new Processor() {

			@Override
			public void process(Exchange exchange) throws Exception {
				final Message IN = exchange.getIn();
				final Message OUT = exchange.getOut();
				
				final Map<String, String> store = new HashMap<String, String>();
				store.put("request body", IN.getBody(String.class));
				
				IN.getHeaders().entrySet().forEach(entry ->
				{
					store.put(entry.getKey(), entry.getValue() + "");
				});

				String jsonInString = objectMapper.writeValueAsString(store);
				OUT.setBody(jsonInString);
			}
		};
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
//	    from("direct:start")
//	    .choice().when(new Predicate() {
//
//			@Override
//			public boolean matches(Exchange exchange) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//	    	
//	    }
//	    )
//	    .endChoice();

		from("timer:healthCheck?period=10000")
		.removeHeaders("*")
		.setHeader("Authorization", constant("Basic " + headerForAuthorizeAccount))
        .to(
        		authConsEndPt // http4s://api.backblazeb2.com/b2api/v1/b2_authorize_account
        		+ "?okStatusCodeRange=100-800&throwExceptionOnFailure=false"
        		+ "&disableStreamCache=true&transferException=true&useSystemProperties=true"
    		)
        .process(authNProcessor)
        .to("file://authNProcessor")
        ;

		/**
		 * Configure local Rest server
		 */
		restConfiguration()
		.component("restlet")
		.host(serviceConfig.getHost())
		.port(serviceConfig.getPort())
		.bindingMode(RestBindingMode.auto)
		.componentProperty("chunked", "true");

		rest(serviceConfig.getContextUri()).produces("application/json")
		
			// Upload a File
	        .post("/new/{filePath}").to("direct:uploadFile")

	        // Update a File
	        .put("/mod/{filePath}").to("direct:putFile")

	        // List Directory
	        .get("/ls/{dirPath}").to("direct:listDirectory")

	        // Get file info
//	        .get("/file/{filePath}").to("direct:infoFile")

	        // Delete file
	        .delete("/rm/{filePath}").to("direct:listDirectory");
		
//        from("direct:uploadFile").        
//		to("log:block");
        from("direct:putFile").to("file://putFile");
        from("direct:listDirectory").process(echoP);
        from("direct:infoFile").to("file://infoFile");
        from("test-jms:test.queue")
        .wireTap("direct:uploadFile")
        .process(echoP);
        
		 from("direct:uploadFile").to("file://uploadFile");
		 
		 
		 
//         .get("/").to("direct:uploadFile")
//         .get().to("direct:uploadFile")
//
//			.produces("application/json")
//			.bindingMode(RestBindingMode.json)
//	         .post().to("direct:uploadFile")
//	         .put().to("direct:putFile")
//	         .get().to("direct:uploadFile")
////	         .get().to("direct:listDirectory")
//	         .get("/{fileId}").to("direct:getFile")
//	         .delete("/{fileId}").to("direct:deleteFile")
	         ;

//			Map<String, String> obj = new HashMap<String, String>();
//			obj.put("ans", "reply");
//			String jsonInString ="Nope";
//			try {
//				jsonInString = objectMapper.writeValueAsString(obj);
//			} catch (JsonProcessingException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			log.info(jsonInString);
		 
//		 from("direct:uploadFile")
//		 	.process("b2ResponseProcessor(objectMapper)")
//		 	.transform(body().regexReplaceAll("\n", "<br/>"))
//		 	.wireTap("activemq:remoteUploadFile")
//		 	.to(getB2URL.apply("POST", new HashMap()))
			 
			 // process the response
//			 .process(echoP);
	}
}
