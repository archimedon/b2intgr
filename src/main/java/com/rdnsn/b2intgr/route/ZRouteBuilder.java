package com.rdnsn.b2intgr.route;

import static com.rdnsn.b2intgr.api.RemoteStorageConfiguration.getHttp4Proto;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.camel.model.rest.RestDefinition;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
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
import com.rdnsn.b2intgr.CloudFSConfiguration;
import com.rdnsn.b2intgr.api.AuthResponse;
import com.rdnsn.b2intgr.api.GetUploadUrlResponse;
import com.rdnsn.b2intgr.api.UploadFileResponse;
import com.rdnsn.b2intgr.processor.AuthAgent;
import com.rdnsn.b2intgr.processor.CloudFSProcessor;
import com.rdnsn.b2intgr.processor.UploadProcessor;

/**
 * Base Router
 */
public class ZRouteBuilder extends RouteBuilder {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	protected final String DIRECTORY_SEP;

	private final CloudFSConfiguration serviceConfig;
	private final ObjectMapper objectMapper;
	private final AuthAgent authAgent;
//	private final JsonDataFormat jsonUploadAuthFormat;

	public ZRouteBuilder(ObjectMapper objectMapper, CloudFSConfiguration serviceConfig, AuthAgent authAgent) throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		super();
		this.objectMapper = objectMapper;
		this.serviceConfig = serviceConfig;
		this.authAgent = authAgent;
		this.DIRECTORY_SEP = serviceConfig.getCustomSeparator();
//		this.jsonUploadAuthFormat = new JsonDataFormat(JsonLibrary.Jackson);
//		jsonUploadAuthFormat.setUnmarshalType(GetUploadUrlResponse.class);
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
		defineRestServer();

	// Replies -> List of buckets
	from("direct:rest.list_buckets")
		.enrich("bean:authAgent?method=getRemoteAuth", authAgent)
		.to("direct:listdir")
	.end();
	
	// Replies -> HREF to resource
	from("direct:rest.upload")
		.process(saveLocally())
		.wireTap("direct:b2upload")
		.to("direct:uploadreply")
	.end();

	from("direct:uploadreply")
		.process(new Processor() {
		    @Override
		    public void process(Exchange exchange) throws IOException {
		    	
		    	UploadData obj = exchange.getIn().getBody(UploadData.class);
		    		Map<String, String> filemap = obj.getFiles().stream().collect(Collectors.toMap((UserFile x) -> {
		    			return x.getFilepath().toUri().toString().replaceFirst(
		    					"file://"+ serviceConfig.getDocRoot(),
		    					serviceConfig.getProtocol() + "://" + serviceConfig.getHost()
		    				);
		    			}, uf -> uf.getName() ));
		    		String sjson = objectMapper.writeValueAsString(filemap);
				exchange.getOut().setBody(sjson);
		    }
		});
	
	from("direct:b2upload")
	.delay(1000)
	.asyncDelayed()
		.split(new Expression()  {
	        @Override
	        @SuppressWarnings("unchecked")
	        public <T> T evaluate(Exchange exchange, Class<T> type) {
	    		UploadData body = exchange.getIn().getBody(UploadData.class);
	            return (T) body.getFiles().iterator();
	        }
		 })
//		.delay(1000)
//		.asyncDelayed()
		.throttle(1)
		.process(ex -> { UserFile uf = ex.getIn().getBody(UserFile.class);
			ex.getIn().setBody(null);
			ex.getIn().setHeader("userFile", uf);
		})
		.enrich("bean:authAgent?method=getRemoteAuth", authAgent)
		.setHeader("Authorization", simple(authAgent.getRemoteAuth().getAuthorizationToken()))
		
