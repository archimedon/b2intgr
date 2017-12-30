package com.rdnisn.acrhq;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.restlet.RestletConstants;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.InputRepresentation;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdnisn.acrhq.CloudFSProcessor.UploadData;
import com.rdnisn.acrhq.CloudFSProcessor.Verb;

import static com.rdnisn.acrhq.RemoteStorageAPI.getHttp4Proto;
import static com.rdnisn.acrhq.RemoteStorageAPI.http4Suffix;
import static com.rdnisn.acrhq.RemoteStorageAPI.sha1;

/**
 * Base Router
 */
public class ZRouteBuilder extends RouteBuilder {
	
    final private static Log log = LogFactory.getLog(ZRouteBuilder.class);

	final private CloudFSConfiguration serviceConfig;
	final private ObjectMapper objectMapper;
	final private String headerForAuthorizeAccount;
	final private String docRoot = "/Users/ronalddennison/eclipse-workspace/acrhq";
	final private String domain = "http://test.rdnsn.com";
	
	public ZRouteBuilder(ObjectMapper objectMapper, String configFile) throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		super();
		this.objectMapper = objectMapper;
		this.serviceConfig = objectMapper.readValue(
			new FileInputStream(configFile == null ?  "config.json" : configFile),
			CloudFSConfiguration.class
		);

		this.headerForAuthorizeAccount = Base64.getEncoder().encodeToString(
			(serviceConfig.getRemoteAccountId() + ":" + serviceConfig.getRemoteApplicationKey()).getBytes()
		);
		
