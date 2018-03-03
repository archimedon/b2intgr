package com.rdnsn.b2intgr;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.rdnsn.b2intgr.dao.ProxyUrlDAO;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.util.jndi.JndiContext;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
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

    private static final String ENV_PREFIX = "B2I_";
    private static final String CONFIG_ENV_PATTERN = "\\$([\\w\\_\\-\\.]+)";
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
        this.serviceConfig = getSettings();

        if (! new ProxyUrlDAO(serviceConfig.getNeo4jConf(), objectMapper).isAlive()) {
            System.err.println("No connection to Neo4J");
            System.exit(1);
        }
        File f = new File(serviceConfig.getDocRoot());
        if (!f.exists()) {
            System.err.println((f.mkdirs() ? "Made DocRoot directory " : "Make DocRoot directory failed: ") + f.getPath());
        } else {
            System.err.println("DocRoot directory exists: " + f.getPath());
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

        String confFile = readStream(getClass().getResourceAsStream("/config.json"));
        confFile = injectExtern(confFile);

        CloudFSConfiguration confObject = objectMapper.readValue(confFile, CloudFSConfiguration.class);

        doEnvironmentOverrides(confObject, confFile);

        return confObject;


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

    private void doEnvironmentOverrides(CloudFSConfiguration confObject, String confFile) throws IOException {

        Map<String, Object> map = objectMapper.readValue(confFile, HashMap.class);

        crawl(map).forEach( propName -> {
            String ev = null;

            if ( (ev = System.getenv(ENV_PREFIX + propName )) != null) {
                try {
                    if (propName.indexOf('.') > 0) {
                        PropertyUtils.setNestedProperty(confObject, propName, ev);
                    }
                    else {
                        BeanUtils.setProperty(confObject, propName, ev);
                    }
                    System.err.format("Override config['%s'] with env['%s']%n", propName , ENV_PREFIX + propName);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        });
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

    public static void main(String[] args) throws Exception {
        MainApp app = new MainApp(args);
        app.boot();
        app.writeConnectionString();
    }

    private void writeConnectionString() throws Exception {
        String buf = String.format("http://%s:%d%s", InetAddress.getLocalHost().getHostAddress(), serviceConfig.getPort(), serviceConfig.getContextUri());
        System.err.println("Listening on: " + buf);
    }

    public void boot() throws Exception {

        final JndiContext jndiContext = new JndiContext();

        final AuthAgent authAgent = new AuthAgent(
            serviceConfig.getRemoteAuthenticationUrl(), serviceConfig.getBasicAuthHeader(), this.objectMapper
        );

        jndiContext.bind("authAgent", authAgent);
//        jndiContext.bind("makeRes", (AggregationStrategy)(Exchange original, Exchange resource) -> {
//            if (resource != null) {  original.getOut().copyFrom(resource.getIn()); }
//            return original;
//        });

        CamelContext camelContext = new DefaultCamelContext(jndiContext);
//        camelContext.addComponent("activemq", ActiveMQComponent.activeMQComponent("vm://localhost?broker.persistent=false"));
        camelContext.addRoutes(new ZRouteBuilder(objectMapper, serviceConfig, authAgent));
        camelContext.start();
    }
}

