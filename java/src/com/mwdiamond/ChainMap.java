package com.mwdiamond;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChainMap<K, V> extends ForwardingMap<K, V> {
  @SuppressWarnings("unchecked")
  private final V NULL_SENTINAL = (V)new Object();

  private final HashMap<K, V> mutable = new HashMap<>();
  private final List<Map<K, V>> chain;

  @SuppressWarnings("unchecked")
  public ChainMap(Iterable<? extends Map<? extends K, ? extends V>> maps) {
    chain = new ImmutableList.Builder<Map<K, V>>()
        .add(mutable)
        .addAll((Iterable<Map<K, V>>) maps)
        .build();
  }

  @Override
  protected Map<K, V> delegate() {
    return mutable;
  }

  public Map<K, V> headMap() {
    return Collections.unmodifiableMap(mutable);
  }

  private <O> O doLazy(Function<Map<K, V>, Optional<O>> function, O fallback) {
    for(Map<K, V> map : chain) {
      Optional<O> result = function.apply(map);
      if(result.isPresent()) {
        return result.get();
      }
    }
    return fallback;
  }

  @Override
  public int size() {
    int size = 0;
    for(Map<K, V> map : chain) {
      size += map.size();
    }
    return size;
  }

  @Override
  public boolean isEmpty() {
    return doLazy(new Function<Map<K, V>, Optional<Boolean>>() {
      @Override
      public Optional<Boolean> apply(Map<K, V> map) {
        return map.isEmpty() ? Optional.<Boolean>absent() : Optional.of(false);
      }
    }, true);
  }

  @Override
  public boolean containsKey(final Object key) {
    return doLazy(new Function<Map<K, V>, Optional<Boolean>>() {
      @Override
      public Optional<Boolean> apply(Map<K, V> map) {
        return map.containsKey(key) ? Optional.of(true) : Optional.<Boolean>absent();
      }
    }, false);
  }

  @Override
  public boolean containsValue(final Object value) {
    return doLazy(new Function<Map<K, V>, Optional<Boolean>>() {
      @Override
      public Optional<Boolean> apply(Map<K, V> map) {
        return map.containsValue(value) ? Optional.of(true) : Optional.<Boolean>absent();
      }
    }, false);
  }

  @Override
  public V get(final Object key) {
    V value = doLazy(new Function<Map<K, V>, Optional<V>>() {
      @Override
      public Optional<V> apply(Map<K, V> map) {
        if(map.containsKey(key)) {
          V value = map.get(key);
          return value == null ? Optional.of(NULL_SENTINAL) : Optional.of(value);
        }
        return Optional.absent();
      }
    }, null);
    return NULL_SENTINAL.equals(value) ? null : value;
  }

  @Override
  public V remove(final Object key) {
    V value = doLazy(new Function<Map<K, V>, Optional<V>>() {
      @Override
      public Optional<V> apply(Map<K, V> map) {
        if(map.containsKey(key)) {
          V value = map.remove(key);
          return value == null ? Optional.of(NULL_SENTINAL) : Optional.of(value);
        }
        return Optional.absent();
      }
    }, null);
    return NULL_SENTINAL.equals(value) ? null : value;
  }

  @Override
  public void clear() {
    for(Map<K, V> map : chain) {
      map.clear();
    }
  }

  @Override
  public Set<K> keySet() {
    Set<K> unionSet = ImmutableSet.of();
    for(Map<K, V> map : chain) {
      unionSet = Sets.union(map.keySet(), unionSet);
    }
    return unionSet;
  }

  @Override
  public Collection<V> values() {
    return new AbstractCollection<V>() {
      @Override
      public Iterator<V> iterator() {
        return Iterators.concat(FluentIterable.from(chain).transform(
            new Function<Map<K, V>, Iterator<V>>(){
          @Override
          public Iterator<V> apply(Map<K, V> map) {
            return map.values().iterator();
          }}).iterator());
      }

      @Override
      public int size() {
        int size = 0;
        for(Map<K, V> map : chain) {
          size += map.values().size();
        }
        return size;
      }};
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    Set<Map.Entry<K, V>> unionSet = ImmutableSet.of();
    for(Map<K, V> map : chain) {
      unionSet = Sets.union(map.entrySet(), unionSet);
    }
    return unionSet;
  }
}
