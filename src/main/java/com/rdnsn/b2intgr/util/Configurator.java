package com.rdnsn.b2intgr.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdnsn.b2intgr.CloudFSConfiguration;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Configurator {

    private final Logger LOG = LoggerFactory.getLogger(Configurator.class);

    private final String ENV_PREFIX = "B2I_";
    private final String CONFIG_ENV_PATTERN = "\\$([\\w\\_\\-\\.]+)";
    private final String configFilePath = "/config.json";

    private final String ENV_VAR_PATTERN = "(?i)^B2I_.*";

    private final Map<String, String> ENV_VARS =
            System.getenv().entrySet().stream()
                    .filter(entry ->entry.getKey().matches(ENV_VAR_PATTERN))
                    .collect(Collectors.toMap(
                        entry -> entry.getKey().toUpperCase().replaceAll("(?<!B2I)_", "."),
                        entry -> entry.getValue())
                    );


    private final ObjectMapper objectMapper;


    public Configurator() {
        super();
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        objectMapper.configure(MapperFeature.USE_ANNOTATIONS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    }

    public Configurator(ObjectMapper objectMapper) {
        super();
        this.objectMapper = objectMapper;
    }

    public CloudFSConfiguration getConfiguration() {
        String confFile = readStream(getClass().getResourceAsStream(configFilePath));
        return getConfiguration(confFile);
    }


    @JsonIgnore
    public CloudFSConfiguration getConfiguration(String confFile) {

         // interpolate $vars in config file
        confFile = injectExtern(confFile);

        // Override with specially prefixed environment variables
        return doEnvironmentOverrides(confFile);
    }

    @JsonIgnore
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

    @JsonIgnore
    private CloudFSConfiguration doEnvironmentOverrides(String confFile) {
        CloudFSConfiguration confObject = JsonHelper.coerceClass(objectMapper, confFile, CloudFSConfiguration.class);
        Map<String, Object> propValueMap = JsonHelper.coerceClass(objectMapper, confFile, HashMap.class);
        crawl(propValueMap).forEach( propName -> {
            String ev = null;
            // Replace dot with underscore because linux no like dot
            if ( (ev = ENV_VARS.get(ENV_PREFIX + propName.toUpperCase())) != null) {
                try {
                    if (propName.indexOf('.') > 0) {
                        PropertyUtils.setNestedProperty(confObject, propName, ev);
                    }
                    else {
                        BeanUtils.setProperty(confObject, propName, ev);
                    }
                    LOG.info("Override config['{}'] with env['{}']", propName , ENV_PREFIX + propName);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        });

        return confObject;
    }


    @JsonIgnore
    private String injectExtern(String confFile) {
        Matcher m = Pattern.compile(CONFIG_ENV_PATTERN).matcher(confFile);
        String tmp = null;
        while (m.find()) {
            tmp = System.getenv(m.group(1));
            if (tmp != null && tmp.length() > 0) {
                LOG.info("Setting: '{}' from environment", m.group(1));
                confFile = confFile.replaceAll("\\$" + m.group(1) +"\\b" , tmp);
            }
        }
        return confFile;
    }


    private String readStream(InputStream stream) {
        ByteArrayOutputStream into = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        try {
            for (int n = -1; 0 < (n = stream.read(buf)); into.write(buf, 0, n)) {
            }
            into.close();
        } catch (IOException ioe) {
        }

        return into.toString();
    }

}