		// Prepare Exchange for http-post to Backblaze - `upload_file` operation
		.setHeader(Exchange.HTTP_METHOD, constant("POST"))
		// 
		.setBody(simple(makeUploadReqData()))
		.enrich(getHttp4Proto(authAgent.getRemoteAuth().resolveGetUploadUrl()), (original, resource) -> {
			GetUploadUrlResponse uploadAuth = null;
			try {
//				UserFile uf = original.getIn().getHeader("userFile", UserFile.class);
//				uf.getFilepath().toFile();
				uploadAuth = objectMapper.readValue(resource.getIn().getBody(String.class), GetUploadUrlResponse.class);
//				original.getIn().setBody(uploadAuth);
				original.getIn().setHeader("uploadAuth", uploadAuth);
//				original.getIn().setHeader(Exchange.HTTP_URI, getHttp4Proto(uploadAuth.getUploadUrl()));
				
			} catch (IOException e) {
			    if (original.getPattern().isOutCapable()) {
			        original.getOut().setBody(e);
			    }
			}
			log.info("Upload URL Response: " + uploadAuth);
			return original;
		})
		.log("\n\nafter GetUploadUrl:\n${headers}\n\n")
		
		.process(new UploadProcessor(serviceConfig, objectMapper))
		.log("body: ${body}" )
//		.to("file://output?fileName=url_map.csv&fileExist=append")
		.end();

	from("direct:listdir")
	.process(new CloudFSProcessor() {
		
	    @Override
	    public void process(Exchange exchange) throws Exception {
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

	}

	private RestDefinition defineRestServer() {
		/**
		 * Configure local Rest server
		 */
		restConfiguration()
		.component("restlet")
		.host(serviceConfig.getHost())
		.port(serviceConfig.getPort())
		.componentProperty("urlDecodeHeaders", "true")
		.skipBindingOnErrorCode(false)
		.dataFormatProperty("prettyPrint", "true")
		.componentProperty("chunked", "true")
		;

		return rest(serviceConfig.getContextUri()).produces("application/json")
				// Upload a File
		        .post("/{destDir}/upload")
		        .bindingMode(RestBindingMode.off)
		        .consumes("multipart/form-data").produces("application/json")
		        .to("direct:rest.upload")

		        // Update a File
//		        .put("/mod/{filePath}").to("direct:putFile")

		        // List Buckets
		        .get("/ls")
				.bindingMode(RestBindingMode.auto)
				.to("direct:rest.list_buckets").produces("application/json")
		        
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

	private Processor saveLocally() {
		
		return new Processor() {

			@Override
			public void process(Exchange exchange) {
				
				final Message messageIn = exchange.getIn();
				
				MediaType mediaType = messageIn.getHeader(Exchange.CONTENT_TYPE, MediaType.class);
				InputRepresentation representation =
						new InputRepresentation(messageIn.getBody(InputStream.class), mediaType);
				
				
				try {
					String contextId = null;
					contextId = URLDecoder.decode(messageIn.getHeader("destDir", String.class), "UTF-8").replaceAll(DIRECTORY_SEP, "/");
				
					String destDirBase = serviceConfig.getDocRoot() + File.separatorChar + contextId;
					
					UploadData uploadData = null;
				
					List<FileItem> items = 
							new RestletFileUpload( new DiskFileItemFactory()).parseRepresentation(representation);
					
					if (! items.isEmpty()) {
						
						uploadData = new UploadData();
						
						for ( FileItem item : items) {
							if (item.isFormField()) {
								uploadData.putFormField(item.getFieldName(), item.getString());
							}
							else {
								String pathFromUser = item.getFieldName();
								
								Path destination = Paths.get(destDirBase + File.separatorChar + pathFromUser , item.getName());
								Files.createDirectories(destination.getParent());
								log.info("Received file:\n\tname: {}\n\tsize: {}", item.getName(), item.getSize());
								Files.copy(item.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
								UserFile uf = new UserFile(destination, StringUtils.isBlank(item.getFieldName())
									? contextId
									: contextId + File.separatorChar + pathFromUser + File.separatorChar + URLEncoder.encode(destination.getFileName().toString()));
								uf.setContentType(item.getContentType());
								item.delete();
								uploadData.addFile(uf);
								exchange.getOut().setBody(uploadData);
							}
						}
					}
				} catch (FileUploadException | IOException e) {
					e.printStackTrace();
				}
			}
			
		};
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
}
