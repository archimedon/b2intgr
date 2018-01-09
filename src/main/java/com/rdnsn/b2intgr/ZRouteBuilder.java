package com.rdnsn.b2intgr;

import static com.rdnsn.b2intgr.RemoteStorageAPI.getHttp4Proto;
import static com.rdnsn.b2intgr.RemoteStorageAPI.http4Suffix;
import static com.rdnsn.b2intgr.RemoteStorageAPI.sha1;

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
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.impl.ExpressionAdapter;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
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
import com.rdnsn.b2intgr.CloudFSProcessor.Verb;

/**
 * Base Router
 */
public class ZRouteBuilder extends RouteBuilder {
	
	private static final String DIRECTORY_SEP = "\\^";

	private Logger log = LoggerFactory.getLogger(getClass());

	private final CloudFSConfiguration serviceConfig;
	private final ObjectMapper objectMapper;
	private final String headerForAuthorizeAccount;
	private final AuthAgent authAgent;
//	private final JsonDataFormat jsonUploadAuthFormat;

	public ZRouteBuilder(ObjectMapper objectMapper, CloudFSConfiguration serviceConfig, AuthAgent authAgent) throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		super();
		this.objectMapper = objectMapper;
		this.serviceConfig = serviceConfig;
		this.authAgent = authAgent;
		this.headerForAuthorizeAccount = serviceConfig.getBasicAuthHeader();
//		this.jsonUploadAuthFormat = new JsonDataFormat(JsonLibrary.Jackson);
//		jsonUploadAuthFormat.setUnmarshalType(UploadAuthResponse.class);
		// enable Jackson json type converter
		getContext().getGlobalOptions().put("CamelJacksonEnableTypeConverter", "true");
		// allow Jackson json to convert to pojo types also
		getContext().getGlobalOptions().put("CamelJacksonTypeConverterToPojo", "true");
	}

	/**
	 * Routes ...
	 */
	public void configure() {

		httpExceptionHandler();
		generalExceptionHandler();
		RestDefinition res = defineRestServer(serviceConfig);

	// Replies -> List of buckets
	from("direct:rest.list_buckets")
		.enrich("bean:authAgent?method=getRemoteAuth", authAgent)
		.to("direct:listdir")
	.end();
	
	// Replies -> HREF to resource
	from("direct:rest.upload")
		.process(exchange -> exchange.getOut().setBody(saveLocally(exchange)))
		.wireTap("direct:b2upload")
		.to("direct:uploadreply")
	.end();

		

	Expression exp;
	
	ZRouteBuilder router = this;
		
	final Processor quickUp = new CloudFSProcessor() {
	    @Override
	    public void process(Exchange exchange) throws IOException {
	    	log.debug("quickUp: " + CloudFSProcessor.dumpExch(exchange));
	    	
	    	UploadData obj = exchange.getIn().getBody(UploadData.class);
	    		System.err.println("serialdat  : " + obj);
	    		Map<String, String> filemap = obj.getFiles().stream().collect(Collectors.toMap(router::getLocalURL, uf -> uf.getName() ));
	    		String sjson = objectMapper.writeValueAsString(filemap);
			exchange.getOut().setBody(sjson);
	    }
	};
	
	
	from("direct:uploadreply")
		.process(quickUp);
/*
 * 		    		String serialdat = exchange.getIn().getBody(String.class);
		    		System.err.println("serialdat  : " + serialdat);
		    		UploadData obj = objectMapper.readValue(serialdat, UploadData.class);
		    		Map<String, String> filemap = obj.getFiles().stream().collect(Collectors.toMap(router::getLocalURL, uf -> uf.getName() ));
		    		String sjson = objectMapper.writeValueAsString(filemap);
				exchange.getOut().setBody(sjson);

 */
	
	 class FileSplitter implements Expression {
        @Override
        @SuppressWarnings("unchecked")
        public <T> T evaluate(Exchange exchange, Class<T> type) {
//    		log.debug(CloudFSProcessor.dumpExch(exchange));
    		UploadData body = exchange.getIn().getBody(UploadData.class);

//        		exchange.getOut().copyFrom(exchange.getIn());
//        		UploadData body = CloudFSProcessor.getReply(exchange, Verb.transientUpload, UploadData.class);
//            final UploadData body = exchange.getIn().getHeader("locprocdata", UploadData.class);
     		
            return (T) body.getFiles().iterator();
        }
	 }
	 
	 from("direct:b2upload")
		.split(new FileSplitter())
		.process(ex -> { UserFile uf = ex.getIn().getBody(UserFile.class);
			System.err.println("getting uf: " + uf);
			ex.getIn().setBody(null);
			ex.getIn().setHeader("userFile", uf);
		})
		.enrich("bean:authAgent?method=getRemoteAuth", authAgent)
		.setHeader("Authorization", simple(authAgent.getRemoteAuth().getAuthorizationToken()))
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		.setBody(simple(makeUploadReqData()))
		.enrich(getHttp4Proto(authAgent.getRemoteAuth().resolveGetUploadUrl()), (original, resource) -> {
			UploadAuthResponse uploadAuth = null;
			try {
//				UserFile uf = original.getIn().getHeader("userFile", UserFile.class);
//				uf.getFilepath().toFile();
				uploadAuth = objectMapper.readValue(resource.getIn().getBody(String.class), UploadAuthResponse.class);
//				original.getIn().setBody(uploadAuth);
				original.getIn().setHeader("uploadAuth", uploadAuth);
//				original.getIn().setHeader(Exchange.HTTP_URI, getHttp4Proto(uploadAuth.getUploadUrl()));
				
			} catch (IOException e) {
			    if (original.getPattern().isOutCapable()) {
			        original.getOut().setBody(e);
			    }
			}
			System.err.println("resource: " + uploadAuth);
			return original;
		})
		.log("\n\nafter GetUploadUrl:\n${headers}\n\n")
		.process((exchange) -> {
			final Message IN = exchange.getIn();
			UserFile userFile = IN.getHeader("userFile", UserFile.class);
			UploadAuthResponse uploadAuth = IN.getHeader("uploadAuth", UploadAuthResponse.class);
			
            final File file = userFile.getFilepath().toFile();
			
			final HttpPost request = new HttpPost(uploadAuth.getUploadUrl());
			request.setEntity(MultipartEntityBuilder.create().addBinaryBody(file.getName(), file).build());
			
		    request.setHeader("Authorization", uploadAuth.getAuthorizationToken());
			request.setHeader("Content-Type", userFile.getContentType());
			request.setHeader("X-Bz-Content-Sha1", "do_not_verify");
			request.setHeader("X-Bz-File-Name", file.getName());
			request.setHeader("X-Bz-Info-Author", "unknown");
//			request.setHeader("Content-Length", file.length() + "");

			try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

				ByteArrayOutputStream buf = new ByteArrayOutputStream();
				
				HttpResponse response = httpclient.execute(request);
				response.getEntity().writeTo(buf);
				exchange.getOut().setBody(buf.toString("UTF-8"));
	
				Map<String, Object> m = new HashMap<String, Object>();
				Arrays.asList(response.getAllHeaders()).stream()
					.forEach(hdr -> m.put(hdr.getName() , hdr.getValue()));
				
				exchange.getOut().setHeaders(m);
				
			} catch (IOException e) {
				exchange.getOut().setBody(e.getMessage());
				e.printStackTrace();
			}
			
		})
		.log(
				"====================================\n"
				+ "headers: ${headers}" 
			+ "====================================\n"
			+ "body: ${body}" )
		.end()