		// enable Jackson json type converter
		getContext().getGlobalOptions().put("CamelJacksonEnableTypeConverter", "true");
		// allow Jackson json to convert to pojo types also (by default jackson only
		// converts to String and other simple types)
		getContext().getGlobalOptions().put("CamelJacksonTypeConverterToPojo", "true");
	}

	private OnExceptionDefinition httpExceptionHandler() {
		return onException(HttpOperationFailedException.class).onWhen(exchange -> {
			if (exchange.isFailed()) {}
			exchange.getOut().setHeader("cause", exchange.getException().getMessage());
			HttpOperationFailedException exe = exchange.getException(HttpOperationFailedException.class);
			return exe.getStatusCode() > 204;
		})
				.log("HTTP exception handled")
				.handled(true)
//.continued(true)
				.setBody(constant("There will be HttpOperationFailedException blood because..:\n${header.cause}"));
	}
	
	private OnExceptionDefinition generalExceptionHandler() {
		
		return onException(Exception.class).process(exchange ->  {
	exchange.getOut().setHeader("cause", exchange.getException());
			exchange.getOut().setBody(exchange.getIn().getBody());
			log.debug("except: " , exchange.getException());
//	HttpOperationFailedException exe = exchange.getException(HttpOperationFailedException.class);
		})
				.log("Not handled")
				.handled(false)
//.continued(true)
				.setBody(constant("cause: ${header.cause}"));
		
	}
	 
	private String getLocalURL(Map.Entry<Path, String> x) {
		return x.getKey().toUri().toString().replaceFirst("file://" + docRoot, domain);
	}
	/**
	 * Routes ...
	 */
	public void configure() {
        
		
		
		httpExceptionHandler();
		generalExceptionHandler();
		

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
	        .post("/new/{filePath}").to("direct:rest.upload").outType(Map.class)

	        // Update a File
//	        .put("/mod/{filePath}").to("direct:putFile")

	        // List Buckets
	        .get("/ls").to("direct:rest.list_buckets")
	        
	        // List Directory
//	        .get("/ls/{dirPath}").to("direct:rest.lsdir")

	        // Get file info
////	        .get("/file/{filePath}").to("direct:infoFile")

	        // Delete file
//	        .delete("/{filePath}").to("direct:rest.rm")
//	        .delete("/dir/{dirPath}").to("direct:rest.rmdir")
	        ;
		
		
	// Replies -> Authentication
	from("direct:auth").process(new Pinger(
			getContext(), objectMapper,
			http4Suffix(getHttp4Proto(serviceConfig.getRemoteAuthenticationUrl())), headerForAuthorizeAccount));

	// Replies -> List of buckets
	from("direct:rest.list_buckets")
		.pipeline().to("direct:auth", "direct:listdir");
	
	// Replies -> HREF to resource
	from("direct:rest.upload").to("direct:localsave");
	
	
	from("direct:localsave")
		.to("direct:locprocdata")
		.wireTap("direct:backproc")
		.to("direct:uploadreply");
		
	
	
	from("direct:locprocdata").process(new CloudFSProcessor() {
		
	    @Override
	    public void process(Exchange exchange) throws Exception {
	    		setReply(exchange, Verb.transientUpload, saveLocally(exchange.getIn()));
	    }
	});
	
	from("direct:uploadreply")
		.process(exchange -> {
			exchange.getOut()
					.setBody(objectMapper
							.writeValueAsString(exchange.getIn().getBody(UploadData.class).getFiles().entrySet()
									.stream().collect(Collectors.toMap(this::getLocalURL, f -> f.getValue()))));
		});

	from("direct:backproc")
	.process(exchange -> {
		exchange.getOut().setBody(objectMapper.writeValueAsString(exchange.getIn().getBody()));
	}).to("file:upload");
//	.process(ex -> { UploadData up = ex.getIn().getHeader("locprocdata", UploadData.class); System.out.println(up.getFiles());})
//	.pipeline().to("direct:auth", "direct:get_up_url", "direct:upload");
	
	
	from("direct:listdir")
	.process(new CloudFSProcessor() {
		
	    @Override
	    public void process(Exchange exchange) throws Exception {

	    	final B2Response authBody = (B2Response) getReply(exchange, Verb.authorizeService);
		exchange.getOut().copyFrom(exchange.getIn());

		final Message responseOut = getContext().createProducerTemplate()
			.send(getHttp4Proto(authBody.getApiUrl() + "/b2api/v1/b2_list_buckets"), innerExchg -> {
				innerExchg.getIn().setBody(objectMapper.writeValueAsString(new HashMap<String, Object>() {{
					put("accountId", authBody.getAccountId());
					put("bucketTypes", new ArrayList<String>() {{add("allPrivate");add("allPublic");}});
				}}));
				
				innerExchg.getIn().setHeader("Authorization", authBody.getAuthorizationToken());
		}).getOut();
		
		int	responseCode = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
		
		exchange.getOut().setBody(responseOut.getBody(String.class));
		log.debug("inner responseCode " + responseCode);
	    }
	    
	});
	
	from("direct:get_up_url")
	.process(fin -> log.debug("locprocdata ---> " + fin.getIn().getHeader("locprocdata")));
//		.process(exchange -> {
//			final B2Response authBody = exchange.getIn().getHeader(Pinger.B2AUTHN, B2Response.class);
//			exchange.getOut().copyFrom(exchange.getIn());
//			log.debug("direct:get_up_url authBody " + authBody);
//
//			final Message responseOut = getContext().createProducerTemplate()
//				.send(getHttp4Proto(authBody.getApiUrl() + "/b2api/v1/b2_get_upload_url"), innerExchg -> {
//
//					innerExchg.getIn().setBody(objectMapper.writeValueAsString(new HashMap<String, Object>() {{
//						put("bucketId", "2ab327a44f788e635ef20613");
//					}}));
//					
//					log.debug("Authorization HDR: " + exchange.getIn().getHeader("Authorization"));
//					log.debug("Authorization USED: " + authBody.getAuthorizationToken());
//					innerExchg.getIn().setHeader("Authorization", authBody.getAuthorizationToken());
//			}).getOut();
//			
//			try {
//				B2Response authResponse = objectMapper.readValue(responseOut.getBody(String.class), B2Response.class);
//				authBody.setUploadUrl(authResponse.getUploadUrl());
//				authBody.setBucketId(authResponse.getBucketId());
//				exchange.getOut().setHeader("Authorization", authResponse.getAuthorizationToken());
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			
//		});

	Processor p = new Processor() {

	    @Override
	    public void process(Exchange exchange) throws Exception {

	        MediaType mediaType = exchange.getIn().getHeader(Exchange.CONTENT_TYPE, MediaType.class);
	        InputRepresentation representation =
	            new InputRepresentation(exchange.getIn().getBody(InputStream.class), mediaType);

	        try {
	            List<FileItem> items = 
	                new RestletFileUpload(
	                    new DiskFileItemFactory()).parseRepresentation(representation);

	            for (FileItem item : items) {
	                if (item.isFormField()) {
	                		log.debug("item: " + item.getFieldName() + " : " + item.getString());
	                }
	                else {
	                	InputStream inputStream = item.getInputStream();
	                	Path destination = Paths.get("MyFile.png");
	                	Files.copy(inputStream, destination,
	                			StandardCopyOption.REPLACE_EXISTING);
	                	log.debug("file item: " + item.getName());
	                }
	            }
	        } catch (FileUploadException | IOException e) {
	            e.printStackTrace();
	        }


	    }

	};
/*

curl \
    -H "Authorization: $UPLOAD_AUTHORIZATION_TOKEN" \
    -H "X-Bz-File-Name: ${FILE_REMOTE_NAME}" \
    -H "Content-Type: $MIME_TYPE" \
    -H "X-Bz-Content-Sha1: $SHA1_OF_FILE" \
    -H "X-Bz-Info-Author: unknown" \
    --data-binary "@$FILE_TO_UPLOAD" \
    $UPLOAD_URL    

*/
	
//	.process(ex -> toMultipart(ex));
	from("direct:upload")
		.process(exchange -> {
			final B2Response authBody = exchange.getIn().getHeader(Pinger.B2AUTHN, B2Response.class);
			exchange.getOut().copyFrom(exchange.getIn());

			log.debug("uploadUrl " + authBody.getUploadUrl());

			
			 final MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
			 UploadData data = exchange.getIn().getHeader("locprocdata", UploadData.class);
			 data.getFiles().entrySet().forEach(ent -> {
				 entityBuilder.addBinaryBody(ent.getValue(), ent.getKey().toFile());
			 });
			 data.getMeta().entrySet().forEach(ent -> {
				 entityBuilder.addTextBody(ent.getKey(), ent.getValue());
			 });
			 

//			  // Set multipart entity as the outgoing message’s body…
//			  exchange.getOut().setBody(entityBuilder.build());
			  
			 MultipartEntity entity = new MultipartEntity();
			 File upfile = new File("ReadMe.txt");
			 log.debug("upfile.length() " + upfile.length());
			 log.debug("sha1(upfile) " + sha1(upfile));
			 
			    entity.addPart("file", new FileBody(upfile));

			    String SHA = sha1(upfile);
			    
			    HttpPost request = new HttpPost(authBody.getUploadUrl());
			    request.setHeader("Authorization", authBody.getAuthorizationToken());
			    request.setHeader("Content-Type", "b2/x-auto");
//			    request.setHeader("Content-Length", upfile.length() + "");
			    request.setHeader("X-Bz-Content-Sha1", "do_not_verify");
			    request.setHeader("X-Bz-File-Name", "dennison-test.ReadMe.txt");
//			    request.setHeader("Content-Type", "text/plain");
//			    request.setHeader("X-Bz-Info-Author", "unknown");
			    request.setEntity(entity);

			    log.debug("request");
			    for (org.apache.http.Header h: request.getAllHeaders()) {
			    		log.debug(h.getName() + " : " +  h.getValue());
			    }
			    
			    
			    		
			    
			    HttpClient client = new DefaultHttpClient();
			    HttpResponse response = client.execute(request);
			    log.debug("response:");
			    for (org.apache.http.Header h: response.getAllHeaders()) {
		    		log.debug(h.getName() + " : " +  h.getValue());
			    }

//			
			log.debug("uploadUrl getStatusLine " + response.getStatusLine());
			
			
			java.io.ByteArrayOutputStream buf = new ByteArrayOutputStream();
			response.getEntity().writeTo(buf);
			exchange.getOut().setBody(buf.toString("UTF-8"));
		});
	
	}

	public void toMultipart(Exchange exchange) {

	  // Read the incoming message…
		File file = exchange.getIn().getBody(File.class);
		String name = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);

		 log.debug("upName: " + name);
	  // Encode the file as a multipart entity…
	MultipartEntityBuilder entity = MultipartEntityBuilder.create();
	  entity.addBinaryBody("file", file);
	  entity.addTextBody("name", name);

	  // Set multipart entity as the outgoing message’s body…
	  exchange.getOut().setBody(entity.build());
	}
}
