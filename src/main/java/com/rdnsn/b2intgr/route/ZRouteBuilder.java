package com.rdnsn.b2intgr.route;

import static com.rdnsn.b2intgr.RemoteStorageConfiguration.getHttp4Proto;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.rdnsn.b2intgr.api.*;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.jackson.JacksonDataFormat;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.restlet.data.MediaType;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.InputRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.rdnsn.b2intgr.CloudFSConfiguration;
import com.rdnsn.b2intgr.Constants;
import com.rdnsn.b2intgr.model.UploadData;
import com.rdnsn.b2intgr.model.UserFile;
import com.rdnsn.b2intgr.processor.AuthAgent;
import com.rdnsn.b2intgr.processor.UploadException;
import com.rdnsn.b2intgr.processor.UploadProcessor;

/**
 * Base Router
 */
public class ZRouteBuilder extends RouteBuilder {

	public static final String HTTP4_PARAMS = "";
	//	 + "?throwExceptionOnFailure=false&okStatusCodeRange=100-99&disableStreamCache=true";

	private Logger log = LoggerFactory.getLogger(getClass());

	private final CloudFSConfiguration serviceConfig;
	private final ObjectMapper objectMapper;
	private final AuthAgent authAgent;

	public ZRouteBuilder(ObjectMapper objectMapper, CloudFSConfiguration serviceConfig, AuthAgent authAgent)
			throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		super();
		this.objectMapper = objectMapper;
		this.serviceConfig = serviceConfig;
		this.authAgent = authAgent;
        JacksonDataFormat UploadUrlResponseFormat = new JacksonDataFormat(GetUploadUrlResponse.class);
        JacksonDataFormat UploadFileResponseFormat = new JacksonDataFormat(GetUploadUrlResponse.class);
        JacksonDataFormat AuthResponseFormat = new JacksonDataFormat(AuthResponse.class);

