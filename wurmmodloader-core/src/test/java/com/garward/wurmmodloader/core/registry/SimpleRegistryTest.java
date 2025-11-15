package com.garward.wurmmodloader.core.registry;

import com.garward.wurmmodloader.api.registry.ResourceLocation;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Unit tests for SimpleRegistry.
 */
public class SimpleRegistryTest {

    private SimpleRegistry<String> registry;
    private ResourceLocation key1;
    private ResourceLocation key2;

    @Before
    public void setUp() {
        registry = new SimpleRegistry<>("test_registry");
        key1 = new ResourceLocation("mymod", "item1");
        key2 = new ResourceLocation("mymod", "item2");
    }

    @Test
    public void testRegisterAndGet() {
        registry.register(key1, "value1");

        Optional<String> result = registry.get(key1);
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    public void testGetNonExistent() {
        Optional<String> result = registry.get(key1);
        assertFalse(result.isPresent());
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterNullKey() {
        registry.register(null, "value");
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterNullValue() {
        registry.register(key1, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterDuplicate() {
        registry.register(key1, "value1");
        registry.register(key1, "value2"); // Should throw
    }

    @Test
    public void testContainsKey() {
        assertFalse(registry.containsKey(key1));
        registry.register(key1, "value1");
        assertTrue(registry.containsKey(key1));
    }

    @Test
    public void testKeySet() {
        registry.register(key1, "value1");
        registry.register(key2, "value2");

        Set<ResourceLocation> keys = registry.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains(key1));
        assertTrue(keys.contains(key2));
    }

    @Test
    public void testValues() {
        registry.register(key1, "value1");
        registry.register(key2, "value2");

        Set<String> values = registry.values().collect(Collectors.toSet());
        assertEquals(2, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
    }

    @Test
    public void testSize() {
        assertEquals(0, registry.size());
        registry.register(key1, "value1");
        assertEquals(1, registry.size());
        registry.register(key2, "value2");
        assertEquals(2, registry.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(registry.isEmpty());
        registry.register(key1, "value1");
        assertFalse(registry.isEmpty());
    }

    @Test
    public void testGetRegistryName() {
        assertEquals("test_registry", registry.getRegistryName());
    }

    @Test
    public void testFreeze() {
        assertFalse(registry.isFrozen());
        registry.freeze();
        assertTrue(registry.isFrozen());
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterAfterFreeze() {
        registry.freeze();
        registry.register(key1, "value1"); // Should throw
    }

    @Test
    public void testGetAfterFreeze() {
        registry.register(key1, "value1");
        registry.freeze();

        // Reading should still work
        Optional<String> result = registry.get(key1);
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    public void testKeySetIsUnmodifiable() {
        registry.register(key1, "value1");
        Set<ResourceLocation> keys = registry.keySet();

        try {
            keys.add(key2);
            fail("KeySet should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testMultipleValues() {
        // Test that registry can hold many values
        for (int i = 0; i < 100; i++) {
            ResourceLocation key = new ResourceLocation("mod", "item" + i);
            registry.register(key, "value" + i);
        }

        assertEquals(100, registry.size());

        for (int i = 0; i < 100; i++) {
            ResourceLocation key = new ResourceLocation("mod", "item" + i);
            Optional<String> result = registry.get(key);
            assertTrue(result.isPresent());
            assertEquals("value" + i, result.get());
        }
    }

    @Test
    public void testDifferentNamespaces() {
        ResourceLocation key1 = new ResourceLocation("mod1", "item");
        ResourceLocation key2 = new ResourceLocation("mod2", "item");

        registry.register(key1, "value1");
        registry.register(key2, "value2");

        assertEquals("value1", registry.get(key1).get());
        assertEquals("value2", registry.get(key2).get());
    }
}
