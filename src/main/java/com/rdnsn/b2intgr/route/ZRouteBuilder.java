package com.rdnsn.b2intgr.route;


import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.rdnsn.b2intgr.MainApp;
import com.rdnsn.b2intgr.dao.ProxyUrlDAO;
import com.rdnsn.b2intgr.model.ProxyUrl;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;

import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestDefinition;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.http.HttpStatus;
import org.restlet.data.MediaType;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.InputRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.rdnsn.b2intgr.api.*;
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

    public static final String HTTP4_PARAMS = "?throwExceptionOnFailure=false&okStatusCodeRange=100-999";
//            "&disableStreamCache=true";

    private Logger log = LoggerFactory.getLogger(getClass());

    private final CloudFSConfiguration serviceConfig;
    private final ObjectMapper objectMapper;

    private final AuthAgent authAgent;
    private final String ppath_delete_files = "/b2api/v1/b2_delete_file_version" + HTTP4_PARAMS;
    private final String ppath_list_file_vers = "/b2api/v1/b2_list_file_versions" + HTTP4_PARAMS;
    private final String ppath_list_buckets = "/b2api/v1/b2_list_buckets" + HTTP4_PARAMS;
    private final String ppath_list_file_names = "/b2api/v1/b2_list_file_names";
    private final String downloadServicePath = "/file";
    private final URI mailURL;
    private String bchmodService = "/chbktacc";
    private String grantDwnldService = "/grantDwnld";

    // // TODO: 2/13/18 url-encode the downloadURL
    public ZRouteBuilder(ObjectMapper objectMapper, CloudFSConfiguration serviceConfig, AuthAgent authAgent) throws URISyntaxException {
        super();
        this.objectMapper = objectMapper;
        this.serviceConfig = serviceConfig;
        this.authAgent = authAgent;
        this.mailURL = new URI(
            "smtps",
            null,
            serviceConfig.getMailConfig().getHost(),
            serviceConfig.getMailConfig().getPort(),
            "",
            String.format("username=%s&password=%s&to=%s",
                serviceConfig.getMailConfig().getUsername(),
                serviceConfig.getMailConfig().getPassword(),
                serviceConfig.getMailConfig().getRecipients()
            ),
            null
        );

        // enable Jackson json type converter
        getContext().getGlobalOptions().put("CamelJacksonEnableTypeConverter", "true");
        // allow Jackson json to convert to pojo types also
        getContext().getGlobalOptions().put("CamelJacksonTypeConverterToPojo", "true");
    }

    /**
     * Routes ...
     */
    public void configure() {

        final AggregationStrategy fileListResultAggregator = (Exchange oldExchange, Exchange newExchange) -> {
            Message newIn = newExchange.getIn();

            FileResponse newBody = newIn.getBody(FileResponse.class);

            if (oldExchange == null) {

                newIn.setBody(new DeleteFilesResponse()
                    .updateFile(newBody)
                    .setMakeDownloadUrl(
                        file -> buildURLString(
                            authAgent.getAuthResponse().getDownloadUrl(),
                            "file",
                                serviceConfig.getRemoteStorageConf().getBucketName(),
                                file.getFileName()
                        )
                    )
                );
                return newExchange;
            }
            else {
                oldExchange.getIn()
                    .getBody(DeleteFilesResponse.class)
                    .updateFile(newBody);
                return oldExchange;
            }
        };

        final AggregationStrategy fileResponseAggregator = (Exchange original, Exchange resource) -> {

            // TODO: 3/6/18 Get rid of BiFunc MakeDownloadURL()
            original.getOut().setBody(coerceClass(resource.getIn(), ListFilesResponse.class).setMakeDownloadUrl(file ->
               buildURLString(authAgent.getAuthResponse().getDownloadUrl(), "file", serviceConfig.getRemoteStorageConf().getBucketName(), file.getFileName())
            ));
            return original;
        };

        final Processor createPostFileList = (exchange) -> {
            final AuthResponse auth = exchange.getIn().getHeader(Constants.AUTH_RESPONSE, AuthResponse.class);

//            String bucketId = exchange.getIn().getHeader("bucketId", String.class);
//            String bucketId = URLDecoder.decode(exchange.getIn().getHeader("bucketId", String.class), Constants.UTF_8);

//            log.debug(" bucketId: {}", bucketId);
            ListFilesRequest lfr = exchange.getIn().getBody(ListFilesRequest.class);
//            lfr.setBucketId(bucketId);
            exchange.getOut().setHeader(Constants.AUTHORIZATION, auth.getAuthorizationToken());
            exchange.getOut().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
            exchange.getOut().setBody(lfr);

        };

        final Processor createPost = (Exchange exchange) -> {

            final AuthResponse auth = exchange.getIn().getHeader(Constants.AUTH_RESPONSE, AuthResponse.class);
            exchange.getOut().setHeader(Constants.AUTHORIZATION, auth.getAuthorizationToken());
            exchange.getOut().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
            exchange.getOut().setBody(objectMapper.writeValueAsString(ImmutableMap.of(
                    "accountId", auth.getAccountId(),
                    "bucketTypes", ImmutableList.of("allPrivate", "allPublic")
            )));
        };

        onException(UploadException.class)
            .asyncDelayedRedelivery()
            .maximumRedeliveries(serviceConfig.getMaximumRedeliveries())
            .redeliveryDelay(serviceConfig.getRedeliveryDelay())
                .backOffMultiplier(3)
            .to("direct:mail")
            .handled(true);


//        onException(org.apache.camel.http.common.HttpOperationFailedException.class).process(exchange -> {
////            ErrorObject err = exchange.getOut().getBody(ErrorObject.class);
////            exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, err.getStatus());
//            exchange.getOut().setBody("{ err: \"v\"}");
//        }).handled(true);

        onException(B2BadRequestException.class).process(exchange -> {
            ErrorObject err = exchange.getOut().getBody(ErrorObject.class);
            exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, err.getStatus());
            exchange.getOut().setBody(err);
        }).handled(true);

        defineRestServer();

        from("direct:mail")
            .setHeader("subject", constant("BackBlaze Upload Failed"))
            .to(mailURL.toString());

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
        from("direct:rest.multipart")
                .process(new SaveLocally())
                // Send to b2
                .wireTap("direct:b2upload")
                .end()

                .process(new ReplyProxyUrls())
                .end();


        from("direct:b2upload").routeId("upload_facade")
                .to("direct:auth")
                .split(new ListSplitExpression())
                .to("vm:sub")
                .end();

        from("vm:sub")
                .threads(serviceConfig.getPoolSize(), serviceConfig.getMaxPoolSize())
                .to("direct:b2send")
                .end();

        from("direct:b2send").routeId("atomic_upload")
                .errorHandler(noErrorHandler())
                .process(new UploadProcessor(serviceConfig, objectMapper))
