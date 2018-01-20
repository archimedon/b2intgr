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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
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
import com.rdnsn.b2intgr.processor.UploadException;
import com.rdnsn.b2intgr.processor.UploadProcessor;

/**
 * Base Router
 */
public class ZRouteBuilder extends RouteBuilder {

	private static int cnt = 0;

	private Logger log = LoggerFactory.getLogger(getClass());

	protected final String DIRECTORY_SEP;

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
		this.DIRECTORY_SEP = serviceConfig.getCustomSeparator();
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

//		errorHandler(deadLetterChannel("direct:mailadmin").redeliveryDelay(1000).logStackTrace(false));
		 onException(Exception.class)
     .maximumRedeliveries(3).redeliveryDelay(0);
		// route specific on exception for MyFunctionalException
		// we MUST use .end() to indicate that this sub block is ended

		// onException(org.apache.camel.http.common.HttpOperationFailedException.class)
		// .maximumRedeliveries(serviceConfig.getMaximumRedeliveries()).redeliveryDelay(serviceConfig.getRedeliveryDelay())
		// .to("direct:wrapupload");

		// onException(Exception.class).to("direct:wrapupload")
		// .maximumRedeliveries(4).redeliveryDelay(2000)
		// .asyncDelayedRedelivery()
		;

		// errorHandler(defaultErrorHandler().maximumRedeliveries(3));

		// uploadExceptionHandler();
		// generalExceptionHandler();

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
			.process(saveLocally()).wireTap("direct:b2upload")
			.to("direct:uploadreply")
			.end();

		from("direct:uploadreply")
			.process(new Processor() {
				@Override
				public void process(Exchange exchange) throws IOException {
	
					UploadData obj = exchange.getIn().getBody(UploadData.class);
					Map<String, String> filemap = obj.getFiles().stream().collect(Collectors.toMap((UserFile x) -> {
						return x.getFilepath().toUri().toString().replaceFirst("file://" + serviceConfig.getDocRoot(),
								serviceConfig.getProtocol() + "://" + serviceConfig.getHost());
					}, uf -> uf.getName()));
					String sjson = objectMapper.writeValueAsString(filemap);
					exchange.getOut().setBody(sjson);
				}
		});

		from("direct:b2upload")
			.to("direct:auth")
			// .delay(1000)
			// .asyncDelayed()
			.split(new Expression() {
				@Override
				@SuppressWarnings("unchecked")
				public <T> T evaluate(Exchange exchange, Class<T> type) {
					UploadData body = exchange.getIn().getBody(UploadData.class);
					return (T) body.getFiles().iterator();
				}
			})
			// .delay(1000)
			// .asyncDelayed()
			.process(ex -> {
				UserFile uf = ex.getIn().getBody(UserFile.class);
				ex.getIn().setBody(null);
				ex.getIn().setHeader("userFile", uf);
			})
			.to("direct:wrapupload")
			.end();

		from("direct:wrapupload")
			.log("Calling foo route redelivery count: ${header.CamelRedeliveryCounter}")
			.to("direct:getUploadUrl", "direct:b2send")
			.end();

		from("direct:getUploadUrl")
			.errorHandler(noErrorHandler())

			// .throttle(1)

