package com.rdnsn.b2intgr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.rdnsn.b2intgr.api.*;
import com.rdnsn.b2intgr.dao.ProxyUrlDAO;
import com.rdnsn.b2intgr.model.UserFile;
import com.rdnsn.b2intgr.processor.AuthAgent;
import com.rdnsn.b2intgr.route.ZRouteBuilder;
import com.rdnsn.b2intgr.util.Configurator;
import com.rdnsn.b2intgr.util.Constants;
import com.rdnsn.b2intgr.util.JsonHelper;
import com.rdnsn.b2intgr.util.MirrorMap;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.util.jndi.JndiContext;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.CharacterSet;
import org.restlet.data.Disposition;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.ext.html.FormData;
import org.restlet.ext.html.FormDataSet;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.rdnsn.b2intgr.route.ZRouteBuilder.getHttp4Proto;
import static com.rdnsn.b2intgr.route.ZRouteBuilder.makeBadRequestException;
import static com.rdnsn.b2intgr.util.JsonHelper.sha1;


public class ZRouteTest extends CamelTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ZRouteTest.class);

    private static AuthAgent authAgent;
    private static GetUploadUrlResponse getUploadUrlResponse;

    private static ObjectMapper objectMapper;
    private static CloudFSConfiguration serviceConfig;
    private final String configFilePath = "/config.json";
    private ZRouteBuilder zRouteBuilder;

    String basePath = getClass().getResource("/test-samples").getPath();

    class TestUpload {
        String b2FileId;
        String b2FileName;
        String formName;
        String bucketId;
        String sha1;
        Path filePath;

        TestUpload(String relpath, String formName) {
            this.formName = formName;
            this.filePath = Paths.get(basePath, relpath);
            this.sha1 = sha1(filePath.toFile());
        }
    }

    private Map<String, TestUpload> smallTestFiles = ImmutableMap.of(
        "1024b", new TestUpload("/1024b-file.txt", "1024b"),
        "small", new TestUpload("/small.file", "small"),
        "logo", new TestUpload("/208x32.png", "logo"),
        "68b-img", new TestUpload("/68b-img.gif", "68b-img")
    );

    public ZRouteTest() throws Exception {
        super();
//        setUseRouteBuilder(false);
//        System.setProperty("skipStartingCamelContext", "true");
    }

    @Override
    public RouteBuilder createRouteBuilder() throws Exception
    {
        LOG.info("Create RouteBuilder");
        this.zRouteBuilder = new ZRouteBuilder(objectMapper, serviceConfig, authAgent);
        return zRouteBuilder;

    }

    @Override
    public void doPreSetup() throws Exception {

        this.objectMapper = new ObjectMapper();

        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        objectMapper.configure(MapperFeature.USE_ANNOTATIONS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        String confFile = readStream(getClass().getResourceAsStream(configFilePath));

        this.serviceConfig = new Configurator(objectMapper).getConfiguration(confFile);

        LOG.debug(serviceConfig.toString());

        // Override DocRoot for tests
        serviceConfig.setDocRoot(getClass().getResource("/").getPath());
    }

    @Override
    protected void doPostSetup() {

        try {
            // Sets system variable @code(ZRouteBuilder.bucketMap)
            lookupAvailableBuckets(this.zRouteBuilder);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(Constants.EFAULT);
        }

        if (! setupWorkDirectory()) {
            System.exit(Constants.ENOENT);
        }
    }

    private void lookupAvailableBuckets(ZRouteBuilder zRouteBuilder) throws Exception {

        Endpoint gatewayEndpoint = zRouteBuilder.endpoint("direct:rest.list_buckets");
        Producer producer = gatewayEndpoint.createProducer();

        LOG.info("Populating available buckets ...");
        Exchange exchange = gatewayEndpoint.createExchange(ExchangePattern.OutOnly);
        producer.process(exchange);

        BucketListResponse buckets = JsonHelper.coerceClass(objectMapper, exchange.getOut(), BucketListResponse.class);

        MirrorMap<String, String> bucketIdNameMap = new MirrorMap<String, String>(
            buckets.getBuckets().stream().collect(Collectors.toMap(B2Bucket::getBucketId, B2Bucket::getBucketName))
        );

        zRouteBuilder.setBucketMap(bucketIdNameMap);
    }

    @Override
    protected JndiContext createJndiContext() throws Exception {
        final JndiContext jndiContext = new JndiContext();
        LOG.info("Creating JndiContext ...");

        ZRouteTest.authAgent = new AuthAgent(serviceConfig.getRemoteAuthenticationUrl(), serviceConfig.getBasicAuthHeader(), this.objectMapper);
        jndiContext.bind("authAgent", authAgent);

        return jndiContext;
    }

    /**
     *
     */
    @Test
    public void test0BackBlazeConnect() {
        assertNotNull("authAgent", authAgent);
        assertNotNull("authResponse", authAgent.getAuthResponse());
        assertNotNull("authResponse token", authAgent.getAuthResponse().getAuthorizationToken());
    }

    /**
     *
     */
    @Test
    public void test1DBConnection() {
        ProxyUrlDAO purl = new ProxyUrlDAO(serviceConfig.getNeo4jConf(), objectMapper);
        boolean ans = purl.isAlive();
        assertTrue("Connection to Neo4j Failed", ans);
    }

    /*
    {"files": [
        {
            "fileId": "4_z2ab327a44f788e635ef20613_f1033cee59fc240fb_d20180214_m100250_c001_v0001005_t0018",
            "fileName": "hh/site/images/v2/1024px-Jamaica_relief_location_map.jpg",
            "downloadUrl": "https://f001.backblazeb2.com/file/b2public/hh/site/images/v2/1024px-Jamaica_relief_location_map.jpg",
            "contentType": "image/jpeg",
            "action": "upload",
            "fileInfo": {
                "author": "unknown"
            },
            "size": 108179,
            "uploadTimestamp": 1518602570000
        }, ...
    }
     */
    @Test
    public void testListVersions() throws Exception {

        final Map<String, Object> body = ImmutableMap.of(
                "bucketId", serviceConfig.getRemoteBucketId(),
                "startFileName" , "",
                "prefix" , "hh/site/images/v2/",
                "delimiter" , "/",
                "maxFileCount" , 7
        );

        final Message responseOut = template.send(getHttp4Proto(ZRouteBuilder.RESTAPI_ENDPOINT + ZRouteBuilder.listFileVersService), (exchange) -> {

            // Ensure Empty
            exchange.getIn().removeHeaders("*");
            exchange.getIn().setBody(null);

            exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
            exchange.getIn().setBody(JsonHelper.objectToString(objectMapper, body));

        }).getOut();


        log.debug("responseOut Headers:\n" +  responseOut.getHeaders().entrySet().stream().map(entry -> entry.getKey() + " ::: " + entry.getValue()).collect(Collectors.joining("\n")));

        Integer code = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);

        assertNotNull("An HttpStatus is expected", code);
        assertEquals("HttpStatus 200 expected",HttpStatus.SC_OK, code.longValue());

        String rbody = responseOut.getBody(String.class);
        Map<String, List<Map<String, Object>>> filesWrap = JsonHelper.coerceClass(objectMapper, rbody, HashMap.class);
        List<Map<String, Object>> files = List.class.cast(filesWrap.get("files"));
        Map<String, Object> fileItem = files.get(0);


        assertNotNull("files List expected", files);
        String fname = (String)fileItem.get("fileName");
        String prefix = (String) body.get("prefix");
        log.debug("flname: {}\nprefix: {}", fname, prefix);
        assertTrue(fname.startsWith(prefix));

    }
    @Test
    public void testDeleteFile() throws Exception {


        String sbody = "{"+
                "\"files\": ["+
                "{ \"fileId\": \"4_z2ab327a44f788e635ef20613_f114545477dc6bc62_d20180210_m035713_c001_v0001102_t0013\","+
                "\"fileName\": \"hh/site/images/v2/Installing Apache Karaf as a service - Apache Karaf Cookbook.webloc\""+
                "},"+
                "{"+
                "\"fileId\": \"4_z2ab327a44f788e635ef20613_f118dc058c9a22e26_d20180210_m100755_c001_v0001101_t0043\","+
                "\"fileName\": \"hh/site/images/v2/Installing Apache Karaf as a service - Apache Karaf Cookbook.webloc\""+
                "}"+
                "]"+
                "}";


        final Message responseOut = template.send(getHttp4Proto(ZRouteBuilder.RESTAPI_ENDPOINT + ZRouteBuilder.deleteFilesService), (exchange) -> {

            // Ensure Empty
            exchange.getIn().removeHeaders("*");
            exchange.getIn().setBody(null);

            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
            exchange.getIn().setBody(sbody);

        }).getOut();


        log.debug("responseOut Headers:\n" +  responseOut.getHeaders().entrySet().stream().map(entry -> entry.getKey() + " ::: " + entry.getValue()).collect(Collectors.joining("\n")));

        int code = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);

        assertNotNull("An HttpStatus is expected", code);
        assertTrue("HttpStatus 2xx expected", HttpStatus.SC_OK <= code && code < 300);

        String rbody = responseOut.getBody(String.class);
        System.out.println("rbody:\n" + rbody);

    }

    @Test
    public void testListFilenames() throws Exception {

        final Map<String, Object> body = ImmutableMap.of(
            "bucketId", serviceConfig.getRemoteBucketId(),
            "startFileName" , "",
            "prefix" , "hh/site/images/v2/",
            "delimiter" , "/",
            "maxFileCount" , 7
        );

        final Message responseOut = template.send(getHttp4Proto(ZRouteBuilder.RESTAPI_ENDPOINT + ZRouteBuilder.listFileNamesService), (exchange) -> {

            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
            exchange.getIn().setBody(JsonHelper.objectToString(objectMapper, body));
        }).getOut();


        log.debug("responseOut Headers:\n" +  responseOut.getHeaders().entrySet().stream().map(entry -> entry.getKey() + " ::: " + entry.getValue()).collect(Collectors.joining("\n")));

        Integer code = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);

        assertNotNull("An HttpStatus is expected", code);
        assertEquals("HttpStatus 200 expected",HttpStatus.SC_OK, code.longValue());

        String rbody = responseOut.getBody(String.class);

        List<Map<String, Object>> files = getListOfMap("files", rbody);

        Map<String, Object> fileItem = files.get(0);

        assertNotNull("File List expected", files);
        String fname = (String)fileItem.get("fileName");
        String prefix = (String) body.get("prefix");

        assertTrue(fname.startsWith(prefix));

    }

    private List<Map<String, Object>> getListOfMap(String itemsKey, String jsonListOfItems) {
        Map<String, List<Map<String, Object>>> filesWrap = JsonHelper.coerceClass(objectMapper, jsonListOfItems, HashMap.class);
        return List.class.cast(filesWrap.get(itemsKey));
    }
    /**
     *
     * Server Response JSON:
     * <pre>
     {"buckets": [
     {
     "accountId": "30f20426f0b1",
     "bucketId": "4a48fe8875c6214145260818",
     "bucketInfo": {},
     "bucketName" : "Kitten-Videos",
     "bucketType": "allPrivate",
     "lifecycleRules": []
     },
     { ... }
     ]}
     </pre>
     * @throws IOException
     */
    @Test
    public void testListBuckets() throws IOException {

        final Message responseOut = template.send(getHttp4Proto(ZRouteBuilder.RESTAPI_ENDPOINT + ZRouteBuilder.listBucketsService), (exchange) -> {
            // Ensure Empty
            exchange.getIn().removeHeaders("*");
            exchange.getIn().setBody(null);
        }).getOut();

        assertEquals(HttpStatus.SC_OK, responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class).longValue());

        List<Map<String, Object>> bucketList = getListOfMap("buckets", responseOut.getBody(String.class));

        assertNotNull("bucketList expected", bucketList);

        Map<String, Object> bucket = bucketList.get(0);

        assertEquals(serviceConfig.getRemoteAccountId(), bucket.get("accountId"));
    }


    private BiConsumer<Map.Entry<String, TestUpload>, BasicHttpClient> inputUploadFile = (Map.Entry<String, TestUpload> entry, BasicHttpClient client) -> {
        TestUpload sample = entry.getValue();
        if (sample.filePath.toFile().exists()) {
            try {
                client.addInput(sample.filePath.toFile(), sample.formName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    @Test
    public void testUploadSmall() throws IOException {

        final String uploadUrl = ZRouteBuilder.RESTAPI_ENDPOINT +  ZRouteBuilder.uploadFileUriService
            .replace("{bucketId}", serviceConfig.getRemoteBucketId())
            .replace("{author}", "testUser")
            .replace("{destDir}", "testing");

        BasicHttpClient client = new BasicHttpClient(uploadUrl).streamData();


        // TODO: 4/5/18 - use testfiles array when doing deletes. Rewuires a DB lookup using SHA
        smallTestFiles.entrySet().forEach(entry -> inputUploadFile.accept(entry, client));

        String str = client.post();

        // Bind Json to Object
        List<UserFile> uploadResponses = objectMapper.readValue(str, new TypeReference<List<UserFile>>(){});

        for (UserFile userFile : uploadResponses ) {
            String sha1 = userFile.getSha1();
            TestUpload upped = smallTestFiles.values().stream().filter(ufile -> ufile.sha1.equals(sha1)).findFirst().get();
            assertNotNull("sha1 expected", upped);
        }
    }

    @Test
    public void testUploadLarge() throws IOException {


        final String uploadUrl = ZRouteBuilder.RESTAPI_ENDPOINT +  ZRouteBuilder.uploadFileUriService
                .replace("{bucketId}", serviceConfig.getRemoteBucketId())
                .replace("{author}", "testUser")
                .replace("{destDir}", "testing");

        BasicHttpClient client = new BasicHttpClient(uploadUrl).streamData();
        smallTestFiles.entrySet().forEach(entry -> inputUploadFile.accept(entry, client));

        String str = client.post();

        // Bind Json to Object
        List<UserFile> uploadResponses = objectMapper.readValue(str, new TypeReference<List<UserFile>>(){});

        for (UserFile userFile : uploadResponses ) {
            String sha1 = userFile.getSha1();
            TestUpload upped = smallTestFiles.values().stream().filter(ufile -> ufile.sha1.equals(sha1)).findFirst().get();
            assertNotNull("sha1 expected", upped);
        }
    }

    @Test()
    public void testGetUploadUrl() throws IOException {

        AuthResponse remoteAuth = ZRouteTest.authAgent.getAuthResponse();

        getUploadUrlResponse = objectMapper.readValue(
            template.send( getHttp4Proto(remoteAuth.resolveGetUploadUrl()) + ZRouteBuilder.HTTP4_PARAMS, (Exchange exchange) -> {
                exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
                exchange.getIn().setHeader(Constants.AUTHORIZATION, remoteAuth.getAuthorizationToken());
                exchange.getIn().setBody(JsonHelper.objectToString(objectMapper, ImmutableMap.<String, String>of("bucketId", serviceConfig.getRemoteBucketId())));
            }).getOut().getBody(String.class),
            GetUploadUrlResponse.class);

        assertNotNull("Response expected", getUploadUrlResponse);
        assertNotNull("AuthorizationToken expected", getUploadUrlResponse.getAuthorizationToken());
        assertNotNull("UploadUrl expected", getUploadUrlResponse.getUploadUrl());
    }

    @Before
    public void beforeEachTest() throws Exception {
        template = context.createProducerTemplate();
        template.start();
    }

    @After
    public void afterEachTest() throws Exception {
        template.stop();
    }

    @Override
    public boolean isCreateCamelContextPerClass() {
        // we override this method and return true, to tell Camel test-kit that
        // it should only create CamelContext once (per class), so we will
        // re-use the CamelContext between each test method in this class
        return true;
    }

    private boolean setupWorkDirectory() {
        File f = new File(serviceConfig.getDocRoot());
        if (!f.exists()) {
            if (f.mkdirs()){
                LOG.info("Made DocRoot directory " + f.getPath());
                return true;
            }
            else {
                throw new RuntimeException("Make DocRoot directory failed: " + f.getPath());
            }
        } else {
            LOG.info("DocRoot directory exists: " + f.getPath());
            return true;
        }
    }

    private String readStream(InputStream stream) throws IOException {
        ByteArrayOutputStream into = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int n = -1; 0 < (n = stream.read(buf)); into.write(buf, 0, n)){}
        into.close();
        return into.toString();
    }
}