//                .delay(500)
                .process(new PersistMapping())
                .end();

        from("direct:list_buckets")
                .process(createPost)
                .enrich(
                    getHttp4Proto(authAgent.getApiUrl()) + ppath_list_buckets, (Exchange original, Exchange resource) -> {
                        original.getIn().copyFromWithNewBody(
                                resource.getIn(),
                                resource.getIn().getBody(String.class)
                        );
                        return original;
                    })
                .end();

        from("direct:list_files")
                .process((exchange) -> {
                    final AuthResponse auth = exchange.getIn().getHeader(Constants.AUTH_RESPONSE, AuthResponse.class);

//            String bucketId = exchange.getIn().getHeader("bucketId", String.class);
//            String bucketId = URLDecoder.decode(exchange.getIn().getHeader("bucketId", String.class), Constants.UTF_8);

//            log.debug(" bucketId: {}", bucketId);
                    ListFilesRequest lfr = exchange.getIn().getBody(ListFilesRequest.class);
//            lfr.setBucketId(bucketId);
                    exchange.getOut().setHeader(Constants.AUTHORIZATION, auth.getAuthorizationToken());
                    exchange.getOut().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
                    exchange.getOut().setBody(lfr.toString());

                })
//                .marshal().json(JsonLibrary.Jackson)
                .enrich(
                    getHttp4Proto(authAgent.getApiUrl()) + ppath_list_file_names, fileResponseAggregator)
                .end();

        from("direct:list_filevers")
                .process(createPostFileList)
                .marshal().json(JsonLibrary.Jackson)
                .enrich(getHttp4Proto(authAgent.getApiUrl()) + ppath_list_file_vers, fileResponseAggregator)
                .end();

        from("direct:rest.file_proxy")
                .to("direct:show");

        from("direct:rest.dir_auth")
                .to("direct:auth", "direct:dir_auth");

        from("direct:rest.bchmod")
                .to("direct:auth", "direct:btouch_remote");

        from("direct:btouch_remote")
                .process(exchange -> {
                    TouchBucketRequest bt = exchange.getIn().getBody(TouchBucketRequest.class);
                    bt.setAccountId(serviceConfig.getRemoteAccountId());
//

//                    final AuthResponse auth = exchange.getIn().getHeader(Constants.AUTH_RESPONSE, AuthResponse.class);
                    exchange.getOut().setHeader(Constants.AUTHORIZATION, exchange.getIn().getHeader(Constants.AUTHORIZATION, String.class));
//                    exchange.getOut().copyFromWithNewBody(
//                        exchange.getIn(),
//                        exchange.getIn()
//                            .getBody(TouchBucketRequest.class)
//                            .setAccountId(serviceConfig.getRemoteAccountId())
//                            .toString()
//                    );

                    exchange.getOut().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);


//                    exchange.getOut().copyFromWithNewBody(exchange.getIn(), bt);

//                    String bucketId = URLDecoder.decode(exchange.getIn().getHeader("bucketId", String.class), Constants.UTF_8)
//                            .replaceAll(serviceConfig.getCustomSeparator(), "/");
//
//                    log.debug("got bt {}", bt);
                    exchange.getOut().setBody(bt.toString());
                })
                .enrich(getHttp4Proto(authAgent.getApiUrl()) + "/b2api/v1/b2_update_bucket", (Exchange original, Exchange resource) -> {
                        original.getIn().copyFromWithNewBody(
                                resource.getIn(),
                                resource.getIn().getBody(String.class)
                        );
                        return original;
                    })
            .end();

        from("direct:dir_auth")
            .process((Exchange exchange) -> {


                DirectoryAccessRequest bt = exchange.getIn().getBody(DirectoryAccessRequest.class);
                String authToken = exchange.getIn().getHeader(Constants.AUTHORIZATION, String.class);

//                IN.removeHeaders("*");

                exchange.getOut().setHeader(Constants.AUTHORIZATION, authToken);
//                exchange.getOut().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
//                exchange.getOut().setHeader("Content-Type", "application/x-www-form-urlencoded");
//                exchange.getOut().setHeader("Content-Length", Integer.toString(bt.toString().getBytes().length));


                exchange.getOut().setBody(bt);


                log.debug("body: {}", bt);
                log.debug("getHeaders: {}", exchange.getOut().getHeaders().entrySet());
            })
                .marshal().json(JsonLibrary.Jackson)
                .enrich(getHttp4Proto(authAgent.getApiUrl()) + "/b2api/v1/b2_get_download_authorization?throwExceptionOnFailure=false&okStatusCodeRange=100",(Exchange original, Exchange resource) -> {
                original.getIn().copyFromWithNewBody(
                        resource.getIn(),
                        resource.getIn().getBody(String.class));
//                final Integer code = resource.getOut().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
//
//                log.info("HTTP_RESPONSE_CODE:{ '{}'}", code);
//
//                if (code != null && HttpStatus.SC_OK == code) {
//                    log.info("Completed: '{}'", "yeah");
//                    String respBody = resource.getOut().getBody(String.class);
//                    log.debug("respBody: {} ", respBody);
//                    original.getOut().setBody(respBody);
//                }
//                else {
////                    ErrorObject errorObject =  coerceClass(resource.getOut(), ErrorObject.class);
////                    log.debug("errorObject: {} ", errorObject);
////                    original.getOut().setBody(errorObject);
//                    String respBody = resource.getOut().getBody(String.class);
//                    log.debug("else respBody: {} ", respBody);
//
//                }
                return original;
            })
        .end();


        from("direct:show")
            .process((Exchange exchange) -> {

            HttpRequest request
                    = exchange.getIn().getHeader("CamelRestletRequest", HttpRequest.class);

            // /[serviceContextUri][servicePath][partialPath]
            // [/cloudfs/api/v1] [/file] [/top/site/images/v2/Flag_of_Jamaica.png]
            String uri = request.getHttpCall().getRequestUri();
            String ctx = serviceConfig.getContextUri() + downloadServicePath + "/";

            String ppath = uri.substring(uri.indexOf(ctx) + ctx.length());
            File file = new File(serviceConfig.getDocRoot() + File.separatorChar + ppath);

            if (! file.exists()) {
                log.info("ppath: {} ", ppath);

                try (ProxyUrlDAO proxyMapUpdater = getProxyUrlDao()) {
                    String needleKey = new URL(MainApp.RESTAPI_HOST, uri).toExternalForm();
                    log.debug("needleKey : {}", needleKey);

                    ProxyUrl actual = proxyMapUpdater.getProxyUrl(
                        new ProxyUrl().setProxy(ppath)
                    );

                    log.debug("Found proxy-Actual: {}", actual);
                    if (actual != null) {
                        exchange.getOut().setHeader("Location", actual.getActual());
                        exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 301);
                    }
                    else {
                        throw makeBadRequestException("Invalid URL.", exchange, "Neither file nor mapping exists." , 400);
                    }
                }
            }
            else {
                InputStream is = new BufferedInputStream(new FileInputStream(file));
                String mimeType = URLConnection.guessContentTypeFromStream(is);
                is.close();

                exchange.getOut().setHeader(Exchange.CONTENT_TYPE, mimeType);
                exchange.getOut().setHeader(Exchange.CONTENT_LENGTH, file.length());
                exchange.getOut().setBody(file);
            }
        })
        .end();
        from("direct:rm_files")
            .split(new Expression() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T evaluate(Exchange exchange, Class<T> type) {
                    Message IN = exchange.getIn();
                    DeleteFilesRequest body = exchange.getIn().getBody(DeleteFilesRequest.class);

                    String authToken = exchange.getIn().getHeader(Constants.AUTHORIZATION, String.class);
                    IN.removeHeaders("*");

                    IN.setHeader(Constants.AUTHORIZATION, authToken);
//                    IN.setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);

                    log.debug("headers: {}", IN.getHeaders().entrySet());
                    return (T) body.getFiles().iterator();
                }
            }, fileListResultAggregator)
            .to("vm:delete")
            .end();

        from("vm:delete")
            // Convert to JSON to be Post-body
            .marshal().json(JsonLibrary.Jackson)
