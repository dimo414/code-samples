package com.mwdiamond;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ChainMapTest {
    private Map<String, Integer> map1;
    private Map<String, Integer> map2;
    private Map<String, Integer> map3;
    private ChainMap<String, Integer> chainMap;
    
    @BeforeMethod
    protected void createMaps() {
        map1 = new HashMap<>(
          ImmutableMap.of("a", 1, "b", 1, "c", 1));
        map2 = new HashMap<>(
          ImmutableMap.of("b", 2, "c", 2, "d", 2));
        map3 = new HashMap<>(
          ImmutableMap.of("c", 3, "d", 3, "e", 3));
        
        chainMap = new ChainMap<>(ImmutableList.of(map1, map2, map3));
    }
    
    @Test
    public void equality() {
        Map<String, Integer> contents = ImmutableMap.of("a", 1, "b", 1, "c", 1, "d", 2, "e", 3);
        assertEquals(chainMap, contents);
        assertEquals(contents, chainMap);
        assertEquals(chainMap.hashCode(), contents.hashCode());

        assertEquals(chainMap.keySet(), contents.keySet());
        assertEquals(chainMap.values(), contents.values());
        assertEquals(chainMap.entrySet(), contents.entrySet());
        
        assertEntrySet(chainMap);
    }
    
    // TODO more tests, particularly mutations and type safety
    
    private static <K, V> void assertEntrySet(Map<K, V> map) {
      Set<Entry<K, V>> set = map.entrySet();
      assertEquals(set.size(), map.size());
      for(Entry<K, V> e : set) {
        V value = map.get(e.getKey());
        assertEquals(e.getValue(), value);
      }
    }
}
