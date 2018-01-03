package com.rdnisn.acrhq;

import static com.rdnisn.acrhq.RemoteStorageAPI.getHttp4Proto;
import static com.rdnisn.acrhq.RemoteStorageAPI.http4Suffix;
import static com.rdnisn.acrhq.RemoteStorageAPI.sha1;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.restlet.data.MediaType;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.InputRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.rdnisn.acrhq.CloudFSProcessor.Verb;

/**
 * Base Router
 */
public class ZRouteBuilder extends RouteBuilder {
	
	private static final String DIRECTORY_SEP = "\\^";

	private Logger log = LoggerFactory.getLogger(getClass());

	private final CloudFSConfiguration serviceConfig;
	private final ObjectMapper objectMapper;
	private final String headerForAuthorizeAccount;

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
		// allow Jackson json to convert to pojo types also
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
//		.continued(true)
		.setBody(constant("There will be HttpOperationFailedException blood because..:\n${header.cause}"));
	}

	private OnExceptionDefinition generalExceptionHandler() {
		return onException(Exception.class).process(exchange ->  {
			exchange.getOut().setHeader("cause", exchange.getException());
			exchange.getOut().setBody(exchange.getIn().getBody());
			log.debug("except: " , exchange.getException()); //	HttpOperationFailedException exe = exchange.getException(HttpOperationFailedException.class);
		})
		.log("Not handled")
		.handled(false) //	.continued(true)
		.setBody(constant("cause: ${header.cause}"));
	}

	private String getLocalURL(UserFile x) {
		return x.getFilepath().toUri().toString().replaceFirst(
			"file://"+ serviceConfig.getDocRoot(),
			serviceConfig.getProtocol() + "://" + serviceConfig.getHost()
		);
	}
	
	private UploadData saveLocally(final Exchange exchange) throws UnsupportedEncodingException{
		Message messageIn = exchange.getIn();
		
        MediaType mediaType = messageIn.getHeader(Exchange.CONTENT_TYPE, MediaType.class);
        String destDir = java.net.URLDecoder.decode(messageIn.getHeader("destDir", String.class), "UTF-8")
        		.replaceAll(DIRECTORY_SEP, "/");
        
        log.debug("Destination directory: " + destDir);
        
        InputRepresentation representation =
            new InputRepresentation(messageIn.getBody(InputStream.class), mediaType);

		UploadData uploadData = null;
		
        try {
            List<FileItem> items = 
                new RestletFileUpload( new DiskFileItemFactory()).parseRepresentation(representation);

            if (! items.isEmpty()) {
            	
            		uploadData = new UploadData();
            	
            		for (FileItem item : items) {
            			if (item.isFormField()) {
            				uploadData.putFormField(item.getFieldName(), item.getString());
            			}
            			else {
            				Path destination = Paths.get(destDir, item.getName());
            				Files.createDirectories(destination.getParent());
    	                		Files.copy(item.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
    	                		UserFile uf = new UserFile(destination, item.getFieldName());
//    	                		uf.setContentType(item.getContentType());
            				uploadData.addFile(uf);
            			}
            		}
            }
        } catch (FileUploadException | IOException e) {
            e.printStackTrace();
        }
        return uploadData;
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
		.componentProperty("urlDecodeHeaders", "true")
		.bindingMode(RestBindingMode.json)
		.skipBindingOnErrorCode(false)
		.dataFormatProperty("prettyPrint", "true")
		
		.host(serviceConfig.getHost())
		.port(serviceConfig.getPort())
		.bindingMode(RestBindingMode.auto)
		.componentProperty("chunked", "true");

		rest(serviceConfig.getContextUri()).produces("application/json")
		
			// Upload a File
	        .post("/new/{destDir}").to("direct:rest.upload").outType(Map.class)

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
	from("direct:auth").process(new LoginProcessor(getContext(), objectMapper,
			http4Suffix(getHttp4Proto(serviceConfig.getRemoteAuthenticationUrl())), headerForAuthorizeAccount));

	// Replies -> List of buckets
	from("direct:rest.list_buckets")
		.to("direct:auth", "direct:listdir");
	
	// Replies -> HREF to resource
	from("direct:rest.upload").to("direct:localsave");
	
	from("direct:localsave")
		.to("direct:locprocdata").wireTap("direct:backproc")
		.to("direct:uploadreply");
		
	ZRouteBuilder router = this;
	from("direct:locprocdata")
		.process(exchange -> CloudFSProcessor.setReply(exchange, Verb.transientUpload, saveLocally(exchange)));
	
	
	from("direct:uploadreply")
		.process(new CloudFSProcessor() {
		    @Override
		    public void process(Exchange exchange) throws JsonProcessingException {
		    		UploadData obj = (UploadData) getReply(exchange, Verb.transientUpload);
		    		Map<String, String> filemap = obj.getFiles().stream().collect(Collectors.toMap(router::getLocalURL, uf -> uf.getName() ));
		    		String sjson = objectMapper.writeValueAsString(filemap);
				exchange.getOut().setBody(sjson);
		    }
		});

	
	 class FileSplitter implements Expression {
        @Override
        @SuppressWarnings("unchecked")
        public <T> T evaluate(Exchange exchange, Class<T> type) {
        		exchange.getOut().copyFrom(exchange.getIn());
            final UploadData body = exchange.getIn().getHeader("locprocdata", UploadData.class);
            return (T) body.getFiles();
        }
	 }
	 
	 from("direct:backproc")
		.to("direct:auth")
		.split(new FileSplitter())
		.to("direct:get_up_url", "direct:processFileItem")
    .end();
//	.process(exchange -> {
//		exchange.getOut().setBody(objectMapper.writeValueAsString(exchange.getIn().getBody()));
//	}).to("file:upload");
//	.process(ex -> { UploadData up = ex.getIn().getHeader("locprocdata", UploadData.class); log.debug(up.getFiles());})
//	.pipeline().to("direct:auth", "direct:get_up_url", "direct:upload");
	
	final Processor processFileItem = new CloudFSProcessor() {
	    @Override
	    public void process(Exchange exchange) throws Exception {
	    	
//	    		final B2Response authBody = (B2Response) getReply(exchange, Verb.authorizeService);
	    		UserFile userFile = exchange.getIn().getBody(UserFile.class);
	    		
	    		String authToken = (String) CloudFSProcessor.getReply(exchange, Verb.authToken);
	    		String uploadUrl = (String) CloudFSProcessor.getReply(exchange, Verb.uploadUrl);
	    		String uploadToken = (String) CloudFSProcessor.getReply(exchange, Verb.uploadToken);

	    		log.debug("authToken: " + authToken);
	    		log.debug("uploadUrl: " + uploadUrl);
	    		log.debug("uploadToken: " + uploadToken);
		log.debug("procFile: " + userFile.getName());
		log.debug("procFile: " + userFile.getFilepath().toString());
		
		


		 final MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		 entityBuilder.addBinaryBody(userFile.getName(), userFile.getFilepath().toFile());
		 
		 String SHA = sha1(userFile.getFilepath().toFile());
		    
		    HttpPost request = new HttpPost(uploadUrl);
		    request.setHeader("Authorization", uploadToken);
		    request.setHeader("Content-Type", userFile.getContentType());
//		    request.setHeader("Content-Length", upfile.length() + "");
		    request.setHeader("X-Bz-Content-Sha1", "do_not_verify");
		    request.setHeader("X-Bz-File-Name", userFile.getFilepath().getFileName().toString());
//		    request.setHeader("X-Bz-Info-Author", "unknown");
		    request.setEntity(entityBuilder.build());


		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

			HttpResponse response = httpclient.execute(request);

			java.io.ByteArrayOutputStream buf = new ByteArrayOutputStream();
			response.getEntity().writeTo(buf);
			exchange.getOut().setBody(buf.toString("UTF-8"));
		}

		

	    }
	};
	
	from("direct:processFileItem")
	.process(processFileItem)
	.log("BackBlaze response:\n${body}");

	from("direct:listdir")
	.process(new CloudFSProcessor() {
		
	    @Override
	    public void process(Exchange exchange) throws Exception {

	    	final B2Response authBody = (B2Response) getReply(exchange, Verb.authorizeService);
		exchange.getOut().copyFrom(exchange.getIn());

		final Message responseOut = getContext().createProducerTemplate()
			.send(getHttp4Proto(authBody.getApiUrl() + "/b2api/v1/b2_list_buckets"), innerExchg -> {
				innerExchg.getIn().setBody(objectMapper.writeValueAsString(ImmutableMap.of(
					"accountId", authBody.getAccountId()
					,"bucketTypes", new ArrayList<String>() {{add("allPrivate");add("allPublic");}}
				)));
				innerExchg.getIn().setHeader("Authorization", authBody.getAuthorizationToken());
		}).getOut();
		
		int	responseCode = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
		
		exchange.getOut().setBody(responseOut.getBody(String.class));
		log.debug("inner responseCode " + responseCode);
	    }
	    
	});
	
	from("direct:get_up_url")
		.process(exchange -> {
			final B2Response authBody = (B2Response) CloudFSProcessor.getReply(exchange, Verb.authorizeService);
			exchange.getOut().copyFrom(exchange.getIn());
			
			log.debug("Get upload URL");

			final Message responseOut = getContext().createProducerTemplate()
				.send(getHttp4Proto(authBody.getApiUrl() + "/b2api/v1/b2_get_upload_url"), innerExchg -> {
					
					log.debug(String.format("Request(%s)", authBody.getApiUrl() + "/b2api/v1/b2_get_upload_url"));
					
					innerExchg.getIn()
						.setHeader("Authorization", authBody.getAuthorizationToken());
					
					innerExchg.getIn()
						.setBody(objectMapper.writeValueAsString(ImmutableMap.of("bucketId", "2ab327a44f788e635ef20613")));
			}).getOut();
			
			try {
				B2Response authResponse = objectMapper.readValue(responseOut.getBody(String.class), B2Response.class);
				CloudFSProcessor.setReply(exchange, Verb.uploadUrl, authResponse.getUploadUrl());
				CloudFSProcessor.setReply(exchange, Verb.uploadToken, authResponse.getAuthorizationToken());
				authBody.setUploadUrl(authResponse.getUploadUrl());
				authBody.setBucketId(authResponse.getBucketId());
//				exchange.getOut().setHeader("Authorization", authResponse.getAuthorizationToken());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		});

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
