package com.rdnsn.b2intgr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.rdnsn.b2intgr.api.AuthResponse;
import com.rdnsn.b2intgr.api.GetUploadUrlResponse;
import com.rdnsn.b2intgr.api.UploadFileResponse;
import com.rdnsn.b2intgr.dao.ProxyUrlDAO;
import com.rdnsn.b2intgr.processor.AuthAgent;
import com.rdnsn.b2intgr.route.ZRouteBuilder;
import com.rdnsn.b2intgr.util.Configurator;
import com.rdnsn.b2intgr.util.Constants;
import com.rdnsn.b2intgr.util.JsonHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.util.jndi.JndiContext;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.rdnsn.b2intgr.route.ZRouteBuilder.getHttp4Proto;


public class ZRouteTest extends CamelTestSupport {
    private static final Logger LOG = LoggerFactory.getLogger(ZRouteTest.class);

    private static final String ENV_PREFIX = "B2I_";
    private static final String CONFIG_ENV_PATTERN = "\\$([\\w\\_\\-\\.]+)";
    private static final String LIST_VERSIONS_URI = "/lsvers";
    private static final String LIST_BUCKETS_URI = "/list";

    private static final int ENOENT = 2;        /* No such file or directory */


    private static AuthAgent authAgent;
    private static GetUploadUrlResponse getUploadUrlResponse;

    public static URI RESTAPI_ENDPOINT;
    private static ObjectMapper objectMapper;
    private static CloudFSConfiguration serviceConfig;
    private final String configFilePath = "/config.json";

    private int numberOfBuckets = 2;

    public ZRouteTest() throws Exception {
        super();
//        setUseRouteBuilder(false);
//        System.setProperty("skipStartingCamelContext", "true");
    }

    @Override
    public RouteBuilder createRouteBuilder() throws Exception
    {
//        zRouteBuilder = new ZRouteBuilder(objectMapper, serviceConfig, authAgent);
        LOG.info("Create RouteBuilder");
        return new ZRouteBuilder(objectMapper, serviceConfig, authAgent);

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
        // scheme, null, host, -1, path, null, fragment
        // String scheme, String userInfo, String host, int port, String path, String query, String fragment)
        RESTAPI_ENDPOINT = new URI(serviceConfig.getProtocol(), null, serviceConfig.getHost(), serviceConfig.getPort(), serviceConfig.getContextUri(), null, null);

        if (! setupWorkDirectory()) {
            System.exit(ENOENT);
        }
    }

    @Override
    protected JndiContext createJndiContext() throws Exception {
        final JndiContext jndiContext = new JndiContext();
        LOG.info("Create JndiContext");

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
                "maxFileCount" , 70
        );

        final String token = authAgent.getAuthResponse().getAuthorizationToken();

        final Message responseOut = template.send(getHttp4Proto(RESTAPI_ENDPOINT + LIST_VERSIONS_URI), (exchange) -> {

            // Ensure Empty
            exchange.getIn().removeHeaders("*");
            exchange.getIn().setBody(null);

            exchange.getIn().setHeader(Constants.AUTHORIZATION, token);
            exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);

            exchange.getIn().setBody(JsonHelper.objectToString(objectMapper, body));

        }).getOut();

        Integer code = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);

        assertNotNull("An HttpStatus is expected", code);
        assertEquals("HttpStatus 200 expected",HttpStatus.SC_OK, code.longValue());

        Map<String, List<Map<String, Object>>> filesWrap = JsonHelper.coerceClass(objectMapper, responseOut, HashMap.class);
        List<Map<String, Object>> files = List.class.cast(filesWrap.get("files"));
        Map<String, Object> fileItem = files.get(0);


        assertNotNull("files List expected", files);
//        assertListSize(files, numberOfBuckets);
        assertTrue(((String)fileItem.get("fileName")).startsWith((String) body.get("prefix")));

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

        final Message responseOut = template.send(getHttp4Proto(RESTAPI_ENDPOINT + LIST_BUCKETS_URI), (exchange) -> {
            // Ensure Empty
            exchange.getIn().removeHeaders("*");
            exchange.getIn().setBody(null);
        }).getOut();

        assertEquals(HttpStatus.SC_OK, responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class).longValue());

        Map<String, List<Map<String, Object>>> bucketsWrap = JsonHelper.coerceClass(objectMapper, responseOut, HashMap.class);
        List<Map<String, Object>> bucketList = List.class.cast(bucketsWrap.get("buckets"));

        Map<String, Object> bucket = bucketList.get(0);

        assertNotNull("bucketList expected", bucketList);
        assertListSize(bucketList, numberOfBuckets);
        assertEquals(serviceConfig.getRemoteAccountId(), bucket.get("accountId"));
    }

    @Test
    public void testUploadSmall() throws IOException {
        String basePath = getClass().getResource("/test-samples").getPath();
        Path filePath = Paths.get(basePath, "/small.file");

        UploadFileResponse uploadResponse = upload(filePath);
        assertNotNull("fileId expected", uploadResponse.getFileId());
        log.info(" uploadResponse: '{}'", uploadResponse);
    }

    @Test
    public void testUploadLarge() throws IOException {
        String basePath = getClass().getResource("/test-samples").getPath();
        Path filePath = Paths.get(basePath, "/large.file");

        UploadFileResponse uploadResponse = upload(filePath);
        assertNotNull("fileId expected", uploadResponse.getFileId());
        log.info(" uploadResponse: '{}'", uploadResponse);
    }

    public UploadFileResponse upload(Path filePath) throws IOException {
        log.info("sample file: '{}'", filePath.toString());

        Path assetPath = Paths.get(serviceConfig.getDocRoot());
        String relpath = assetPath.relativize(filePath).toString();

        String SHA1 = JsonHelper.sha1(filePath.toFile());

        final Message responseOut = template.send(getHttp4Proto(getUploadUrlResponse.getUploadUrl())
                + "?throwExceptionOnFailure=false&okStatusCodeRange=100", (Exchange innerExchg) -> {

            innerExchg.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
            innerExchg.getIn().setHeader(Constants.X_BZ_FILE_NAME, relpath);
            innerExchg.getIn().setHeader(Constants.X_BZ_CONTENT_SHA1, SHA1);
            innerExchg.getIn().setHeader(Exchange.CONTENT_LENGTH, filePath.toFile().length());
//            innerExchg.getIn().setHeader(Exchange.CONTENT_TYPE, "text/plain");
            innerExchg.getIn().setHeader(Exchange.CONTENT_TYPE, "b2/x-auto");
            innerExchg.getIn().setHeader(Constants.AUTHORIZATION, getUploadUrlResponse.getAuthorizationToken());
            innerExchg.getIn().setHeader(Constants.X_BZ_INFO_AUTHOR, "testCase");
            innerExchg.getIn().setBody(filePath.toFile());
        }).getOut();

        final Integer code = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);

        log.info("HTTP_RESPONSE_CODE:{ '{}' XBzFileName: '{}'}", code, relpath);

        assertEquals(HttpStatus.SC_OK, responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class).longValue());
        return JsonHelper.coerceClass(objectMapper, responseOut, UploadFileResponse.class);
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

//    @Override
//    public void doPostSetup() throws Exception {
//        LOG.debug("doPostSetup");
//    }

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