		// enable Jackson json type converter
		getContext().getGlobalOptions().put("CamelJacksonEnableTypeConverter", "true");
		// allow Jackson json to convert to pojo types also
		getContext().getGlobalOptions().put("CamelJacksonTypeConverterToPojo", "true");
	}
	
	/**
	 * Routes ...
	 */
	public void configure() {

//		 onException(HttpOperationFailedException.class)
//		 .maximumRedeliveries(serviceConfig.getMaximumRedeliveries())
//		 .redeliveryDelay(serviceConfig.getRedeliveryDelay())
//		 .end();
		// errorHandler(defaultErrorHandler().maximumRedeliveries(3));
		
//		onException(HttpOperationFailedException.class).onWhen(exchange -> {
//			exchange.getOut().setHeader("cause", exchange.getException().getMessage());
//			HttpOperationFailedException exe = exchange.getException(HttpOperationFailedException.class);
//			return exe.getStatusCode() > 204;
//		}).redeliveryDelay(2000).handled(true)
//		.log("HTTP exception handled").onException(java.lang.NullPointerException.class);
		

		 onException(UploadException.class) 
		 .maximumRedeliveries(serviceConfig.getMaximumRedeliveries())
		 .redeliveryDelay(serviceConfig.getRedeliveryDelay());

		defineRestServer();

//		from("direct:mailadmin")
//			.log("${headers}")
//		.end();
		
		// Authenticate
		from("direct:auth")
			.enrich("bean:authAgent?method=getAuthResponse", authAgent)
		.end();

		// Replies -> List of buckets
		from("direct:rest.list_buckets")
			.to("direct:auth", "direct:list_buckets")
		.end();

		// Replies -> List Files in base bucket
		from("direct:rest.list_files")
			.to("direct:auth", "direct:list_files")
		.end();

		// Replies -> Delete Files
		from("direct:rest.rm_files")
			.to("direct:auth", "direct:rm_files")
		.end();

		// Replies -> HREF to resource
		from("direct:rest.upload")
			.process(new SaveLocally())
				// Send to b2
				.wireTap("direct:b2upload")
			.end()
			.process(replyProxyUrls())
		.end();

		from("direct:b2upload").routeId("upload_facade")
			.to("direct:auth")
			.split( new ListSplitExpression())
//			.to("vm:sub?concurrentConsumers=5")
			.to("vm:sub")
		.end();

		from("vm:sub")
            .threads(serviceConfig.getPoolSize(), serviceConfig.getMaxPoolSize())
			.to("direct:b2send")
		.end();
		
		from("direct:b2send").routeId("atomic_upload")
			.errorHandler(noErrorHandler())
			.process(new UploadProcessor(serviceConfig, objectMapper))
		.end();

		from("direct:list_buckets")
            .process(createPost())
            .enrich(
                getHttp4Proto(authAgent.getApiUrl() + "/b2api/v1/b2_list_buckets" + ZRouteBuilder.HTTP4_PARAMS),
                (Exchange original, Exchange resource) -> {
                    String json = resource.getIn().getBody(String.class);
                    original.getIn().copyFromWithNewBody(resource.getIn(), json);
                    System.err.println(resource.getIn().getHeaders());
                    return original;
                }
            )
			.end();

        from("direct:list_files")
            .process( exchange -> {
                final AuthResponse auth = exchange.getIn().getHeader(Constants.AUTH_RESPONSE, AuthResponse.class);
                ListFilesRequest lfr = exchange.getIn().getBody(ListFilesRequest.class);
                lfr.setBucketId(serviceConfig.getRemoteBucketId());
                exchange.getIn().setHeader(Constants.AUTHORIZATION, auth.getAuthorizationToken());
                exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
                exchange.getIn().setBody(lfr);
            }).marshal().json(JsonLibrary.Jackson)
//                .process( exchange -> { System.err.println("in: " + exchange.getIn().getBody(String.class));})
            .enrich(
                getHttp4Proto(authAgent.getApiUrl()) + "/b2api/v1/b2_list_file_names" + ZRouteBuilder.HTTP4_PARAMS,
                (Exchange original, Exchange resource) -> {
                    if (resource == null) return original;
                    System.err.println(resource.getIn().getHeaders());
                    try {
                        B2FileListResponse re = objectMapper.readValue(resource.getIn().getBody(String.class), B2FileListResponse.class);
                        original.getOut().setBody(re);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return original;
                }
        ) //.marshal().json(JsonLibrary.Jackson)
        .end();

        from("direct:rm_files").marshal().json(JsonLibrary.Jackson, DeleteFilesRequest.class) //.inputType(DeleteFilesRequest.class)
//            .outputType(DeleteFile.class)
            .split( new Expression () {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T evaluate(Exchange exchange, Class<T> type) {
                    Message IN = exchange.getIn();

//                    DeleteFilesRequest body = exchange.getIn().getBody(DeleteFilesRequest.class);
                    DeleteFilesRequest body = null;
                    try {
                        body = objectMapper.readValue(IN.getBody(String.class), DeleteFilesRequest.class);
                        log.debug("body.getFiles(): {} ", body.getFiles());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    IN.setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
                    IN.removeHeader("authResponse");
                    return (T) body.getFiles().iterator();
                }
            })
                .to("vm:delete")
                .end();

        from("vm:delete").marshal().json(JsonLibrary.Jackson)
//        .threads(2, 2)
//                .to(getHttp4Proto(authAgent.getApiUrl()) + "/b2api/v1/b2_delete_file_version" + "?throwExceptionOnFailure=true")
                .process(exchange -> {

                    String fileDesc = exchange.getIn().getBody(String.class);
                    log.debug("fileDesc {} " + fileDesc);

                    String json = "Failed";

//                    try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
//                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
//                        HttpPost httpPost = new HttpPost(authAgent.getApiUrl() + "/b2api/v1/b2_delete_file_version");
//                        httpPost.setHeader(Constants.AUTHORIZATION, exchange.getIn().getHeader(Constants.AUTHORIZATION, String.class));
//                        httpPost.setHeader("Content-type", "application/json");
//                        httpPost.setEntity(new StringEntity(fileDesc));
//
//                        CloseableHttpResponse response = httpclient.execute(httpPost);
//
//                        response.getEntity().writeTo(buf);
//                        json = buf.toString(Constants.UTF_8);
//
//                        log.info("StatusLine: {}", response.getStatusLine());
//
//                        log.info("json :{}", json);
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                    exchange.getIn().setBody(json);
                    exchange.getOut().setBody(json);
//                    if (HttpStatus.SC_OK == code) {
//                        }

//                    String json = exchange.getIn().getBody(String.class);
//                    analyze(exchange);
//                    exchange.getOut().setBody(json);
                })

//                .enrich(
//                    getHttp4Proto(authAgent.getApiUrl()) + "/b2api/v1/b2_delete_file_version" +
//                        "?throwExceptionOnFailure=false&okStatusCodeRange=100-999",
////                        "?throwExceptionOnFailure=false&okStatusCodeRange=100-999&disableStreamCache=true"
//
//                        (Exchange original, Exchange resource) -> {
////                            if (resource == null) return original;
//
//                            String json = resource.getIn().getBody(String.class);
//
//                            log.debug("json: {}", json);
//
//                            Integer responseCode = resource.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
//                            log.debug("responseCode: {}", responseCode);
////                            original.getOut().copyFromWithNewBody(resource.getIn(), json);
////                            original.getIn().copyFromWithNewBody(resource.getIn(), json);
//
//                            original.getOut().setBody(json);
//                            original.getIn().setBody(json);
//
//                            original.setException(null);
//                            resource.setException(null);
//
////                            HttpOperationFailedException excep = resource.getException(HttpOperationFailedException.class);
////                            if (excep != null) {
////                            }
////
//
//                           return original;
//                        } )

//                .onException(HttpOperationFailedException.class).process(exchange -> {
//
//                            String obod = exchange.getOut().getBody(String.class);
//                            String ibod = exchange.getIn().getBody(String.class);
//
//            log.debug("obod: {}", obod);
//            log.debug("ibod: {}", ibod);
//
//            exchange.getOut().setHeader("cause", exchange.getException().getMessage());
//            exchange.getOut().setBody("oyoyo");
////            HttpOperationFailedException exe = exchange.getException(HttpOperationFailedException.class);
////            return exe.getStatusCode() > 200;
//        }).handled(true)
//                .process(ex -> {
//                    String json = ex.getIn().getBody(String.class);
//                    System.err.println("the json: " + json);
//                    ex.getOut().setBody("yojo");
//                    ex.getIn().setBody("in - yojo");
//                })
                .log("headers: ${headers}")
//                .log("${body}")
                .end();



	}

    private void analyze(final Exchange exchange) {
        log.debug("exchange.getException: {}", exchange.getException());
        log.debug("exchange.getProperties: {}", exchange.getProperties());
        log.debug("exchange.getPattern: {}", exchange.getPattern());
        log.debug("exchange.getIn().getHeaders: {}", exchange.getIn().getHeaders());
        log.debug("exchange.getIn().getBody: {}", exchange.getIn().getBody(String.class));
        log.debug("exchange.getOut().getHeaders: {}", exchange.getOut().getHeaders());
        log.debug("exchange.getOut().getBody: {}", exchange.getOut().getBody());
    }

    private Processor createPost() {

        return new Processor() {

            @Override
            public void process(Exchange exchange) throws IOException {

                final AuthResponse auth = exchange.getIn().getHeader(Constants.AUTH_RESPONSE, AuthResponse.class);
                exchange.getOut().setHeader(Constants.AUTHORIZATION, auth.getAuthorizationToken());
                exchange.getOut().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
                exchange.getOut().setBody(objectMapper.writeValueAsString(ImmutableMap.of(
                "accountId", auth.getAccountId(),
                "bucketTypes", ImmutableList.of("allPrivate", "allPublic")
                )));
            }
        };
    }

    private Processor replyProxyUrls() {
		return new Processor() {

			@Override
			public void process(Exchange exchange) throws IOException {

				UploadData obj = exchange.getIn().getBody(UploadData.class);
				String sjson = objectMapper.writeValueAsString(
					obj.getFiles().stream().collect(Collectors.toMap((UserFile x) -> {
						return x.getFilepath().toUri().toString().replaceFirst(
                        "file://" + serviceConfig.getDocRoot(),
                        serviceConfig.getProtocol() + "://" + serviceConfig.getHost()
                        );
					}, uf -> uf.getName()))
				);
				exchange.getOut().setBody(sjson);
			}
		};
	}

	private RestDefinition defineRestServer() {
		/**
		 * Configure local Rest server
		 */
		restConfiguration().component("restlet").host(serviceConfig.getHost()).port(serviceConfig.getPort())
				.componentProperty("urlDecodeHeaders", "true").skipBindingOnErrorCode(false)
				.dataFormatProperty("prettyPrint", "true").componentProperty("chunked", "true");

		return rest(serviceConfig.getContextUri()).produces("application/json")


            // Upload a File
            .post("/{destDir}/upload")
                .bindingMode(RestBindingMode.off)
                .consumes("multipart/form-data")
                .produces("application/json")
                .to("direct:rest.upload")

            // List Buckets
            .get("/list")
                .bindingMode(RestBindingMode.off)
                .produces("application/json")
                .to("direct:rest.list_buckets")


            // List Files
            .post("/ls").type(ListFilesRequest.class)
				.bindingMode(RestBindingMode.auto)
				.produces("application/json")
				.to("direct:rest.list_files")

            .delete("/rm") //.type(DeleteFilesRequest.class)
				.bindingMode(RestBindingMode.auto)
//                .produces("text/plain")
				.produces("application/json")
				.to("direct:rest.rm_files")

    ;
        // Update a File objectMapper.readValue(
        // .put("/mod/{filePath}").to("direct:putFile")

		// Get file info
		//// .get("/file/{filePath}").to("direct:infoFile")

	}
	
	private class SaveLocally implements Processor {

		@Override
		public void process(Exchange exchange) {

			final Message messageIn = exchange.getIn();

			MediaType mediaType = messageIn.getHeader(Exchange.CONTENT_TYPE, MediaType.class);
			InputRepresentation representation = new InputRepresentation(messageIn.getBody(InputStream.class), mediaType);

			try {
				String contextId = URLDecoder.decode(messageIn.getHeader(Constants.TRNSNT_FILE_DESTDIR, String.class), Constants.UTF_8)
						.replaceAll(serviceConfig.getCustomSeparator(), "/");

				String destDirBase = serviceConfig.getDocRoot() + File.separatorChar + contextId;

				UploadData uploadData = null;

				List<FileItem> items = new RestletFileUpload(new DiskFileItemFactory()).parseRepresentation(representation);

				if (!items.isEmpty()) {

					uploadData = new UploadData();

					for (FileItem item : items) {
						if (item.isFormField()) {
							uploadData.putFormField(item.getFieldName(), item.getString());
						} else {
							String pathFromUser = item.getFieldName();

							Path destination = Paths.get(destDirBase + File.separatorChar + pathFromUser, item.getName());
							Files.createDirectories(destination.getParent());
							log.info("\"Received file\":{ \"name\": \"{}\", \"Size\": \"{}\"}", item.getName(), item.getSize());
							Files.copy(item.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
							UserFile uf = new UserFile(destination,
									StringUtils.isBlank(item.getFieldName()) ? contextId
											: contextId + File.separatorChar + pathFromUser + File.separatorChar
													+ URLEncoder.encode(destination.getFileName().toString(), Constants.UTF_8));
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
	}
	
	private class ListSplitExpression implements Expression {
		@Override
		@SuppressWarnings("unchecked")
		public <T> T evaluate(Exchange exchange, Class<T> type) {
			UploadData body = exchange.getIn().getBody(UploadData.class);
			return (T) body.getFiles().iterator();
		}
	}		

}
