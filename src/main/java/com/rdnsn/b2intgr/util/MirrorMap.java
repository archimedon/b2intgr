package com.rdnsn.b2intgr.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MirrorMap<K, V> {

    private Map<K, V> backing;

    public MirrorMap(Map<K, V> backing) {
        super();
        this.backing = backing;
        new HashSet<V>(backing.values());
    }

    public Object getRef(String keyOrVal) {
        return (backing.containsKey(keyOrVal))
            ? backing.get(keyOrVal)
            : backing.values().contains(keyOrVal)
                ? backing.entrySet().stream().filter(entry -> entry.getValue().equals(keyOrVal)).findFirst().get().getKey()
                : null;
    }

    public V getObject(String keyOrVal) {
        return (backing.containsKey(keyOrVal))
            ? backing.get(keyOrVal)
            : null;
    }

    public K getSubject(String keyOrVal) {
        return backing.values().contains(keyOrVal)
                ? backing.entrySet().stream().filter(entry -> entry.getValue().equals(keyOrVal)).findFirst().get().getKey()
                : null;
    }
}
