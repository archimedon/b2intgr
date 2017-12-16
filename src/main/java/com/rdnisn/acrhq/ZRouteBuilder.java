package com.rdnisn.acrhq;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restlet.Request;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.rdnisn.acrhq.RemoteStorageAPI.getHttp4Proto;
import static com.rdnisn.acrhq.RemoteStorageAPI.http4Suffix;

/**
 * A Camel Java8 DSL Router
 */
public class ZRouteBuilder extends RouteBuilder {
	
    final private static Log log = LogFactory.getLog(ZRouteBuilder.class);

	final private CloudFSConfiguration serviceConfig;
	final private ObjectMapper objectMapper;
	final private String headerForAuthorizeAccount;
	final private String authConsEndPt;
	
	public ZRouteBuilder(ObjectMapper objectMapper, String configFile) throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		super();
		this.objectMapper = objectMapper;
		this.serviceConfig = 
				objectMapper.readValue(new FileInputStream(configFile == null ?  "config.json" : configFile), CloudFSConfiguration.class);

		this.headerForAuthorizeAccount = Base64.getEncoder().encodeToString(
			(serviceConfig.getRemoteAccountId() + ":" + serviceConfig.getRemoteApplicationKey()).getBytes()
		);
		
		this.authConsEndPt = getHttp4Proto(serviceConfig.getRemoteAuthenticationUrl());
		
		// enable Jackson json type converter
		getContext().getProperties().put("CamelJacksonEnableTypeConverter", "true");
		// allow Jackson json to convert to pojo types also (by default jackson only
		// converts to String and other simple types)
		getContext().getProperties().put("CamelJacksonTypeConverterToPojo", "true");
		
