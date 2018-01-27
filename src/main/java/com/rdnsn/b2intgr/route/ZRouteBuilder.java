package com.rdnsn.b2intgr.route;

import static com.rdnsn.b2intgr.RemoteStorageConfiguration.getHttp4Proto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang3.StringUtils;
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
import com.rdnsn.b2intgr.api.AuthResponse;
import com.rdnsn.b2intgr.model.UploadData;
import com.rdnsn.b2intgr.model.UserFile;
import com.rdnsn.b2intgr.processor.AuthAgent;
import com.rdnsn.b2intgr.processor.UploadException;
import com.rdnsn.b2intgr.processor.UploadProcessor;

/**
 * Base Router
 */
public class ZRouteBuilder extends RouteBuilder {

	public static final String HTTP4_PARAMS = 	"?throwExceptionOnFailure=true";
	//	 + "?okStatusCodeRange=100-999&throwExceptionOnFailure=true&disableStreamCache=true";;

	private Logger log = LoggerFactory.getLogger(getClass());

	private final CloudFSConfiguration serviceConfig;
	private final ObjectMapper objectMapper;
	private final AuthAgent authAgent;
	// private final JsonDataFormat jsonUploadAuthFormat;

	public ZRouteBuilder(ObjectMapper objectMapper, CloudFSConfiguration serviceConfig, AuthAgent authAgent)
			throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		super();
		this.objectMapper = objectMapper;
		this.serviceConfig = serviceConfig;
		this.authAgent = authAgent;
		// this.jsonUploadAuthFormat = new JsonDataFormat(JsonLibrary.Jackson);
		// jsonUploadAuthFormat.setUnmarshalType(GetUploadUrlResponse.class);
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

		from("direct:mailadmin")
			.log("${headers}")
		.end();
		
		// Authenticate
		from("direct:auth")
			.enrich("bean:authAgent?method=getRemoteAuth", authAgent)
		.end();

		// Replies -> List of buckets
		from("direct:rest.list_buckets")
			.to("direct:auth")
			.to("direct:listdir")
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
			.process(exchange -> {
				UserFile uf = exchange.getIn().getBody(UserFile.class);
				exchange.getIn().setBody(null);
				exchange.getIn().setHeader(Constants.USER_FILE, uf);
			})
			.to("vm:sub")
		.end();

		from("vm:sub")
			.threads(serviceConfig.getPoolSize(), serviceConfig.getMaxPoolSize())
			.to("direct:b2send")
		.end();
		
		from("direct:b2send").routeId("atomicupload")
			.errorHandler(noErrorHandler())
			.choice()
				.when(header(Exchange.REDELIVERY_COUNTER).isGreaterThanOrEqualTo(0))
					.log("\"Redelivery\": { \"count\" : ${header.CamelRedeliveryCounter} }")
				.otherwise()
					.log("\"Start Upload\"")
			.end()
			.process(new UploadProcessor(serviceConfig, objectMapper))
		.end();

		from("direct:listdir")
			.process(new Processor() {

			@Override
			public void process(Exchange exchange) throws Exception {
				final AuthResponse authBody = exchange.getIn().getHeader(Constants.AUTH_RESPONSE, AuthResponse.class);
				exchange.getOut().copyFrom(exchange.getIn());

				ProducerTemplate producer = getContext().createProducerTemplate();
				
				final Message responseOut = producer.send(
					getHttp4Proto(authBody.getApiUrl() + "/b2api/v1/b2_list_buckets"), innerExchg -> {
						innerExchg.getIn().setBody(objectMapper.writeValueAsString(
								ImmutableMap.of("accountId", authBody.getAccountId(), "bucketTypes", new ArrayList<String>() {
									{
										add("allPrivate");
										add("allPublic");
									}
								})));
						innerExchg.getIn().setHeader(Constants.AUTHORIZATION, authBody.getAuthorizationToken());
					}
				).getOut();
				
				int responseCode = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
				producer.stop();

				exchange.getOut().setBody(responseOut.getBody(String.class));
				log.debug("inner responseCode " + responseCode);
			}

		});

	}

	private Processor replyProxyUrls() {
		return new Processor() {
			
			@Override
			public void process(Exchange exchange) throws IOException {

				UploadData obj = exchange.getIn().getBody(UploadData.class);
				String sjson = objectMapper.writeValueAsString(
					obj.getFiles().stream().collect(Collectors.toMap((UserFile x) -> {
						return x.getFilepath().toUri().toString().replaceFirst("file://" + serviceConfig.getDocRoot(),
								serviceConfig.getProtocol() + "://" + serviceConfig.getHost());
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
				.post("/{destDir}/upload").bindingMode(RestBindingMode.off)
				.consumes("multipart/form-data")
				.produces("application/json")
				.to("direct:rest.upload")

				// Update a File
				// .put("/mod/{filePath}").to("direct:putFile")

				// List Buckets
				.get("/ls")
				.bindingMode(RestBindingMode.auto)
				.produces("application/json")
				.to("direct:rest.list_buckets")

		// List Directory
		// .get("/ls/{dirPath}").to("direct:rest.lsdir")

		// Get file info
		//// .get("/file/{filePath}").to("direct:infoFile")

		// Delete file
		// .delete("/{filePath}").to("direct:rest.rm")
		// .delete("/dir/{dirPath}").to("direct:rest.rmdir")
		;
	}
	
	private class SaveLocally implements Processor {

		@Override
		public void process(Exchange exchange) {

			final Message messageIn = exchange.getIn();

			MediaType mediaType = messageIn.getHeader(Exchange.CONTENT_TYPE, MediaType.class);
			InputRepresentation representation = new InputRepresentation(messageIn.getBody(InputStream.class), mediaType);

			try {
				String contextId = null;
				contextId = URLDecoder.decode(messageIn.getHeader(Constants.TRNSNT_FILE_DESTDIR, String.class), Constants.UTF_8)
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
