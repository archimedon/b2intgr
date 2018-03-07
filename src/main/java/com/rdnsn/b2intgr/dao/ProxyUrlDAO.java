package com.rdnsn.b2intgr.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdnsn.b2intgr.Neo4JConfiguration;
import com.rdnsn.b2intgr.model.ProxyUrl;
import org.neo4j.driver.v1.*;

import java.io.IOException;
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
    private Driver driver;
    private final ObjectMapper objectMapper;

    public ProxyUrlDAO(Neo4JConfiguration conf, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        driver = GraphDatabase.driver(conf.getUrlString(), AuthTokens.basic(conf.getUsername(), conf.getPassword()));
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
            stat = ( re.hasNext() ) ? re.single().get(0).asBoolean(): false ;
            session.run("MATCH (b:BBIntgrTest) DELETE b;");
            session.close();
        }
        catch(Exception e) {
            stat = false;
            e.printStackTrace();
        }
        finally {
            close();
        }
        return stat;
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

                    System.err.format("found: %s%n", found);

                }

                return found;
            });
            return resData;
        }
    }

    synchronized private Session getSession() {
        return driver.session();
    }
}