			// Prepare Exchange for http-post to Backblaze - `upload_file` operation
			.setHeader(Exchange.HTTP_METHOD, constant("POST"))
			//
			.setBody(simple(makeUploadReqData()))
			.enrich(getHttp4Proto(authAgent.getRemoteAuth().resolveGetUploadUrl()), (original, resource) -> {
				try {
					final Message IN = original.getIn();
					final GetUploadUrlResponse uploadAuth = objectMapper.readValue(resource.getIn().getBody(String.class),
							GetUploadUrlResponse.class);

					final UserFile userFile = IN.getHeader("userFile", UserFile.class);

					final File file = userFile.getFilepath().toFile();
					final String remoteFilen = userFile.getName();

					log.debug("File-Name: {}", remoteFilen);
					log.debug("Content-Type: {}", userFile.getContentType());
					log.debug("Content-Length: {}", file.length());

					String authdUploadUrl = getHttp4Proto(uploadAuth.getUploadUrl());
					// String authdUploadUrl = getHttp4Proto("https://www.google.com");
					log.debug("uploadAuth: {}", authdUploadUrl);

					IN.setHeader("uploadAuth", uploadAuth);
					// IN.setHeader("uploadAuthToken", uploadAuth.getAuthorizationToken());
					IN.setHeader("authdUploadUrl", authdUploadUrl);
					// IN.setHeader(Exchange.HTTP_URI,
					// uploadAuth.getUploadUrl().replaceFirst("https:", ""));
					IN.setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
					// IN.setHeader(Exchange.CONTENT_ENCODING, "gzip" );
					IN.setHeader(Exchange.CONTENT_LENGTH, file.length() + "");
					IN.setHeader(Exchange.CONTENT_TYPE, userFile.getContentType());
					IN.setHeader(HttpHeaders.AUTHORIZATION, uploadAuth.getAuthorizationToken());
					// IN.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
					// IN.setHeader(Exchange.HTTP_CHARACTER_ENCODING, "iso-8859-1");
					if (IN.getHeader("cnt") == null && Pattern.matches("^[\\dax].*" , file.getName())) {
						IN.setHeader("cnt", Boolean.TRUE);
						IN.setHeader("X-Bz-Content-Sha1", sha1(file) + 'e');
					} else {
						IN.setHeader("X-Bz-Content-Sha1", sha1(file));
					}
					IN.setHeader("X-Bz-File-Name", remoteFilen);
					IN.setHeader("X-Bz-Info-Author", "unknown");
					IN.setBody(file);

				} catch (IOException e) {
					if (original.getPattern().isOutCapable()) {
						original.getOut().setBody(e);
					}
				}
				return original;
			})
			.log("\n\nafter GetUploadUrl:\n${headers}\n\n")
			.end();

		from("direct:b2send")
		.errorHandler(noErrorHandler())

			.onException(UploadException.class).handled(true).redeliverDelay(3000).to("direct:wrapupload").end()
			.setHeader(Exchange.HTTP_METHOD, HttpMethods.POST)
//			.toD("${header.authdUploadUrl}")
			.toD("${header.authdUploadUrl}" + "?okStatusCodeRange=100-999&throwExceptionOnFailure=true"
					+ "&disableStreamCache=false&transferException=false")
			.process(exchange -> {
				// System.err.println("headers: " + exchange.getIn().getHeaders());
				Integer code = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
				String body = exchange.getIn().getBody(String.class);
				System.err.println("body: " + body);
				if (code != null) {
					if (code != 200) {
						throw new UploadException("bad request " + exchange.getIn().getHeaders());
					}
					System.err.println("code: " + code);
					UploadFileResponse uploadResponse = objectMapper.readValue(body, UploadFileResponse.class);
					
					exchange.getOut().copyFromWithNewBody(exchange.getIn(), uploadResponse);
					
					String downloadUrlBase = exchange.getIn().getHeader("downloadUrlBase", String.class);
					String remoteFilen = exchange.getIn().getHeader("X-Bz-File-Name", String.class);
					
					String downloadUrl = String.format("%s/file/%s/%s", downloadUrlBase,
							serviceConfig.getRemoteStorageConf().getBucketName(), remoteFilen);
					System.err.println(downloadUrl);
					exchange.getOut().setHeader("downloadUrl", downloadUrl);
				} else {
					throw new UploadException("bad request " + exchange.getIn().getHeaders());
				}
			})
			// .to("file://output?fileName=url_map.csv&fileExist=append")
			.end();


