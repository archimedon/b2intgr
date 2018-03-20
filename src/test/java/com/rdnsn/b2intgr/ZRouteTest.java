package com.rdnsn.b2intgr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.rdnsn.b2intgr.api.AuthResponse;
import com.rdnsn.b2intgr.api.ListFilesRequest;
import com.rdnsn.b2intgr.dao.ProxyUrlDAO;
import com.rdnsn.b2intgr.processor.AuthAgent;
import com.rdnsn.b2intgr.route.ZRouteBuilder;
import com.rdnsn.b2intgr.util.Constants;
import com.rdnsn.b2intgr.util.JsonHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.impl.DefaultCamelContext;
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
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class ZRouteTest extends CamelTestSupport {
    private static final Logger LOG = LoggerFactory.getLogger(ZRouteTest.class);

    private static final String ENV_PREFIX = "B2I_";
    private static final String CONFIG_ENV_PATTERN = "\\$([\\w\\_\\-\\.]+)";
    private static final long TTL = (12 * 60 * 60) - 10;

    private static long lastmod = 0;

    private static AuthAgent authAgent;

    public static URL RESTAPI_HOST;
    public static URL RESTAPI_ENDPOINT;
    private static ObjectMapper objectMapper;
    private static CloudFSConfiguration serviceConfig;
    private ZRouteBuilder zroute;
//    private CamelContext camelContext;

    private String bucketId ="2ab327a44f788e635ef20613";


    private static final int ENOENT = 2;        /* No such file or directory */
    private static final int EACCES = 13;       /* Permission denied */
    private static final int EFAULT = 14;       /* Bad address */
    private static final int EBUSY = 16;        /* Device or resource busy */
    private static final int ENOTCONN = 107;    /* Transport endpoint is not connected */
    private static final int ETIMEDOUT = 110;   /* Connection timed out */
    private static final int EHOSTDOWN = 112;   /* Host is down */

//    @EndpointInject(uri = "mock:result")
//    MockEndpoint resultEndpoint;


    private int numberOfBuckets = 2;
    private static final String ENDPOINT_URI = "http4://localhost:8080/cloudfs/api/v1";
    private static final String LIST_VERSIONS_URI = ENDPOINT_URI + "/lsvers";
    private static final String LISTBUCKETS_VERSIONS_URI = ENDPOINT_URI + "/list";

    public ZRouteTest() throws Exception {
        super();
        System.err.println("\nnew ZRouteTest");
//        setUseRouteBuilder(false);
//        System.setProperty("skipStartingCamelContext", "true");
    }

    @Override
    protected JndiContext createJndiContext() throws Exception {
        final JndiContext jndiContext = new JndiContext();
        System.err.println("\ncreateJndiContext");

        ZRouteTest.authAgent = new AuthAgent(serviceConfig.getRemoteAuthenticationUrl(), serviceConfig.getBasicAuthHeader(), this.objectMapper);
        jndiContext.bind("authAgent", authAgent);

        return jndiContext;
    }

    @Override
    public RouteBuilder createRouteBuilder() throws Exception
    {
        System.err.println("\ncreateRouteBuilder");
        return new ZRouteBuilder(objectMapper, serviceConfig, authAgent);
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
                "bucketId", bucketId,
                "startFileName" , "",
                "prefix" , "hh/site/images/v2/",
                "delimiter" , "/",
                "maxFileCount" , 70
        );

        final String token = authAgent.getAuthResponse().getAuthorizationToken();
        LOG.error("Token: {}", token);

        final Message responseOut = template.send(LIST_VERSIONS_URI, (exchange) -> {

            // Ensure Empty
            exchange.getIn().removeHeaders("*");
            exchange.getIn().setBody(null);

            exchange.getIn().setHeader(Constants.AUTHORIZATION, token);
            exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);

            exchange.getIn().setBody(JsonHelper.objectToString(objectMapper, body));

        }).getOut();

//        LOG.debug("Headers: {}",
//            responseOut.getHeaders().entrySet().stream().map( entry -> String.format("name: %s%nvalue: %s", entry.getKey(), "" + entry.getValue())).collect(Collectors.toList()));

        Integer code = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);

        assertNotNull("code expected", code);
        assertEquals(HttpStatus.SC_OK, code.longValue());

        Map<String, List<Map<String, Object>>> filesWrap = JsonHelper.coerceClass(objectMapper, responseOut, HashMap.class);
        List<Map<String, Object>> files = List.class.cast(filesWrap.get("files"));
        Map<String, Object> fileItem = files.get(0);


        assertNotNull("files List expected", files);
//        assertListSize(files, numberOfBuckets);
        assertTrue(((String)fileItem.get("fileName")).startsWith((String) body.get("prefix")));

    }
