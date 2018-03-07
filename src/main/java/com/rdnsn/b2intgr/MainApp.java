package com.rdnsn.b2intgr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdnsn.b2intgr.dao.ProxyUrlDAO;
import com.rdnsn.b2intgr.processor.AuthAgent;
import com.rdnsn.b2intgr.route.ZRouteBuilder;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jndi.JndiContext;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;

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


/**
 * Initialize and start the camel routes
 */
public class MainApp {

    private static final String ENV_PREFIX = "B2I_";
    private static final String CONFIG_ENV_PATTERN = "\\$([\\w\\_\\-\\.]+)";
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

    public MainApp(String[] args) throws IOException {
        this.objectMapper = new ObjectMapper();
        this.serviceConfig = getSettings();

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
        camelContext.addRoutes(new ZRouteBuilder(objectMapper, serviceConfig, authAgent));
        camelContext.start();
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

        // Override with specially prefixed environment variables
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

    private void writeConnectionString() throws Exception {
        System.err.println("Listening on: " +
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

