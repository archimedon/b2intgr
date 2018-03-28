package com.rdnsn.b2intgr.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdnsn.b2intgr.Neo4JConfiguration;
import com.rdnsn.b2intgr.model.ProxyUrl;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.neo4j.driver.v1.Values.parameters;
import static org.neo4j.driver.v1.Values.value;

/**
 * TODO: 3/6/18 Need to replace the entire connection architecture... it's innefficient but does not slow any processes
 *
 * Represents a proxy URL.
 *
 */
public class ProxyUrlDAO implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyUrlDAO.class);

    private Driver driver;
    private final ObjectMapper objectMapper;

    public ProxyUrlDAO(Neo4JConfiguration conf, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        try {
            driver = GraphDatabase.driver(conf.getUrlString(), AuthTokens.basic(conf.getUsername(), conf.getPassword()));
        } catch (ServiceUnavailableException sue) {
            throw new RuntimeException("Unable to connect to: " + conf.getUrlString());
        }
    }

    @Override
    public void close() {
        driver.close();
    }

    public boolean isAlive() {
        boolean stat = false;
        try {
            Session session = getSession();
            StatementResult re = session.run("CREATE (b:BBIntgrTest {stat:true}) return b.stat;");
            stat = (re.hasNext()) ? re.single().get(0).asBoolean() : false;
            session.run("MATCH (b:BBIntgrTest) DELETE b;");
            session.close();
        } catch (Exception e) {
            stat = false;
            LOG.error(e.getMessage());
        } finally {
            if (driver != null) {
                close();
            }
            return stat;
        }
    }

    public Object saveOrUpdateMapping(final ProxyUrl message) {

        String findCypher = message.getSha1() == null
                ? String.format("MATCH (p:ProxyUrl) WHERE p.proxy = \"%s\" RETURN id(p)", message.getProxy())
                : String.format("MATCH (p:ProxyUrl) WHERE p.sha1 = \"%s\" RETURN id(p)", message.getSha1());

        try (Session session = getSession()) {
            Object resData = session.writeTransaction((Transaction tx) ->
            {
                Long idResult = null;

                StatementResult result = tx.run(findCypher);
                if (result.hasNext()) {

                    Record res = result.single();
                    idResult = res.size() > 0 ? res.get(0).asLong() : null;

                    try {

                        // TODO: 3/1/18 - this is a shortcut allowing me to add properties without having to update the input
                        Map<String, Object> valMap = objectMapper.readValue(message.toString(), HashMap.class);

                        String updateCypher = String.format("MATCH (p:ProxyUrl) WHERE id(p) = %d", idResult) +
                                valMap.entrySet().stream()
                                        .filter(ent -> ent.getValue() != null)
                                        .map(ent -> String.format(" SET p.%s = $%s", ent.getKey(), ent.getKey()))
                                        .collect(Collectors.joining()) +
                                " RETURN id(p)";


                        LOG.debug("valMap: {}", valMap);

                        result = tx.run(
                                updateCypher,
                                // TODO: 3/1/18 - this is a shortcut allowing me to add properties without having to update the input
                                value(valMap)
                        );
                        idResult = result.single().get(0).asLong();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    String createCypher = String.format("CREATE (p:ProxyUrl %s) RETURN id(p)", message.toCypherJson());

                    idResult = tx.run(createCypher).single().get(0).asLong();
                }
                return idResult;
            });
            return resData;
        }
    }

    public String getActual(final ProxyUrl message) {

        try (Session session = getSession()) {
            String resData = session.writeTransaction((Transaction tx) ->
            {


                StatementResult result = tx.run("MATCH (p:ProxyUrl) WHERE p.proxy = $purl RETURN p.actual",
                        parameters("purl", message.getProxy()));

                String found = null;
                if (result.hasNext()) {

                    Record res = result.single();
                    found = res.size() > 0 ? res.get(0).asString() : null;

                    LOG.debug("found: '{}'", found);

                }

                return found;
            });
            return resData;
        }
    }


    public Integer deleteMapping(final ProxyUrl proxyUrl) {
        try (Session session = getSession()) {
            Integer resData = session.writeTransaction((Transaction tx) -> {

                LOG.debug("proxyUrl.getProxy(): {} ", proxyUrl.getProxy());
                LOG.debug("proxyUrl.getFileId(): {} ", proxyUrl.getFileId());

                StatementResult result = tx.run("MATCH (p:ProxyUrl) WHERE " +
                                "p.proxy = $purl AND " +
                                "p.fileId = $fileId " +
                                "DELETE p",
                        parameters(
                        "purl", proxyUrl.getProxy(),
                            "fileId", proxyUrl.getFileId()
                        ));

                Integer found = null;
                if (result.hasNext()) {

                    Record res = result.single();
                    found = res.size() > 0 ? res.get(0).asInt() : null;

                    LOG.debug("found: '{}'", found);

                }
                return found;
            });
            return resData;
        }


    }

    public ProxyUrl getProxyUrl(final ProxyUrl proxyUrl) {

        ProxyUrl found = new ProxyUrl();

        try (Session session = getSession()) {
            ProxyUrl resData = session.writeTransaction((Transaction tx) ->
            {
                StatementResult result = tx.run("MATCH (p:ProxyUrl) WHERE p.proxy = $purl RETURN properties(p)",
                        parameters("purl", proxyUrl.getProxy()));

                if (result.hasNext()) {

                    Record res = result.single();
                    if (res.size() > 0) {
                        try {
                            HashMap map = new HashMap(res.get(0).asMap() );
                            copyProperties(found, map);
                            LOG.debug("found: '{}'", found);
                        } catch (Exception e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }

                return found;
            });
            return resData;
        }
    }

    private void copyProperties(ProxyUrl found, Map<String, Object> data) {


        Class<?> clazz = ProxyUrl.class;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            try {
                Field field = clazz.getDeclaredField(entry.getKey()); //get the field by name
                if (field != null) {
                    field.setAccessible(true); // for private fields
                    field.set(found, entry.getValue()); // set the field's value for your object
                }
            } catch (NoSuchFieldException | SecurityException e) {
                e.printStackTrace();
                // handle
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                // handle
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                // handle
            }
        }
    }

    synchronized private Session getSession() {
        return driver.session();
    }

}
