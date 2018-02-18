package com.rdnsn.b2intgr;

import java.io.*;
import java.net.InetAddress;
import java.util.Date;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.util.jndi.JndiContext;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdnsn.b2intgr.api.AuthResponse;
import com.rdnsn.b2intgr.processor.AuthAgent;
import com.rdnsn.b2intgr.route.ZRouteBuilder;


/**
 * A Camel Application
 */
public class MainApp {

    private final ObjectMapper objectMapper;
    private final CloudFSConfiguration serviceConfig;

    private static long lastmod = 0;
    //	final private static final long TTL = 10;
    final private static long TTL = (12 * 60 * 60) - 10;


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


    public MainApp(String[] args) throws IOException {
        this.objectMapper = new ObjectMapper();
        this.serviceConfig =  objectMapper.readValue(
                getClass().getResourceAsStream("/config.json"), CloudFSConfiguration.class);
//        this.serviceConfig = objectMapper.readValue(new FileInputStream(configFile), CloudFSConfiguration.class);
    }

    public static void main(String[] args) throws Exception {
        MainApp app = new MainApp(args);
        app.boot();
        app.writeConnectionString();
    }

    private void writeConnectionString() throws Exception {
        String buf = String.format("http://%s:%d%s", InetAddress.getLocalHost().getHostAddress(), serviceConfig.getPort(), serviceConfig.getContextUri());
        FileOutputStream fo = new FileOutputStream(new File(serviceConfig.getDocRoot(), "queue_url.txt"));
        System.err.println("Url: " + buf);
        fo.write(buf.getBytes());
        fo.close();
    }

    public void boot() throws Exception {

        final JndiContext jndiContext = new JndiContext();

        final AuthAgent authAgent = new AuthAgent(
            serviceConfig.getRemoteAuthenticationUrl(), serviceConfig.getBasicAuthHeader(), this.objectMapper
        );

        jndiContext.bind("authAgent", authAgent);
        jndiContext.bind("makeRes", (AggregationStrategy)(Exchange original, Exchange resource) -> {
            if (resource != null) {  original.getOut().copyFrom(resource.getIn()); }
            return original;
        });

        CamelContext camelContext = new DefaultCamelContext(jndiContext);
        camelContext.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm://localhost?broker.persistent=false"));
        camelContext.addRoutes(new ZRouteBuilder(objectMapper, serviceConfig, authAgent));
        camelContext.start();
    }
}

