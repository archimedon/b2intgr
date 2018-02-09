package com.rdnsn.b2intgr.route;

import static com.rdnsn.b2intgr.RemoteStorageConfiguration.getHttp4Proto;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.rdnsn.b2intgr.api.*;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.jackson.JacksonDataFormat;

import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
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

	public static final String HTTP4_PARAMS = "?throwExceptionOnFailure=false&okStatusCodeRange=100-99&disableStreamCache=true";

	private Logger log = LoggerFactory.getLogger(getClass());

	private final CloudFSConfiguration serviceConfig;
	private final ObjectMapper objectMapper;
	private final AuthAgent authAgent;
    private String ppath_delete_files = "/b2api/v1/b2_delete_file_version" + "?throwExceptionOnFailure=false&okStatusCodeRange=100-999";
    private String ppath_list_file_vers = "/b2api/v1/b2_list_file_versions" + "?throwExceptionOnFailure=false&okStatusCodeRange=100-999";

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

		 onException(UploadException.class) 
		 .maximumRedeliveries(serviceConfig.getMaximumRedeliveries())
		 .redeliveryDelay(serviceConfig.getRedeliveryDelay());

		defineRestServer();

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

		// Replies -> List File Versions
		from("direct:rest.list_filevers")
			.to("direct:auth", "direct:list_filevers")
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
            })
            .marshal().json(JsonLibrary.Jackson)
            .enrich(
            getHttp4Proto(authAgent.getApiUrl()) + "/b2api/v1/b2_list_file_names" + ZRouteBuilder.HTTP4_PARAMS,
            (Exchange original, Exchange resource) -> {

                log.debug("resource.getIn().getHeaders(): {} ", resource.getIn().getHeaders());
                original.getOut().setBody(
                    coerceClass(resource.getIn(), ListFilesResponse.class)
                        .setMakeDownloadUrl(file -> String.format("%s/file/%s/%s",
                            authAgent.getAuthResponse().getDownloadUrl(),
                            serviceConfig.getRemoteStorageConf().getBucketName(),
                            file.getFileName()))
                );
                return original;
            })
        .end();

        from("direct:list_filevers")
            .process( exchange -> {
                final AuthResponse auth = exchange.getIn().getHeader(Constants.AUTH_RESPONSE, AuthResponse.class);
                ListFilesRequest lfr = exchange.getIn().getBody(ListFilesRequest.class);
                lfr.setBucketId(serviceConfig.getRemoteBucketId());
                exchange.getIn().setHeader(Constants.AUTHORIZATION, auth.getAuthorizationToken());
                exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
                exchange.getIn().setBody(lfr);
            })
            .marshal().json(JsonLibrary.Jackson)
            .enrich(
            getHttp4Proto(authAgent.getApiUrl()) + ppath_list_file_vers,
            (Exchange original, Exchange resource) -> {
                log.debug("resource.getIn().getHeaders(): {} ", resource.getIn().getHeaders());
                original.getOut().setBody(
                        coerceClass(resource.getIn(), ListFilesResponse.class)
                                .setMakeDownloadUrl(file -> String.format("%s/file/%s/%s",
                                        authAgent.getAuthResponse().getDownloadUrl(),
                                        serviceConfig.getRemoteStorageConf().getBucketName(),
                                        file.getFileName()))
                );
                return original;
            })
        .end();

        from("direct:rm_files")
            .split(new Expression () {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T evaluate(Exchange exchange, Class<T> type) {
                    Message IN = exchange.getIn();
                    DeleteFilesRequest body = exchange.getIn().getBody(DeleteFilesRequest.class);
                    IN.setHeader("DeleteFilesRequest", body);
                    IN.setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
                    IN.removeHeader("authResponse");
                    return (T) body.getFiles().iterator();
                }
            })
            .to("vm:delete")
        .end();

        from("vm:delete")
            .marshal().json(JsonLibrary.Jackson)

//            .threads(serviceConfig.getPoolSize(), serviceConfig.getMaxPoolSize())
            .enrich(
                getHttp4Proto(authAgent.getApiUrl()) + ppath_delete_files,
                (Exchange original, Exchange resource) -> {

                    DeleteFileResponse delFileResponse = coerceClass(original.getIn(), DeleteFileResponse.class);

//                    log.error(" original.getOut().setBody(: " +  original.getOut().getBody());
                    log.error(" original.getIn().setBody(: " +  delFileResponse);

                    DeleteFilesResponse respList = original.getOut().getBody(DeleteFilesResponse.class);

                    if ( respList == null ) {
//                        DeleteFilesRequest delRequest = (DeleteFilesRequest) original.getIn().removeHeader("DeleteFilesRequest");
//                        delResp = new DeleteFilesResponse(delRequest.getFiles());
                                respList = new DeleteFilesResponse();
                        log.error("Created New DeleteResponse");
                        original.getOut().setBody(respList);

                    }


                    final Integer code = resource.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
//                    FileResponse delFile = null;
                    if (HttpStatus.SC_OK != code) {
                        ErrorObject error = coerceClass(resource.getIn(), ErrorObject.class);
                        delFileResponse.setError(error);
                        log.debug("error: " + error);

//                        fes.setCode(delFile.getCode())
//                            .setMessage(delFile.getMessage())
//                            .setStatus(delFile.getStatus());
                    }
                    log.debug("delFileResponse: " + delFileResponse);
                    respList.updateFile(delFileResponse);
                    original.getIn().setBody(respList);

                    log.debug("respList: " + respList);
                    return original;
                }
            )
        .end();
	}

    private String extractId(ReadsError delFile) {
	    String id = null;
        Pattern pattern = Pattern.compile("([^\\s]+)$");
        Matcher matcher = pattern.matcher(delFile.getMessage());

        log.debug("delFile.getMessage(): " + delFile.getMessage());

        if (matcher.find()) {
            id = matcher.group(1);
        }

        log.debug("delFile id: " + id);

        return id;
    }

    //
    private <T> T coerceClass(Message rsrcIn, Class<T> type) {
        T obj = null;
        try {
            String string =  rsrcIn.getBody(String.class);
            log.debug("Got string: {}", string);
            obj = objectMapper.readValue(string, type);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return obj;
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

		return rest(serviceConfig.getContextUri())
                .produces("application/json")

            // Upload a File
            .post("/{destDir}/upload")
                .description("Upload (and revise) files. Uploading to the same name and path rsults in creating a new <i>version</i> of the file.")
                .bindingMode(RestBindingMode.off)
                .consumes("multipart/form-data")
                .produces("application/json")
                .to("direct:rest.upload")

            // List Buckets
            .get("/list")
                .description("List buckets")
                .bindingMode(RestBindingMode.off)
                .produces("application/json")
                .to("direct:rest.list_buckets")


            // List Files
            .post("/ls").type(ListFilesRequest.class).outType(ListFilesResponse.class)
                .description("List files")
				.bindingMode(RestBindingMode.auto)
				.produces("application/json")
				.to("direct:rest.list_files")

            // List File Versions
            .post("/lsvers").type(ListFilesRequest.class).outType(ListFilesResponse.class)
                .description("List file and versions thereof")
				.bindingMode(RestBindingMode.auto)
				.produces("application/json")
				.to("direct:rest.list_filevers")

            .delete("/rm").type(DeleteFilesRequest.class).outType(DeleteFilesResponse.class)
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

							UserFile uf = new UserFile(destination, StringUtils.isBlank(item.getFieldName())
                                ? contextId
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
