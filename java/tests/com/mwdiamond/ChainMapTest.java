package com.mwdiamond;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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
    private Map<String, Integer> initialContents;
    
    @BeforeMethod
    protected void createMaps() {
        map1 = new HashMap<>(
          ImmutableMap.of("a", 1, "b", 1, "c", 1));
        map2 = new HashMap<>(
          ImmutableMap.of("b", 2, "c", 2, "d", 2));
        map3 = new HashMap<>(
          ImmutableMap.of("c", 3, "d", 3, "e", 3));
        
        chainMap = new ChainMap<>(ImmutableList.of(map1, map2, map3));
        initialContents = ImmutableMap.of("a", 1, "b", 1, "c", 1, "d", 2, "e", 3);
    }
    
    @Test
    public void equality() {
        equality(new ChainMap<>(ImmutableList.of()), ImmutableMap.of());
        equality(chainMap, initialContents);
    }
    
    @Test
    public void immutableChainView() {
        equality(ChainMap.immutableChainView(ImmutableList.of(map1, map2, map3)), initialContents);
    }
    
    private static <K, V> void equality(Map<K, V> actualChain, Map<K, V> expected) {
        assertEquals(actualChain, expected);
        assertEquals(expected, actualChain);
        assertEquals(actualChain.hashCode(), expected.hashCode());
        
        assertEquals(actualChain.isEmpty(), expected.isEmpty());
        assertEquals(actualChain.size(), expected.size());

        assertEquals(actualChain.keySet(), expected.keySet());
        assertEquals(actualChain.values(), expected.values());
        assertEquals(actualChain.entrySet(), expected.entrySet());
        
        // Verify EntrySet matches .get(), .containsKey(), .containsValue()
        Set<Entry<K, V>> set = actualChain.entrySet();
        assertEquals(set.size(), actualChain.size());
        for(Entry<K, V> e : set) {
          assertTrue(actualChain.containsKey(e.getKey()));
          assertEquals(actualChain.get(e.getKey()), e.getValue());
          assertTrue(actualChain.containsValue(e.getValue()));
        }
    }
    
    @Test
    public void hiddenValue() {
        chainMap.remove("d");
        chainMap.remove("e");
        assertEquals(chainMap, ImmutableMap.of("a", 1, "b", 1, "c", 1));
        assertTrue(chainMap.containsValue(1));
        assertFalse(chainMap.containsValue(2));
        assertFalse(chainMap.containsValue(3));
        assertFalse(chainMap.containsValue(4));
    }
    
    @Test
    public void mutations() {
        // Remove single element
        assertEquals(chainMap.remove("a"), (Integer)1); // Eclipse needs this cast, javac works without it
        assertEquals(chainMap, ImmutableMap.of("b", 1, "c", 1, "d", 2, "e", 3));

        // Remove duplicated element
        assertEquals(chainMap.remove("c"), (Integer)1);
        assertEquals(chainMap, ImmutableMap.of("b", 1, "d", 2, "e", 3));

        // Overwrite existing element
        assertEquals(chainMap.put("b", -1), (Integer)1);
        assertEquals(chainMap, ImmutableMap.of("b", -1, "d", 2, "e", 3));
        assertEquals(map1, ImmutableMap.of("b", 1));
        assertTrue(chainMap.containsKey("b"));
        assertEquals(chainMap.get("b"), (Integer)(-1));
        
        // Add new element
        assertEquals(chainMap.put("f", -1), null);
        assertEquals(chainMap, ImmutableMap.of("b", -1, "d", 2, "e", 3, "f", -1));
        assertTrue(chainMap.containsKey("f"));
        assertEquals(chainMap.get("f"), (Integer)(-1));
        
        // Add null element
        assertFalse(chainMap.containsKey(null));
        assertFalse(chainMap.containsValue(null));
        assertEquals(chainMap.put(null, null), null);
        assertTrue(chainMap.containsKey(null));
        assertTrue(chainMap.containsValue(null));
    }
    
    // TODO more tests, particularly mutations and type safety
}
