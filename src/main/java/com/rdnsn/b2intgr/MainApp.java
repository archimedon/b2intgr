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
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    private static final long TTL = (12 * 60 * 60) - 10;

    private static long lastmod = 0;

    private final ObjectMapper objectMapper;
    private final CloudFSConfiguration serviceConfig;
    private final String configFilePath = "/config.json";

    public MainApp(String[] args) throws IOException {

        this.objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        objectMapper.configure(MapperFeature.USE_ANNOTATIONS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        this.serviceConfig = new Configurator(objectMapper).getConfiguration();

        log.debug(serviceConfig.toString());
        // Update Host setting in config if NULL
        serviceConfig.setHost(StringUtils.isEmpty(serviceConfig.getHost())
            ? InetAddress.getLocalHost().getHostAddress()
            : serviceConfig.getHost());


        if (! setupWorkDirectory()) {
            System.exit(Constants.ENOENT);
        }
        if (! checkDBConnection()) {
            System.exit(Constants.EHOSTDOWN);
        }
    }

    private boolean setupWorkDirectory() {
        File f = new File(serviceConfig.getDocRoot());
        if (!f.exists()) {
            if (f.mkdirs()){
                log.info("Made DocRoot directory '{}'", f.getPath());
                return true;
            }
            else {
                throw new RuntimeException("Make DocRoot directory failed: " + f.getPath());
            }
        } else {
            log.info("DocRoot directory exists: '{}'", f.getPath());
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
        ZRouteBuilder zRouteBuilder = new ZRouteBuilder(objectMapper, serviceConfig, authAgent);
        camelContext.addRoutes(zRouteBuilder);
        camelContext.start();
        lookupAvailableBuckets(zRouteBuilder);
    }

    private void lookupAvailableBuckets(ZRouteBuilder zRouteBuilder) throws Exception {

        Endpoint gatewayEndpoint = zRouteBuilder.endpoint("direct:rest.list_buckets");
        Producer producer = gatewayEndpoint.createProducer();

        log.info("Populating available buckets ...");
        Exchange exchange = gatewayEndpoint.createExchange(ExchangePattern.OutOnly);
        producer.process(exchange);

        BucketListResponse buckets = JsonHelper.coerceClass(objectMapper, exchange.getOut(), BucketListResponse.class);

        MirrorMap<String, String> bucketIdNameMap = new MirrorMap<String, String>(
                buckets.getBuckets().stream().collect(Collectors.toMap(B2Bucket::getBucketId, B2Bucket::getBucketName))
        );

        zRouteBuilder.setBucketMap(bucketIdNameMap);
    }

    private void writeConnectionString() throws Exception {
        log.info("Listening on: " +
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