		System.err.println(authConsEndPt);
	}
	
    public void processMultiPart(final Exchange exchange) throws Exception {
        File filePayload = null;
        Object obj = exchange.getIn().getMandatoryBody();
    }


	/**
	 * Routes ...
	 */
	public void configure() {
        
		onException(HttpOperationFailedException.class)
        .onWhen(exchange -> {
          HttpOperationFailedException exe = exchange.getException(HttpOperationFailedException.class);
          return exe.getStatusCode() > 204;
        })
        .log("HTTP exception handled")
        .handled(true)
        //.continued(true)
        .setBody(constant("There will be blood"));

		onException(Exception.class)
		.log("Not handled")
		.handled(true)
		//.continued(true)
		.setBody(constant("There will be blood"));
		

		Pinger ping = new Pinger(
				getContext(), objectMapper,
				http4Suffix(authConsEndPt), headerForAuthorizeAccount);


//		<b>b2_get_upload_url</b>
//		export BUCKET_ID=json.bucketId
//		export BUCKET_ID="fa73d7e42f083e836e020613"
//
//		curl \
//			-H "Authorization: ${ACCOUNT_AUTHORIZATION_TOKEN}" \
//			-d "{\"bucketId\": \"${BUCKET_ID}\"}" \
//			${API_URL}/b2api/v1/b2_get_upload_url
//		{
//		  "authorizationToken": "3_20171208215936_9ee5d859568e5cba8a30de02_e007914bf676280d4028d91ced6e22de1ad62ab3_001_upld",
//		  "bucketId": "fa73d7e42f083e836e020613",
//		  "uploadUrl": "https://pod-000-1090-17.backblaze.com/b2api/v1/b2_upload_file/fa73d7e42f083e836e020613/c001_v0001090_t0028"
//		}exchange ->
		
		Processor getUploadUrlProcessor = new Processor() {
			
			
			@Override
			public void process(Exchange exchange) throws Exception {
				// TODO Auto-generated method stub
				final B2Response authBody = exchange.getIn().getBody(B2Response.class);
				final String getuploadUrl = getHttp4Proto(authBody.getApiUrl() + "/b2api/v1/b2_get_upload_url");
				System.err.println("getuploadUrl " + getuploadUrl);
				
				final Message responseOut = getContext().createProducerTemplate()
						.send(getuploadUrl, innerExchg -> {
							
							innerExchg.getIn().setBody(objectMapper.writeValueAsString(new HashMap<String, Object>() {{
								put("bucketId", "2ab327a44f788e635ef20613");
							}}));
							
							innerExchg.getIn().setHeader("Authorization", exchange.getIn().getHeader("Authorization"));
						}).getOut();
				
				int	responseCode = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
				String responseBody = responseOut.getBody(String.class);
				
				exchange.getOut().setBody(responseBody);
				System.err.println("inner responseCode " + responseCode);
				System.err.println("inner responseBody " + responseBody);
			}
		};


//	from("timer:healthCheck?period=2000")
//	  .pipeline().to("direct:auth", "direct:listdir"); // .to(ping, new AuthNProcessor(objectMapper))
		
	from("direct:auth").process(ping);

	from("direct:listDirectory")
		.pipeline().to("direct:auth", "direct:listdir");
	
	from("direct:uploadFile")
		.pipeline().to("direct:auth", "direct:get_up_url", "direct:upload");
	
	
	from("direct:listdir").inputType(B2Response.class)
	.process(exchange -> {
		final B2Response authBody = exchange.getIn().getBody(B2Response.class);
		final String listUrl = getHttp4Proto(authBody.getApiUrl() + "/b2api/v1/b2_list_buckets");
		System.err.println("listUrl " + listUrl);
		
		final Message responseOut = getContext().createProducerTemplate()
			.send(listUrl, innerExchg -> {

				innerExchg.getIn().setBody(objectMapper.writeValueAsString(new HashMap<String, Object>() {{
					put("accountId", authBody.getAccountId());
					put("bucketTypes", new ArrayList<String>() {{add("allPrivate");add("allPublic");}});
				}}));
				
				System.err.println("Authorization: " + exchange.getIn().getHeader("Authorization"));
				innerExchg.getIn().setHeader("Authorization", exchange.getIn().getHeader("Authorization"));
		}).getOut();
		
		int	responseCode = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
		String responseBody = responseOut.getBody(String.class);
		exchange.getOut().setBody(responseBody);
		System.err.println("inner responseCode " + responseCode);
	});
	
	from("direct:get_up_url")
		.process(getUploadUrlProcessor);
	
	
	from("direct:upload").inputType(B2Response.class)
	.process();
	
	
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
//        .put("/mod/{filePath}").to("direct:putFile")

        // List Directory
        .get("/ls/{dirPath}").to("direct:listDirectory")

        // Get file info
////        .get("/file/{filePath}").to("direct:infoFile")

        // Delete file
//        .delete("/rm/{filePath}").to("direct:listDirectory")
        ;
	
//    from("direct:uploadFile").        
//	to("log:block");
//    from("direct:putFile").to("file://putFile");
//    from("direct:listDirectory").process(echoP);
//    from("direct:infoFile").to("file://infoFile");
//    from("test-jms:test.queue")
//    .wireTap("direct:uploadFile")
//    .process(echoP);
//    
//	 from("direct:uploadFile").to("file://uploadFile");
//	 
//	 
//	 
//     .get("/").to("direct:uploadFile")
//     .get().to("direct:uploadFile")
//
//		.produces("application/json")
//		.bindingMode(RestBindingMode.json)
//         .post().to("direct:uploadFile")
//         .put().to("direct:putFile")
//         .get().to("direct:uploadFile")
////         .get().to("direct:listDirectory")
//         .get("/{fileId}").to("direct:getFile")
//         .delete("/{fileId}").to("direct:deleteFile")
//         ;

         
//Message out = exchange.getOut();
//int responseCode = out.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
//
//System.err.println("responseCode: " + responseCode);
//System.err.println("obod: " + out.getBody(String.class));
//	});

//	exchange.getOut().copyFrom(exchange.getIn());
//	authResponse = authenticate();
//	exchange.getOut().setBody(authResponse);
//	exchange.getOut().removeHeaders("*");
//	exchange.getOut().setHeader("Authorization",  authResponse.getAuthorizationToken());

//	.setHeader("Authorization", constant("Basic ${header.authorizationToken}"))
//	.to("http4://google.com")
	;
	
//
////  .process(authNProcessor).process(exchange -> {
//		curl \
//	    -H "Authorization: ${ACCOUNT_AUTHORIZATION_TOKEN}" \
//	    -d "{\"accountId\": \"$ACCOUNT_ID\", \"bucketTypes\": [\"allPrivate\",\"allPublic\"]}" \
//	    "${API_URL}/b2api/v1/b2_list_buckets"    
//
//	{
//	  "buckets": [
//	    {
//	      "accountId": "a374f8e3e263",
//	      "bucketId": "2ab327a44f788e635ef20613",
//	      "bucketInfo": {},
//	      "bucketName": "b2public",
//	      "bucketType": "allPublic",
//	      "corsRules": [],
//	      "lifecycleRules": [],
//	      "revision": 2
//	    },
//	    {
//	      "accountId": "a374f8e3e263",
//	      "bucketId": "fa73d7e42f083e836e020613",
//	      "bucketInfo": {},
//	      "bucketName": "rdnisn-zcloudfs-public",
//	      "bucketType": "allPublic",
//	      "corsRules": [],
//	      "lifecycleRules": [],
//	      "revision": 1
//	    }
//	  ]
//	}
//
//	});
	  //        .to("bean:bean:ping?method=puff")
//
//		.to("bean:pinger?method=authenticate")
//		.setHeader("Authorization", constant("Basic " + headerForAuthorizeAccount))
//        .to(getHttp4Proto(serviceConfig.getRemoteAuthenticationUrl()))
//        .process(authNProcessor)
//        .to("file://authNProcessor")
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

	private void examine(Exchange exchange) {
		
		final Map<String, String> storeOut = new HashMap<String, String>();
		final Map<String, String> store = new HashMap<String, String>();
		store.put("IN.body: ", exchange.getIn().getBody(String.class));
		
		exchange.getIn().getHeaders().entrySet().forEach(entry ->
		{
			store.put(entry.getKey(), entry.getValue() + "");
		});

		storeOut.put("OUT.body: ", exchange.getOut().getBody(String.class));
		
		exchange.getOut().getHeaders().entrySet().forEach(entry ->
		{
			storeOut.put(entry.getKey(), entry.getValue() + "");
		});
		System.err.println("STORE: " + store);
		System.err.println("STORE toreOut: " + storeOut);

		exchange.getOut().setBody(store);
	}
}
