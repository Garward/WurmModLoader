package com.garward.wurmmodloader.core.registry;

import com.garward.wurmmodloader.api.registry.ResourceLocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Unit tests for IdAllocator.
 */
public class IdAllocatorTest {

    private IdAllocator allocator;
    private Path tempFile;

    @Before
    public void setUp() throws IOException {
        allocator = IdAllocator.getInstance();
        allocator.clear(); // Reset for each test
        tempFile = Files.createTempFile("id_allocator_test", ".json");
    }

    @After
    public void tearDown() throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
        allocator.clear();
    }

    @Test
    public void testSingletonInstance() {
        IdAllocator instance1 = IdAllocator.getInstance();
        IdAllocator instance2 = IdAllocator.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    public void testAllocateBasic() {
        ResourceLocation key = new ResourceLocation("mymod", "item1");
        int id = allocator.allocate(key);

        assertTrue(id >= 30000); // Starting ID
        assertEquals(1, allocator.size());
    }

    @Test
    public void testAllocateSequential() {
        ResourceLocation key1 = new ResourceLocation("mymod", "item1");
        ResourceLocation key2 = new ResourceLocation("mymod", "item2");

        int id1 = allocator.allocate(key1);
        int id2 = allocator.allocate(key2);

        assertEquals(id1 + 1, id2); // Sequential allocation
    }

    @Test
    public void testAllocateIdempotent() {
        ResourceLocation key = new ResourceLocation("mymod", "item1");

        int id1 = allocator.allocate(key);
        int id2 = allocator.allocate(key);

        assertEquals(id1, id2); // Same ID returned
        assertEquals(1, allocator.size()); // Only one entry
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAllocateNullKey() {
        allocator.allocate(null);
    }

    @Test
    public void testGetId() {
        ResourceLocation key = new ResourceLocation("mymod", "item1");
        allocator.allocate(key);

        Optional<Integer> id = allocator.getId(key);
        assertTrue(id.isPresent());
        assertTrue(id.get() >= 30000);
    }

    @Test
    public void testGetIdNonExistent() {
        ResourceLocation key = new ResourceLocation("mymod", "nonexistent");
        Optional<Integer> id = allocator.getId(key);
        assertFalse(id.isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetIdNull() {
        allocator.getId(null);
    }

    @Test
    public void testReserveBasic() {
        ResourceLocation key = new ResourceLocation("mymod", "special_item");
        allocator.reserve(key, 30500);

        Optional<Integer> id = allocator.getId(key);
        assertTrue(id.isPresent());
        assertEquals(30500, (int) id.get());
    }

    @Test
    public void testReserveUpdatesNextId() {
        allocator.reserve(new ResourceLocation("mymod", "item1"), 30500);

        ResourceLocation key2 = new ResourceLocation("mymod", "item2");
        int id2 = allocator.allocate(key2);

        assertTrue(id2 > 30500); // Should be at least 30501
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReserveBelowStartingId() {
        allocator.reserve(new ResourceLocation("mymod", "item"), 29999);
    }

    @Test(expected = IllegalStateException.class)
    public void testReserveConflict() {
        ResourceLocation key1 = new ResourceLocation("mymod", "item1");
        ResourceLocation key2 = new ResourceLocation("mymod", "item2");

        allocator.reserve(key1, 30500);
        allocator.reserve(key2, 30500); // Should throw - ID already taken
    }

    @Test
    public void testReserveSameKeyTwiceWithSameId() {
        ResourceLocation key = new ResourceLocation("mymod", "item");

        allocator.reserve(key, 30500);
        allocator.reserve(key, 30500); // Should not throw - same key, same ID
    }

    @Test(expected = IllegalStateException.class)
    public void testReserveSameKeyWithDifferentId() {
        ResourceLocation key = new ResourceLocation("mymod", "item");

        allocator.reserve(key, 30500);
        allocator.reserve(key, 30501); // Should throw - key already has different ID
    }

    @Test
    public void testSaveAndLoad() throws IOException {
        ResourceLocation key1 = new ResourceLocation("mymod", "item1");
        ResourceLocation key2 = new ResourceLocation("othermod", "item2");

        int id1 = allocator.allocate(key1);
        int id2 = allocator.allocate(key2);

        allocator.save(tempFile);

        // Clear and reload
        allocator.clear();
        assertEquals(0, allocator.size());

        allocator.load(tempFile);

        assertEquals(2, allocator.size());
        assertEquals(id1, allocator.getId(key1).get().intValue());
        assertEquals(id2, allocator.getId(key2).get().intValue());
    }

    @Test
    public void testLoadNonExistentFile() throws IOException {
        Path nonExistent = java.nio.file.Paths.get("/tmp/nonexistent_" + System.currentTimeMillis() + ".json");

        // Should not throw
        allocator.load(nonExistent);
        assertEquals(0, allocator.size());
    }

    @Test
    public void testSaveCreatesParentDirectories() throws IOException {
        Path nestedFile = Files.createTempDirectory("id_test").resolve("nested/dir/ids.json");

        ResourceLocation key = new ResourceLocation("mymod", "item");
        allocator.allocate(key);

        allocator.save(nestedFile);

        assertTrue(Files.exists(nestedFile));
        Files.delete(nestedFile);
        Files.delete(nestedFile.getParent());
        Files.delete(nestedFile.getParent().getParent());
    }

    @Test
    public void testClear() {
        allocator.allocate(new ResourceLocation("mymod", "item1"));
        allocator.allocate(new ResourceLocation("mymod", "item2"));

        assertEquals(2, allocator.size());

        allocator.clear();

        assertEquals(0, allocator.size());

        // Next allocation should start from 30000 again
        int id = allocator.allocate(new ResourceLocation("mymod", "item3"));
        assertEquals(30000, id);
    }

    @Test
    public void testSize() {
        assertEquals(0, allocator.size());

        allocator.allocate(new ResourceLocation("mymod", "item1"));
        assertEquals(1, allocator.size());

        allocator.allocate(new ResourceLocation("mymod", "item2"));
        assertEquals(2, allocator.size());
    }

    @Test
    public void testJsonFormat() throws IOException {
        ResourceLocation key1 = new ResourceLocation("mymod", "magic_sword");
        ResourceLocation key2 = new ResourceLocation("othermod", "shield");

        allocator.allocate(key1);
        allocator.allocate(key2);

        allocator.save(tempFile);

        String json = new String(Files.readAllBytes(tempFile), java.nio.charset.StandardCharsets.UTF_8);

        // Check JSON format
        assertTrue(json.contains("\"mymod:magic_sword\""));
        assertTrue(json.contains("\"othermod:shield\""));
        assertTrue(json.contains("30000"));
        assertTrue(json.contains("30001"));
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }

    @Test
    public void testResourceLocationWithColons() throws IOException {
        // Test that ResourceLocation colons in namespace:path don't break JSON parsing
        ResourceLocation key = new ResourceLocation("my-mod", "magic_sword");

        int id = allocator.allocate(key);
        allocator.save(tempFile);

        allocator.clear();
        allocator.load(tempFile);

        assertEquals(id, allocator.getId(key).get().intValue());
    }

    @Test
    public void testLargeNumberOfAllocations() {
        // Stress test with many allocations
        for (int i = 0; i < 1000; i++) {
            ResourceLocation key = new ResourceLocation("mod", "item" + i);
            int id = allocator.allocate(key);
            assertEquals(30000 + i, id);
        }

        assertEquals(1000, allocator.size());

        // Verify all IDs
        for (int i = 0; i < 1000; i++) {
            ResourceLocation key = new ResourceLocation("mod", "item" + i);
            Optional<Integer> id = allocator.getId(key);
            assertTrue(id.isPresent());
            assertEquals(30000 + i, (int) id.get());
        }
    }
}
