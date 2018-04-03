package com.rdnsn.b2intgr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdnsn.b2intgr.api.B2Bucket;
import com.rdnsn.b2intgr.api.BucketListResponse;
import com.rdnsn.b2intgr.dao.ProxyUrlDAO;
import com.rdnsn.b2intgr.processor.AuthAgent;
import com.rdnsn.b2intgr.route.ZRouteBuilder;
import com.rdnsn.b2intgr.util.Configurator;
import com.rdnsn.b2intgr.util.Constants;
import com.rdnsn.b2intgr.util.JsonHelper;
import com.rdnsn.b2intgr.util.MirrorMap;
import org.apache.camel.*;
import org.apache.camel.http.common.HttpMethods;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jndi.JndiContext;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.rdnsn.b2intgr.route.ZRouteBuilder.getHttp4Proto;


/**
 * Initialize and start the camel routes
 */
public class MainApp {
    private static final Logger LOG = LoggerFactory.getLogger(MainApp.class);

    private static final long TTL = (12 * 60 * 60) - 10;

    private static long lastmod = 0;

    public static URL RESTAPI_HOST;
    public static URL RESTAPI_ENDPOINT;
    private final ObjectMapper objectMapper;
    private final CloudFSConfiguration serviceConfig;

    private static final int ENOENT = 2;        /* No such file or directory */
    private static final int EACCES = 13;       /* Permission denied */
    private static final int EFAULT = 14;       /* Bad address */
    private static final int EBUSY = 16;        /* Device or resource busy */
    private static final int ENOTCONN = 107;    /* Transport endpoint is not connected */
    private static final int ETIMEDOUT = 110;   /* Connection timed out */
    private static final int EHOSTDOWN = 112;   /* Host is down */
    private final String configFilePath = "/config.json";

    public MainApp(String[] args) throws IOException {
//        if ( args.length > 1 ) {
//            configFilePath = "/" + args[0];
//        }
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        objectMapper.configure(MapperFeature.USE_ANNOTATIONS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

//        this.serviceConfig = getSettings();
//        String confFile = readStream(getClass().getResourceAsStream(configFilePath));

        this.serviceConfig = new Configurator(objectMapper).getConfiguration();

        LOG.debug(serviceConfig.toString());
        // Update Host setting in config if NULL
        serviceConfig.setHost(StringUtils.isEmpty(serviceConfig.getHost())
            ? InetAddress.getLocalHost().getHostAddress()
            : serviceConfig.getHost());

        this.RESTAPI_HOST = new URL(serviceConfig.getProtocol(), serviceConfig.getHost(), serviceConfig.getPort(), "/");

        this.RESTAPI_ENDPOINT = new URL(RESTAPI_HOST, serviceConfig.getContextUri());

        if (! setupWorkDirectory()) {
            System.exit(ENOENT);
        }
        if (! checkDBConnection()) {
            System.exit(EHOSTDOWN);
        }
    }

    private boolean setupWorkDirectory() {
        File f = new File(serviceConfig.getDocRoot());
        if (!f.exists()) {
            if (f.mkdirs()){
                LOG.info("Made DocRoot directory '{}'", f.getPath());
                return true;
            }
            else {
                throw new RuntimeException("Make DocRoot directory failed: " + f.getPath());
            }
        } else {
            LOG.info("DocRoot directory exists: '{}'", f.getPath());
            return true;
        }
    }

    private boolean checkDBConnection() {
        try {
            if (new ProxyUrlDAO(serviceConfig.getNeo4jConf(), objectMapper).isAlive()) {
                return true;
            }
            else {
                throw new RuntimeException("Unable to write to database. Check connection settings.");
            }
        }
        catch (ServiceUnavailableException sune) {
            throw new RuntimeException(sune.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        MainApp app = new MainApp(args);
        app.boot();
        app.writeConnectionString();
    }

    public void boot() throws Exception {
        final JndiContext jndiContext = new JndiContext();
        final AuthAgent authAgent = new AuthAgent(
            serviceConfig.getRemoteAuthenticationUrl(), serviceConfig.getBasicAuthHeader(), this.objectMapper
        );

        jndiContext.bind("authAgent", authAgent);

        CamelContext camelContext = new DefaultCamelContext(jndiContext);
//        camelContext.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm://localhost?broker.persistent=false"));
        ZRouteBuilder zroute = new ZRouteBuilder(objectMapper, serviceConfig, authAgent);
        camelContext.addRoutes(zroute);
        camelContext.start();


//        String gatewayUri = getHttp4Proto(RESTAPI_ENDPOINT + ZRouteBuilder.LIST_BUCKETS_URI);

        Endpoint gatewayEndpoint = zroute.endpoint("direct:rest.list_buckets");

        Producer producer = gatewayEndpoint.createProducer();
//        ProducerTemplate producer = gatewayEndpoint.getCamelContext().createProducerTemplate();

        LOG.info("Sending order");
        Exchange exchange = gatewayEndpoint.createExchange(ExchangePattern.OutOnly);
//        exchange.getIn().setHeader(Constants.AUTHORIZATION, authAgent.getAuthResponse().getAuthorizationToken());
//        exchange.getIn().setHeader(Exchange.HTTP_METHOD, HttpMethods.POST);
        producer.process(exchange);

        Message out = exchange.getOut();


        BucketListResponse buckets = JsonHelper.coerceClass(objectMapper, exchange.getOut(), BucketListResponse.class);

        MirrorMap<String, String> bucketIdNameMap = new MirrorMap<String, String>(
            buckets.getBuckets().stream().collect(Collectors.toMap(B2Bucket::getBucketId, B2Bucket::getBucketName))
        );

        zroute.setBucketMap(bucketIdNameMap);
    }

//    private String readStream(InputStream stream) throws IOException {
//        ByteArrayOutputStream into = new ByteArrayOutputStream();
//        byte[] buf = new byte[4096];
//
//        for (int n = -1; 0 < (n = stream.read(buf)); into.write(buf, 0, n)){}
//        into.close();
//        return into.toString();
//    }

    private void writeConnectionString() throws Exception {
        LOG.info("Listening on: " +
            new URL("http", serviceConfig.getHost(), serviceConfig.getPort(), serviceConfig.getContextUri())
        );
    }

    public boolean isExpired() {
        // TODO Auto-generated method stub
        return false;
    }

    private boolean noToken(String authorizationToken) {
        return
            StringUtils.isBlank(authorizationToken) || (utcInSecs() - lastmod) >= TTL;
    }

    private long utcInSecs() {
        return new Date().getTime() / 1000;
    }
}

