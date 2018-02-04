package com.rdnsn.b2intgr;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
 * Main Application
 */
public class MainApp {

    private final ObjectMapper objectMapper;
    private final CloudFSConfiguration serviceConfig;


    public MainApp(String configFile) throws IOException {
        this.objectMapper = new ObjectMapper();
        this.serviceConfig = objectMapper.readValue(new FileInputStream(configFile), CloudFSConfiguration.class);
    }

    public static void main(String[] args) throws Exception {
        MainApp app = new MainApp("config.json");
        app.boot();
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