		from("direct:listdir")
			.process(new CloudFSProcessor() {

			@Override
			public void process(Exchange exchange) throws Exception {
				final AuthResponse authBody = exchange.getIn().getHeader("remoteAuth", AuthResponse.class);
				// final AuthResponse authBody = (AuthResponse) getReply(exchange,
				// Verb.authorizeService);
				exchange.getOut().copyFrom(exchange.getIn());

				final Message responseOut = getContext().createProducerTemplate()
						.send(getHttp4Proto(authBody.getApiUrl() + "/b2api/v1/b2_list_buckets"), innerExchg -> {
							innerExchg.getIn().setBody(objectMapper.writeValueAsString(
								ImmutableMap.of("accountId", authBody.getAccountId(), "bucketTypes", new ArrayList<String>() {{
									add("allPrivate");
									add("allPublic");
								}})));
							innerExchg.getIn().setHeader("Authorization", authBody.getAuthorizationToken());
						}).getOut();

				int responseCode = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);

				exchange.getOut().setBody(responseOut.getBody(String.class));
				log.debug("inner responseCode " + responseCode);
			}

		});

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
				.post("/{destDir}/upload").bindingMode(RestBindingMode.off).consumes("multipart/form-data")
				.produces("application/json").to("direct:rest.upload")

				// Update a File
				// .put("/mod/{filePath}").to("direct:putFile")

				// List Buckets
				.get("/ls").bindingMode(RestBindingMode.auto).to("direct:rest.list_buckets").produces("application/json")

		// List Directory
		// .get("/ls/{dirPath}").to("direct:rest.lsdir")

		// Get file info
		//// .get("/file/{filePath}").to("direct:infoFile")

		// Delete file
		// .delete("/{filePath}").to("direct:rest.rm")
		// .delete("/dir/{dirPath}").to("direct:rest.rmdir")
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

	// private OnExceptionDefinition httpExceptionHandler() {
	// return onException(HttpOperationFailedException.class).onWhen(exchange -> {
	// exchange.getOut().setHeader("cause", exchange.getException().getMessage());
	// HttpOperationFailedException exe =
	// exchange.getException(HttpOperationFailedException.class);
	// return exe.getStatusCode() > 204;
	// })
	// .log("HTTP exception handled")
	// .handled(true)
	//// .continued(true)
	// .onException(java.lang.NullPointerException.class).redeliveryDelay(2000)
	// .setBody(constant("There will be HttpOperationFailedException blood
	// because..:\n${header.cause}"));
	// }

	private OnExceptionDefinition uploadExceptionHandler() {
		return onException(Exception.class).process(exchange -> {
			if (exchange.isFailed()) {
				System.err.println("exchange.getFromRouteId() " + exchange.getFromRouteId());
				System.err.println("exchange.getException() " + exchange.getException());
				System.err.println("exchange.getIn().getHeaders() " + exchange.getIn().getHeaders());
			}

			exchange.getOut().setHeader("cause", exchange.getException());
			exchange.getOut().setBody(exchange.getIn().getBody());
			log.debug("except: ", exchange.getException()); // HttpOperationFailedException exe =
																											// exchange.getException(HttpOperationFailedException.class);
		}).log("Not handled").handled(false) // .continued(true)
				.setBody(constant("cause: ${header.cause}"));
	}

	private OnExceptionDefinition generalExceptionHandler() {
		return onException(Exception.class).process(exchange -> {
			exchange.getOut().setHeader("cause", exchange.getException());
			exchange.getOut().setBody(exchange.getIn().getBody());
			log.debug("except: ", exchange.getException()); // HttpOperationFailedException exe =
																											// exchange.getException(HttpOperationFailedException.class);
		}).log("Not handled").handled(false) // .continued(true)
				.setBody(constant("cause: ${header.cause}"));
	}

	public static String sha1(final File file) {
		String ans = null;
		try {
			final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");

			InputStream is = new BufferedInputStream(new FileInputStream(file));
			final byte[] buffer = new byte[1024];
			for (int read = 0; (read = is.read(buffer)) != -1;) {
				messageDigest.update(buffer, 0, read);
			}

			// Convert the byte to hex format
			try (Formatter formatter = new Formatter()) {
				for (final byte b : messageDigest.digest()) {
					formatter.format("%02x", b);
				}
				ans = formatter.toString();
			}
		} catch (IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return ans;
	}

	private Processor saveLocally() {

		return new Processor() {

			@Override
			public void process(Exchange exchange) {

				final Message messageIn = exchange.getIn();

				MediaType mediaType = messageIn.getHeader(Exchange.CONTENT_TYPE, MediaType.class);
				InputRepresentation representation = new InputRepresentation(messageIn.getBody(InputStream.class), mediaType);

				try {
					String contextId = null;
					contextId = URLDecoder.decode(messageIn.getHeader("destDir", String.class), "UTF-8").replaceAll(DIRECTORY_SEP,
							"/");

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
								log.info("Received file:\n\tname: {}\n\tsize: {}", item.getName(), item.getSize());
								Files.copy(item.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
								UserFile uf = new UserFile(destination,
										StringUtils.isBlank(item.getFieldName()) ? contextId
												: contextId + File.separatorChar + pathFromUser + File.separatorChar
														+ URLEncoder.encode(destination.getFileName().toString()));
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
