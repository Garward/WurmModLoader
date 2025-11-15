package com.garward.wurmmodloader.api.registry;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Unit tests for RegisterEvent and RegistryEvent.
 */
public class RegisterEventTest {

    private Registry<String> registry;
    private RegisterEvent<String> event;
    private ResourceLocation key1;
    private ResourceLocation key2;

    @Before
    public void setUp() {
        registry = new TestRegistry<>("test");
        event = new RegisterEvent<>(registry);
        key1 = new ResourceLocation("mymod", "item1");
        key2 = new ResourceLocation("mymod", "item2");
    }

    @Test
    public void testConstructor() {
        assertNotNull(event);
        assertEquals(registry, event.getRegistry());
        assertEquals("test", event.getRegistryName());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullRegistry() {
        new RegisterEvent<>(null);
    }

    @Test
    public void testGetRegistry() {
        Registry<String> result = event.getRegistry();
        assertSame(registry, result);
    }

    @Test
    public void testGetRegistryName() {
        assertEquals("test", event.getRegistryName());
    }

    @Test
    public void testRegister() {
        event.register(key1, "value1");

        // Verify it was registered in the underlying registry
        assertTrue(registry.containsKey(key1));
        assertEquals("value1", registry.get(key1).get());
    }

    @Test
    public void testRegisterMultiple() {
        event.register(key1, "value1");
        event.register(key2, "value2");

        assertEquals(2, registry.size());
        assertEquals("value1", registry.get(key1).get());
        assertEquals("value2", registry.get(key2).get());
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterNullKey() {
        event.register(null, "value");
    }

    @Test(expected = NullPointerException.class)
    public void testRegisterNullValue() {
        event.register(key1, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterToFrozenRegistry() {
        registry.freeze();
        event.register(key1, "value1");
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterDuplicate() {
        event.register(key1, "value1");
        event.register(key1, "value2"); // Should throw
    }

    @Test
    public void testTypeSafety() {
        // Compile-time type safety test
        Registry<String> stringRegistry = new TestRegistry<>("strings");
        RegisterEvent<String> stringEvent = new RegisterEvent<>(stringRegistry);

        // This should compile
        stringEvent.register(key1, "string_value");

        // This should NOT compile (caught at compile time)
        // stringEvent.register(key1, 123); // Would not compile

        assertEquals("string_value", stringRegistry.get(key1).get());
    }

    @Test
    public void testEventImmutability() {
        // Test that event properties cannot be changed after construction
        String originalName = event.getRegistryName();
        Registry<String> originalRegistry = event.getRegistry();

        // Register something
        event.register(key1, "value");

        // Event properties should remain the same
        assertEquals(originalName, event.getRegistryName());
        assertSame(originalRegistry, event.getRegistry());
    }

    @Test
    public void testRegistryEventBaseClass() {
        // Test RegistryEvent base class functionality
        RegistryEvent baseEvent = event;

        assertEquals("test", baseEvent.getRegistryName());
        assertNotNull(baseEvent.getRegistry());
    }

    @Test
    public void testRegisterEventInheritance() {
        // Verify RegisterEvent properly extends RegistryEvent
        assertTrue(event instanceof RegistryEvent);
        assertTrue(event instanceof RegisterEvent);
    }

    @Test
    public void testMultipleEventsWithSameRegistry() {
        RegisterEvent<String> event1 = new RegisterEvent<>(registry);
        RegisterEvent<String> event2 = new RegisterEvent<>(registry);

        event1.register(key1, "value1");
        event2.register(key2, "value2");

        // Both should affect the same registry
        assertEquals(2, registry.size());
        assertTrue(registry.containsKey(key1));
        assertTrue(registry.containsKey(key2));
    }

    @Test
    public void testDifferentEventTypes() {
        // Test with different generic types
        Registry<Integer> intRegistry = new TestRegistry<>("integers");
        RegisterEvent<Integer> intEvent = new RegisterEvent<>(intRegistry);

        intEvent.register(key1, 42);
        assertEquals(Integer.valueOf(42), intRegistry.get(key1).get());

        // Test with custom objects
        Registry<TestObject> objRegistry = new TestRegistry<>("objects");
        RegisterEvent<TestObject> objEvent = new RegisterEvent<>(objRegistry);

        TestObject obj = new TestObject("test");
        objEvent.register(key1, obj);
        assertSame(obj, objRegistry.get(key1).get());
    }

    // Helper class for testing
    private static class TestObject {
        private final String value;

        public TestObject(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Test implementation of Registry interface
    private static class TestRegistry<T> implements Registry<T> {
        private final String registryName;
        private final Map<ResourceLocation, T> entries = new ConcurrentHashMap<>();
        private final AtomicBoolean frozen = new AtomicBoolean(false);

        public TestRegistry(String registryName) {
            this.registryName = registryName;
        }

        @Override
        public void register(ResourceLocation key, T value) {
            if (frozen.get()) {
                throw new IllegalStateException("Registry is frozen");
            }
            if (entries.putIfAbsent(key, value) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }

        @Override
        public Optional<T> get(ResourceLocation key) {
            return Optional.ofNullable(entries.get(key));
        }

        @Override
        public boolean containsKey(ResourceLocation key) {
            return entries.containsKey(key);
        }

        @Override
        public Set<ResourceLocation> keySet() {
            return entries.keySet();
        }

        @Override
        public Stream<T> values() {
            return entries.values().stream();
        }

        @Override
        public int size() {
            return entries.size();
        }

        @Override
        public boolean isEmpty() {
            return entries.isEmpty();
        }

        @Override
        public String getRegistryName() {
            return registryName;
        }

        @Override
        public void freeze() {
            frozen.set(true);
        }

        @Override
        public boolean isFrozen() {
            return frozen.get();
        }
    }
}
