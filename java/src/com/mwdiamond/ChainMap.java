package com.mwdiamond;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ChainMap<K, V> implements Map<K, V> {
  private final HashMap<K, V> innerMap = new HashMap<>();
  private final List<Map<K, V>> chain;

  @SuppressWarnings("unchecked")
  public ChainMap(Iterable<? extends Map<? extends K, ? extends V>> maps) {
    chain = new ImmutableList.Builder<Map<K, V>>()
        .add(innerMap)
        // this is a safe cast because nothing in ChainMap allows you to insert
        // elements into any maps other than the first, innerMap.
        .addAll((Iterable<? extends Map<K, V>>)maps)
        .build();
  }

  public Map<K, V> innerMap() {
    return Collections.unmodifiableMap(innerMap);
  }
  
  public List<Map<K, V>> maps() {
      return chain.stream().map(m -> Collections.unmodifiableMap(m)).collect(Collectors.toList());
  }

  @Override
  public int size() {
    return keySet().size();
  }

  @Override
  public boolean isEmpty() {
    return !chain.stream().filter(map -> !map.isEmpty()).findFirst().isPresent();
  }

  @Override
  public boolean containsKey(Object key) {
    return chain.stream().filter(map -> map.containsKey(key)).findFirst().isPresent();
  }

  @Override
  public boolean containsValue(Object value) {
    return entrySet().stream().filter(e -> value == e.getValue() || (value != null && value.equals(e.getValue()))).findFirst().isPresent();
  }

  @Override
  public V get(Object key) {
    return chain.stream().filter(map -> map.containsKey(key)).findFirst().map(map -> map.get(key)).orElse(null);
  }
  
  @Override
  public V put(K key, V value) {
      V oldValue = get(key);
      innerMap.put(key, value);
      return oldValue;
  }
  
  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
      innerMap.putAll(map);
  }

  @Override
  public V remove(Object key) {
    V removed = chain.stream().filter(map -> map.containsKey(key)).findFirst().map(map -> map.remove(key)).orElse(null);
    // Map.remove() notes:
    // "The map will not contain a mapping for the specified key once the call returns."
    chain.stream().forEach(map -> map.remove(key));
    return removed;
  }

  @Override
  public void clear() {
    chain.forEach(Map::clear);
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return (chain.size() == 1 ? chain.get(0) : Maps.asMap(keySet(), key -> get(key))).entrySet();
  }

  @Override
  public Set<K> keySet() {
    return chain.stream().map(Map::keySet).reduce(ImmutableSet.of(), (a, b) -> Sets.union(a, b));
  }

  @Override
  public Collection<V> values() {
    Set<Map.Entry<K, V>> entries = entrySet();
    return new AbstractCollection<V>() {
      @Override
      public Iterator<V> iterator() {
        return entries.stream().map(e -> e.getValue()).iterator();
      }

      @Override
      public int size() {
        return entries.size();
      }};
  }
  
  @Override
  public boolean equals(Object obj) {
      if (obj instanceof Map) {
          return entrySet().equals(((Map<?,?>)obj).entrySet());
      }
      return false;
  }
  
  @Override
  public int hashCode() {
      return entrySet().hashCode();
  }
  
  @Override
  public String toString() {
      String entryString = entrySet().toString();
      return "{" + entryString.substring(1, entryString.length()-1) + "}";
  }
  
  public static <K, V> Map<K, V> immutableChainView(Iterable<? extends Map<? extends K, ? extends V>> maps) {
      return StreamSupport.stream(maps.spliterator(), false).reduce((Map<K,V>)ImmutableMap.<K,V>of(),
        (a, b) -> Maps.asMap(Sets.union(a.keySet(), b.keySet()), k -> a.containsKey(k) ? a.get(k) : b.get(k)),
        (a, b) -> Maps.asMap(Sets.union(a.keySet(), b.keySet()), k -> a.containsKey(k) ? a.get(k) : b.get(k)));
    }
}