//		
//		
//		.enrich(getHttp4Proto(authAgent.getRemoteAuth().resolveUploadUrl()), (original, resource) -> {
//			System.err.println("url: " + getHttp4Proto(authAgent.getRemoteAuth().resolveUploadUrl()));
//			System.err.println("original: " + original);
//			if (original == null) return resource;
//			
//			System.err.println("original: " + original);
//
//			UploadAuthResponse body = resource.getIn().getBody(UploadAuthResponse.class);
//			UploadAuthResponse bodyOut = resource.getOut().getBody(UploadAuthResponse.class);
//			
//			
//			System.err.println("resource: " + body.getClass());
//			System.err.println("resource Out: " + bodyOut.getClass());
//			original.getIn().setHeader("uploadAuth", body);
//			return original;
//		})
//		.process((exchange) -> {
//			UserFile userFile = exchange.getIn().getHeader("userFile", UserFile.class);
//			System.err.println("userFile: " + userFile);
//
//    			UploadAuthResponse uploadAuth = exchange.getIn().getHeader("uploadAuth", UploadAuthResponse.class);
//    			System.err.println("uploadAuth: " + uploadAuth);
////    			
////    			String SHA = sha1(userFile.getFilepath().toFile());
////   			
////			exchange.getIn().setBody(userFile.getFilepath().toFile());
////			exchange.getIn().setHeader("Authorization", uploadAuth.getAuthorizationToken());
////			exchange.getIn().setHeader("Content-Type", userFile.getContentType());
//////		    request.setHeader("Content-Length", upfile.length() + "");
////			exchange.getIn().setHeader("X-Bz-Content-Sha1", "do_not_verify");
////			exchange.getIn().setHeader("X-Bz-File-Name", userFile.getFilepath().getFileName().toString());
//////		    request.setHeader("X-Bz-Info-Author", "unknown");
////			exchange.getIn().setHeader(Exchange.HTTP_URI, simple(getHttp4Proto(uploadAuth.getUploadUrl())));
////			
////	log.debug("procFile: " + userFile.getName());
////	log.debug("procFile: " + userFile.getFilepath().toString());
//	
//			}
//		)
////		.toD("${header.CamelHttpUri}")
////		.log("${body.class} \n ${headers}")
		.end();
	 
	 