//            .threads(serviceConfig.getPoolSize(), serviceConfig.getMaxPoolSize())
            .enrich(
                getHttp4Proto(authAgent.getApiUrl()) + ppath_delete_files,
                (Exchange original, Exchange resource) -> {

                    log.debug("enrich headers: {}", original.getIn().getHeaders().entrySet());

//                    log.debug("postedData: {} ", original.getIn().getBody(String.class));
//                    log.debug("postedData Out: {} ", original.getOut().getBody(String.class));

                    final Integer code = resource.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);

                    FileResponse postedData = coerceClass(original.getIn(), FileResponse.class);
                    log.debug("postedData: {} ", postedData );

                    log.debug("code {}", code);

                    if (code == null || HttpStatus.SC_OK != code) {
                        ReadsError respData = coerceClass(resource.getIn(), ErrorObject.class);
//                        String respData = resource.getIn().getBody(String.class);
                        log.debug("respData: " + respData);
                        postedData.setError(respData);
                    }
//                    else {
////                        String postedData = resource.getIn().getBody(String.class);
//                        original.getOut().setBody(postedData);
//                    }
                    original.getOut().setBody(postedData);
                    return original;
                }
            ).outputType(FileResponse.class)
            .end();
    }

    public <T> T coerceClass(Message rsrcIn, Class<T> type) {
        T obj = null;
        try {
            String string = rsrcIn.getBody(String.class);
            obj = objectMapper.readValue(string, type);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public static final Pattern httpPattern = Pattern.compile("(https{0,1})(.+)");

    public static String getHttp4Proto(String url) {
        String str = url;
        Matcher m = httpPattern.matcher(url);
        if (m.find()) {
            str = m.replaceFirst("$1" + "4$2");
        }
        return str;
    }

    public static String http4Suffix(String url) {
        return url + "?okStatusCodeRange=100-999&throwExceptionOnFailure=true"
                + "&disableStreamCache=true";
//		+ "&transferException=false&"
//		+ "useSystemProperties=true";
    }

    // TODO: 2/10/18 save URL mapping to DB or file
    class ReplyProxyUrls implements Processor {
        @Override
        public void process(Exchange exchange) throws IOException {

            UploadData obj = exchange.getIn().getBody(UploadData.class);
            exchange.getOut().setBody(obj.getFiles().stream().map(usrf -> usrf).collect(Collectors.toList()));
        }
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
                .post("/upload/{bucketId}/{author}/{destDir}")
                .description("Upload (and revise) files. Uploading to the same name and path results in creating a new <i>version</i> of the file.")
                .bindingMode(RestBindingMode.off)
                .consumes("multipart/form-data")
                .produces("application/json")
                .to("direct:rest.multipart")

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
//                .consumes("application/json")
                .produces("application/json")
                .to("direct:rest.list_files")

                // List File Versions
                .post("/lsvers").type(ListFilesRequest.class).outType(ListFilesResponse.class)
                .description("List file and versions thereof")
                .bindingMode(RestBindingMode.auto)
                .produces("application/json")
                .to("direct:rest.list_filevers")

                .delete("/rm").type(DeleteFilesRequest.class)
                .outType(DeleteFilesResponse.class)
                .bindingMode(RestBindingMode.auto)
                .produces("application/json")
                .to("direct:rest.rm_files")

                // Download
                .get(downloadServicePath + "?matchOnUriPrefix=true")
                .description("File reference")
                .bindingMode(RestBindingMode.off)
                .to("direct:rest.file_proxy")

                // chmodDir
                .post(grantDwnldService).type(DirectoryAccessRequest.class)
                .bindingMode(RestBindingMode.auto)
                .produces("application/json")
//                .consumes("application/json")
                .to("direct:rest.dir_auth")

                // chmod on bucket
                .put(bchmodService).type(TouchBucketRequest.class)
                .description("Update bucket")
                .bindingMode(RestBindingMode.auto)
                .produces("application/json")
                .to("direct:rest.bchmod");


        /**
         * TODO: 2/10/18 -
         *  b2_start_large_file
         *  b2_get_upload_part_url
         *  b2_finish_large_file
         *
         FILE_NAME='bigfile.dat'
         FILE_SIZE=`stat -s $FILE_NAME | awk '{print $'8'}' | sed 's/st_size=\([0-9]*\)/\1/g'`

         # Prepare the large file be spliting it into chunks
         split -b 100000000 $FILE_NAME bz_chunk_
         FILE_CHUNK_NAMES=(`ls -1 bz_chunk_*`)

         # Upload file
         UPLOAD_URL=''; # Provided by b2_get_upload_part_url
         ...
         */

        // Update a File objectMapper.readValue(
        // .put("/mod/{filePath}").to("direct:putFile")

        // Get file info
        //// .get("/file/{filePath}").to("direct:infoFile")

    }

    private class SaveLocally implements Processor {

        @Override
        public void process(Exchange exchange) throws B2BadRequestException {

            final Message messageIn = exchange.getIn();

            MediaType mediaType = messageIn.getHeader(Exchange.CONTENT_TYPE, MediaType.class);


                try {
                    InputRepresentation representation = new InputRepresentation(messageIn.getBody(InputStream.class), mediaType);


                    String contextId = URLDecoder.decode(messageIn.getHeader(Constants.TRNSNT_FILE_DESTDIR, String.class), Constants.UTF_8);

                    if (contextId == null ) {
                        contextId = "";
                    }
                    else {
                        contextId = contextId.replaceAll(serviceConfig.getCustomSeparator(), "/");
                    }

                    String bucketId = URLDecoder.decode(messageIn.getHeader("bucketId", String.class), Constants.UTF_8);
                    String author = URLDecoder.decode(messageIn.getHeader("author", String.class), Constants.UTF_8);


//                    if (contextId.contains("/")) {
//                        List<String> parts = Arrays.asList(contextId.split("/"));
//                        author = parts.get(0);
//                        contextId = String.join("/", parts.subList(1, parts.size()));
//                    }

                    List<FileItem> items = new RestletFileUpload(new DiskFileItemFactory()).parseRepresentation(representation);

                    if (!items.isEmpty()) {

                        UploadData uploadData = new UploadData();

                        for (FileItem item : items) {
                            if (item.isFormField()) {
                                uploadData.putFormField(item.getFieldName(), item.getString());
                            } else {
                                String pathFromUser = contextId + File.separatorChar + item.getFieldName();
                                String partialPath = URLEncoder.encode(pathFromUser + File.separatorChar + item.getName(), Constants.UTF_8)
                                        .replaceAll("%2F", "/");

                                log.debug("partialPath: {}", partialPath);
                                Path destination = Paths.get(serviceConfig.getDocRoot() + File.separatorChar + partialPath);

                                Files.createDirectories(destination.getParent());

                                Files.copy(item.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

                                UserFile userFile = new UserFile(destination)
                                        .setContentType(item.getContentType())
                                        .setBucketId(bucketId)
                                        .setRelativePath(partialPath);

                                userFile.setAuthor(author);

                                userFile.setDownloadUrl(buildURLString(MainApp.RESTAPI_ENDPOINT, "file", partialPath));

                                log.debug("userFile.getDownloadUrl: {}", userFile.getDownloadUrl());

                                item.delete();
                                uploadData.addFile(userFile);
                            }
                        }
                        try (ProxyUrlDAO proxyMapUpdater = getProxyUrlDao()) {
                            uploadData.getFiles().forEach(aUserFile -> {
                                aUserFile.setTransientId((Long) proxyMapUpdater.saveOrUpdateMapping(
                                        new ProxyUrl()
                                                .setProxy(aUserFile.getRelativePath())
                                                .setSha1(aUserFile.getSha1())
                                                .setBucketId(aUserFile.getBucketId())
                                                .setContentType(aUserFile.getContentType())
                                                .setSize(aUserFile.getSize())
                                ));
                            });
                        } catch (Exception e) {
                            throw makeBadRequestException(e, exchange, "DB update error." , 500);
                        }
                        exchange.getOut().setBody(uploadData);
                    }
                } catch (Exception e) {
                    throw makeBadRequestException(e, exchange, "Incomplete Request" , 400);
                }

//            else {


//                log.debug("No media type in Header");
//                exchange.getOut().setBody(new ErrorObject()
//                    .setMessage("No media type in Header")
//                    .setCode("Null media not allowed")
//                    .setStatus(400));
//                exchange.setException(new Throwable("No media type in Header"));
//
//            }
        }
    }

    public static B2BadRequestException makeBadRequestException(String msg, Exchange exchange, String submsg, int status) {
        exchange.getOut().setBody(new ErrorObject()
                .setMessage(msg)
                .setCode(submsg)
                .setStatus(status));

        return new B2BadRequestException(msg);
    }

    public static B2BadRequestException makeBadRequestException(Exception e, Exchange exchange, String submsg, int status) {
        return makeBadRequestException(e.getMessage(), exchange, submsg, status);
    }

    private URL buildURL(URL endpointURL, String... paths) {
        try {
            return new URL(endpointURL , endpointURL.getPath() + "/" + String.join("/", paths));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private String buildURLString(URL endpointURL, String... paths) {
        return buildURL(endpointURL , paths).toString();
    }

    private String buildURLString(String restapiEndpoint, String... paths) {
        try {
            return buildURLString(new URL(restapiEndpoint), paths);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private ProxyUrlDAO getProxyUrlDao() {
        return new ProxyUrlDAO(serviceConfig.getNeo4jConf(), objectMapper);
    }

    private class ListSplitExpression implements Expression {
        @Override
        @SuppressWarnings("unchecked")
        public <T> T evaluate(Exchange exchange, Class<T> type) {
            return (T) exchange.getIn().getBody(UploadData.class).getFiles().iterator();
        }
    }

    private class PersistMapping implements Processor {

        @Override
        public void process(Exchange exchange) {
            UploadFileResponse uploadResponse = exchange.getIn().getBody(UploadFileResponse.class);

            UserFile uf = exchange.getIn().getHeader(Constants.USER_FILE, UserFile.class);

            try (ProxyUrlDAO proxyMapUpdater = getProxyUrlDao()) {
                Long id = (Long) proxyMapUpdater.saveOrUpdateMapping(
                    new ProxyUrl()
//                        .setProxy(uf.getDownloadUrl())
                        .setProxy(uf.getRelativePath())
                        .setBucketId(uploadResponse.getBucketId())
                        .setBucketType(uploadResponse.getBucketType())
                        .setTransientId(uf.getTransientId())
                        // Sha1 is used as ID in Neo
                        .setSha1(uploadResponse.getContentSha1())
                        .setActual(uploadResponse.getDownloadUrl())
                        .setB2Complete(true)
                );
            }

        }
    }
}