//
//
//    @Test
//    public void testUpload() throws Exception {
//
//        final Map<String, Object> body = ImmutableMap.of(
//                "bucketId", bucketId,
//                "startFileName" , "",
//                "prefix" , "hh/site/images/v2/",
//                "delimiter" , "/",
//                "maxFileCount" , 70
//        );
//
//        final String token = authAgent.getAuthResponse().getAuthorizationToken();
//        LOG.error("Token: {}", token);
//
//        final Message responseOut = template.send(LIST_VERSIONS_URI, (exchange) -> {
//
//            // Ensure Empty
//            exchange.getIn().removeHeaders("*");
//            exchange.getIn().setBody(null);
//
//            exchange.getIn().setHeader(Constants.AUTHORIZATION, token);
//            exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
//
//            exchange.getIn().setBody(JsonHelper.objectToString(objectMapper, body));
//
//        }).getOut();
//
////        LOG.debug("Headers: {}",
////            responseOut.getHeaders().entrySet().stream().map( entry -> String.format("name: %s%nvalue: %s", entry.getKey(), "" + entry.getValue())).collect(Collectors.toList()));
//
//        Integer code = responseOut.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
//
//        assertNotNull("code expected", code);
//        assertEquals(HttpStatus.SC_OK, code.longValue());
//
//        Map<String, List<Map<String, Object>>> filesWrap = JsonHelper.coerceClass(objectMapper, responseOut, HashMap.class);
//        List<Map<String, Object>> files = List.class.cast(filesWrap.get("files"));
//        Map<String, Object> fileItem = files.get(0);
//
//
//        assertNotNull("files List expected", files);
////        assertListSize(files, numberOfBuckets);
//        assertTrue(((String)fileItem.get("fileName")).startsWith((String) body.get("prefix")));
//
//    }
//


    /**
     *
     */
    @Test
    public void testBackBlazeConnect() {
        assertNotNull("authAgent", authAgent);
        assertNotNull("authResponse", authAgent.getAuthResponse());
    }

    /**
     *
     */
    @Test
    public void testDBConnection() {
        ProxyUrlDAO purl = new ProxyUrlDAO(serviceConfig.getNeo4jConf(), objectMapper);
        boolean ans = purl.isAlive();
        assertTrue("Connection to Neo4j Failed", ans);
        purl = null;
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

        final Message responseOut = template.send(LISTBUCKETS_VERSIONS_URI, (exchange) -> {
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

    @Override
    public void doPreSetup() throws Exception {
        System.err.println("\ndoPreSetup");
        this.objectMapper = new ObjectMapper();
        this.serviceConfig = getSettings();
        this.RESTAPI_HOST = new URL(serviceConfig.getProtocol(), serviceConfig.getHost(), serviceConfig.getPort(), "/");
        this.RESTAPI_ENDPOINT = new URL(RESTAPI_HOST, serviceConfig.getContextUri());

        if (! setupWorkDirectory()) {
            System.exit(ENOENT);
        }

    }


//    @Override
//    public void doPostSetup() throws Exception {
//        System.err.println("\ndoPostSetup");
//        LOG.debug("doPostSetup");
//    }


    @Before
    public void beforeEachTest() throws Exception {
        LOG.debug("beforeEachTest");
        template = context.createProducerTemplate();
        template.start();
    }

    @After
    public void afterEachTest() throws Exception {
        System.err.println("\nafterEachTest");
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
                    System.err.println("Made DocRoot directory " + f.getPath());
                    return true;
                }
                else {
                    throw new RuntimeException("Make DocRoot directory failed: " + f.getPath());
                }
            } else {
                System.err.println("DocRoot directory exists: " + f.getPath());
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

        private CloudFSConfiguration getSettings() throws IOException {

            // Load config file
            String confFile = readStream(getClass().getResourceAsStream("/config.json"));

            // interpolate $vars in config file
            confFile = injectExtern(confFile);

//             Override with specially prefixed environment variables
            return doEnvironmentOverrides(objectMapper.readValue(confFile, CloudFSConfiguration.class), confFile);
        }


        private List<String> crawl(Map<String, Object> map) {
            List nkeys = new LinkedList();
            map.entrySet().forEach( entry -> {
                if ( entry.getValue() != null && entry.getValue() instanceof Map ) {
                    nkeys.addAll(
                            crawl((Map<String, Object>)entry.getValue())
                                    .stream().map( innerKey -> entry.getKey() + '.' + innerKey).collect(Collectors.toList()));
                }
                else {
                    nkeys.add(entry.getKey());
                }
            });
            return nkeys;
        }

        private CloudFSConfiguration doEnvironmentOverrides(CloudFSConfiguration confObject, String confFile) throws IOException {
            Map<String, Object> propValueMap = objectMapper.readValue(confFile, HashMap.class);
            crawl(propValueMap)
                    .forEach( propName -> {
                        String ev = null;

                        if ( (ev = System.getenv(ENV_PREFIX + propName )) != null) {
                            try {
                                if (propName.indexOf('_') > 0) {
                                    PropertyUtils.setNestedProperty(confObject, propName.replaceAll("_", "."), ev);
                                }
                                else {
                                    BeanUtils.setProperty(confObject, propName, ev);
                                }
                                System.err.format("Override config['%s'] with env['%s']%n", propName , ENV_PREFIX + propName);
                            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            return confObject;
        }

        private String injectExtern(String confFile) {
            Matcher m = Pattern.compile(CONFIG_ENV_PATTERN).matcher(confFile);
            String tmp = null;
            while (m.find()) {
                tmp = System.getenv(m.group(1));
                if (tmp != null && tmp.length() > 0) {
                    confFile = m.replaceAll(tmp);
                }
            }
            return confFile;
        }

    }