//	 
//	 
//	 .transform(new ValueBuilder(new ExpressionAdapter(){
//         @Override
//         public Object evaluate(Exchange exchange) {
//             String s = exchange.getIn().getBody(String.class);
//             return s != null ? s.replace('\n', ' ') : null;
//         }
//     }))
	 
//	 .transform(new ValueBuilder(new ExpressionAdapter(){
//         @Override
//         public Object evaluate(Exchange exchange) {
//             String s = exchange.getIn().getBody(String.class);
//             return s != null ? s.replace('\n', ' ') : null;
//         }
//     }))

	 //	 from("direct:douptun")
//		.setHeader("Authorization", simple(authAgent.getRemoteAuth().getAuthorizationToken()))
//		.enrich(getHttp4Proto(authAgent.getRemoteAuth().getApiUrl()) + "/b2api/v1/b2_get_upload_url", (original, exchange) -> {
//			UploadAuthResponse uplauth = exchange.getIn().getBody(UploadAuthResponse.class);
//			original.getIn().setHeader("uploadAuth", uplauth);
//			return original;
//		})		
//		.log("END douptun: ${headers}")
//	 .end();
//		.setBody(simple(makeUploadReqData()))
//		.enrich(authAgent.getRemoteAuth().getApiUrl() + "/b2api/v1/b2_get_upload_url", (original, exchange) -> {
////			exchange.getOut().copyFrom(exchange.getIn());
//			UploadAuthResponse uplauth = exchange.getIn().getBody(UploadAuthResponse.class);
//			original.getIn().setHeader("uploadAuth", uplauth);
////		    if (original.getPattern().isOutCapable()) {
////		    	original.getOut().setHeader("remoteAuth", auth);
////		    }
//		    return original;
//		}).log("${headers}")
//
//		.to("direct:get_up_url", "direct:processFileItem")
//    .end();
//	.process(exchange -> {
//		exchange.getOut().setBody(objectMapper.writeValueAsString(exchange.getIn().getBody()));
//	}).to("file:upload");
//	.process(ex -> { UploadData up = ex.getIn().getHeader("locprocdata", UploadData.class); log.debug(up.getFiles());})
//	.pipeline().to("activemq:b2auth", "direct:get_up_url", "direct:upload");

	
	final Processor processFileItem = new CloudFSProcessor() {
	    @Override
	    public void process(Exchange exchange) throws Exception {
    		log.debug(CloudFSProcessor.dumpExch(exchange));

//	    		final B2Response authBody = (B2Response) getReply(exchange, Verb.authorizeService);
	    		UserFile userFile = exchange.getIn().getBody(UserFile.class);
	    		
	    		String uploadUrl = (String) CloudFSProcessor.getReply(exchange, Verb.uploadUrl);
	    		String uploadToken = (String) CloudFSProcessor.getReply(exchange, Verb.uploadToken);

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
    		log.debug(CloudFSProcessor.dumpExch(exchange));
    		final AuthResponse authBody = exchange.getIn().getHeader("remoteAuth", AuthResponse.class);
//	    	final AuthResponse authBody = (AuthResponse) getReply(exchange, Verb.authorizeService);
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
		.process(getUploadUrl());

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

	private RestDefinition defineRestServer(CloudFSConfiguration serviceConfig2) {
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

		return rest(serviceConfig.getContextUri()).produces("application/json")
				// Upload a File
		        .post("/new/{destDir}").to("direct:rest.upload").outType(Map.class)

		        // Update a File
//		        .put("/mod/{filePath}").to("direct:putFile")

		        // List Buckets
		        .get("/ls").to("direct:rest.list_buckets")
		        
		        // List Directory
//		        .get("/ls/{dirPath}").to("direct:rest.lsdir")

		        // Get file info
////		        .get("/file/{filePath}").to("direct:infoFile")

		        // Delete file
//		        .delete("/{filePath}").to("direct:rest.rm")
//		        .delete("/dir/{dirPath}").to("direct:rest.rmdir")
		        ;
	}

	private String makeUploadReqData() {
		String uplReqData = null;
		try {
			uplReqData = objectMapper.writeValueAsString(ImmutableMap.of("bucketId", "2ab327a44f788e635ef20613"));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return uplReqData;
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
		log.debug(CloudFSProcessor.dumpExch(exchange));
		
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
    	                		uf.setContentType(item.getContentType());
            				uploadData.addFile(uf);
            			}
            		}
            }
        } catch (FileUploadException | IOException e) {
            e.printStackTrace();
        }
        return uploadData;
    }

	private Processor getUploadUrl() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				
	    		log.debug(CloudFSProcessor.dumpExch(exchange));

	    		final AuthResponse authBody = exchange.getIn().getHeader("remoteAuth", AuthResponse.class);
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
					B2Response verbResponse = objectMapper.readValue(responseOut.getBody(String.class), B2Response.class);
					CloudFSProcessor.setReply(exchange, Verb.uploadUrl, verbResponse.getUploadUrl());
					CloudFSProcessor.setReply(exchange, Verb.uploadToken, verbResponse.getAuthorizationToken());
				} catch (IOException e) {
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
	
	public static class MyCustomExpression implements Expression {

        @Override
        @SuppressWarnings("unchecked")
        public <T> T evaluate(Exchange exchange, Class<T> type) {
            final String body = exchange.getIn().getBody(String.class);

            // just split the body by comma
            String[] parts = body.split(",");
            List<String> list = new ArrayList<String>();
            for (String part : parts) {
                list.add(part);
            }

            return (T) list.iterator();
        }
    }
	
	protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:start").setHeader(Exchange.HTTP_METHOD, new MyCustomExpression()).to("http://www.google.com");
                from("direct:reset")
                    .errorHandler(deadLetterChannel("direct:recovery").maximumRedeliveries(1))
                    .setHeader(Exchange.HTTP_METHOD, new MyCustomExpression()).to("http://www.google.com").to("mock:result");
                from("direct:recovery").setHeader(Exchange.HTTP_METHOD, new MyCustomExpression()).to("http://www.google.com").to("mock:recovery");
            }
        };
    }
}
