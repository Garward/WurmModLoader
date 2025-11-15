package com.garward.wurmmodloader.modsupport;

import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.core.registry.IdAllocator;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Unit tests for ItemTemplateBuilder modernization with registry integration.
 *
 * These tests focus on verifying:
 * - New ResourceLocation-based constructor
 * - Deprecated string-based constructor (backward compatibility)
 * - ID allocation through IdAllocator
 * - Field initialization
 */
public class ItemTemplateBuilderTest {

    @Before
    public void setUp() {
        // Reset IdAllocator singleton for clean test state
        try {
            Field instance = IdAllocator.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            // Ignore if field doesn't exist or can't be reset
        }
    }

    @Test
    public void testResourceLocationConstructor() throws Exception {
        ResourceLocation location = new ResourceLocation("mymod", "test_item");
        ItemTemplateBuilder builder = new ItemTemplateBuilder(location);

        // Verify ResourceLocation field is set
        Field resourceLocationField = ItemTemplateBuilder.class.getDeclaredField("resourceLocation");
        resourceLocationField.setAccessible(true);
        ResourceLocation actualLocation = (ResourceLocation) resourceLocationField.get(builder);

        assertNotNull("ResourceLocation should be set", actualLocation);
        assertEquals("Namespace should match", "mymod", actualLocation.getNamespace());
        assertEquals("Path should match", "test_item", actualLocation.getPath());
    }

    @Test
    public void testResourceLocationConstructorAllocatesId() throws Exception {
        ResourceLocation location = new ResourceLocation("mymod", "test_item");
        ItemTemplateBuilder builder = new ItemTemplateBuilder(location);

        // Verify ID was allocated
        Field templateIdField = ItemTemplateBuilder.class.getDeclaredField("templateId");
        templateIdField.setAccessible(true);
        int templateId = templateIdField.getInt(builder);

        assertTrue("Template ID should be allocated (>= 30000)", templateId >= 30000);
        assertEquals("Template ID should match IdAllocator allocation",
                     IdAllocator.getInstance().allocate(location), templateId);
    }

    @Test
    public void testDeprecatedStringConstructor() throws Exception {
        @SuppressWarnings("deprecation")
        ItemTemplateBuilder builder = new ItemTemplateBuilder("test_item");

        // Verify ResourceLocation was created with default namespace
        Field resourceLocationField = ItemTemplateBuilder.class.getDeclaredField("resourceLocation");
        resourceLocationField.setAccessible(true);
        ResourceLocation actualLocation = (ResourceLocation) resourceLocationField.get(builder);

        assertNotNull("ResourceLocation should be created from string", actualLocation);
        assertEquals("Default namespace should be 'wurm'", "wurm", actualLocation.getNamespace());
        assertEquals("Path should match identifier", "test_item", actualLocation.getPath());
    }

    @Test
    public void testDeprecatedStringConstructorWithNamespace() throws Exception {
        @SuppressWarnings("deprecation")
        ItemTemplateBuilder builder = new ItemTemplateBuilder("mymod:custom_item");

        // Verify ResourceLocation was created with explicit namespace
        Field resourceLocationField = ItemTemplateBuilder.class.getDeclaredField("resourceLocation");
        resourceLocationField.setAccessible(true);
        ResourceLocation actualLocation = (ResourceLocation) resourceLocationField.get(builder);

        assertNotNull("ResourceLocation should be created from namespaced string", actualLocation);
        assertEquals("Namespace should be parsed", "mymod", actualLocation.getNamespace());
        assertEquals("Path should be parsed", "custom_item", actualLocation.getPath());
    }

    @Test
    public void testIdAllocationConsistency() throws Exception {
        ResourceLocation location = new ResourceLocation("mymod", "sword");

        // Create first builder
        ItemTemplateBuilder builder1 = new ItemTemplateBuilder(location);
        Field templateIdField = ItemTemplateBuilder.class.getDeclaredField("templateId");
        templateIdField.setAccessible(true);
        int id1 = templateIdField.getInt(builder1);

        // Create second builder with same location
        ItemTemplateBuilder builder2 = new ItemTemplateBuilder(location);
        int id2 = templateIdField.getInt(builder2);

        assertEquals("Same ResourceLocation should produce same ID", id1, id2);
    }

    @Test
    public void testIdAllocationUniqueness() throws Exception {
        ResourceLocation location1 = new ResourceLocation("mymod", "item1");
        ResourceLocation location2 = new ResourceLocation("mymod", "item2");

        ItemTemplateBuilder builder1 = new ItemTemplateBuilder(location1);
        ItemTemplateBuilder builder2 = new ItemTemplateBuilder(location2);

        Field templateIdField = ItemTemplateBuilder.class.getDeclaredField("templateId");
        templateIdField.setAccessible(true);
        int id1 = templateIdField.getInt(builder1);
        int id2 = templateIdField.getInt(builder2);

        assertNotEquals("Different ResourceLocations should produce different IDs", id1, id2);
    }

    @Test
    public void testNamespaceIsolation() throws Exception {
        ResourceLocation location1 = new ResourceLocation("mod1", "item");
        ResourceLocation location2 = new ResourceLocation("mod2", "item");

        ItemTemplateBuilder builder1 = new ItemTemplateBuilder(location1);
        ItemTemplateBuilder builder2 = new ItemTemplateBuilder(location2);

        Field templateIdField = ItemTemplateBuilder.class.getDeclaredField("templateId");
        templateIdField.setAccessible(true);
        int id1 = templateIdField.getInt(builder1);
        int id2 = templateIdField.getInt(builder2);

        assertNotEquals("Same path in different namespaces should produce different IDs", id1, id2);
    }

    @Test
    public void testBuilderFluentApi() {
        ResourceLocation location = new ResourceLocation("mymod", "test_item");
        ItemTemplateBuilder builder = new ItemTemplateBuilder(location);

        // Verify fluent API works (method chaining)
        ItemTemplateBuilder result = builder
            .name("Test Item", "Test Items", "A test item")
            .size(3)
            .weightGrams(1000)
            .difficulty(25.0f)
            .value(100);

        assertSame("Builder should return itself for method chaining", builder, result);
    }

    @Test
    public void testBackwardCompatibilityWithLegacyCode() throws Exception {
        // Simulate old code using string identifier
        @SuppressWarnings("deprecation")
        ItemTemplateBuilder legacyBuilder = new ItemTemplateBuilder("mymod.old_item");

        Field templateIdField = ItemTemplateBuilder.class.getDeclaredField("templateId");
        templateIdField.setAccessible(true);
        int legacyId = templateIdField.getInt(legacyBuilder);

        // Verify it still allocates IDs properly
        assertTrue("Legacy code should still allocate valid IDs", legacyId >= 30000);

        Field resourceLocationField = ItemTemplateBuilder.class.getDeclaredField("resourceLocation");
        resourceLocationField.setAccessible(true);
        ResourceLocation location = (ResourceLocation) resourceLocationField.get(legacyBuilder);

        assertNotNull("Legacy code should create ResourceLocation internally", location);
    }

    @Test
    public void testMultipleBuilders() throws Exception {
        // Test that multiple builders can be created without interference
        ItemTemplateBuilder b1 = new ItemTemplateBuilder(new ResourceLocation("mod1", "item1"));
        ItemTemplateBuilder b2 = new ItemTemplateBuilder(new ResourceLocation("mod1", "item2"));
        ItemTemplateBuilder b3 = new ItemTemplateBuilder(new ResourceLocation("mod2", "item1"));

        Field templateIdField = ItemTemplateBuilder.class.getDeclaredField("templateId");
        templateIdField.setAccessible(true);

        int id1 = templateIdField.getInt(b1);
        int id2 = templateIdField.getInt(b2);
        int id3 = templateIdField.getInt(b3);

        // All should have valid, unique IDs
        assertTrue("Builder 1 should have valid ID", id1 >= 30000);
        assertTrue("Builder 2 should have valid ID", id2 >= 30000);
        assertTrue("Builder 3 should have valid ID", id3 >= 30000);

        assertNotEquals("Builder 1 and 2 should have different IDs", id1, id2);
        assertNotEquals("Builder 1 and 3 should have different IDs", id1, id3);
        assertNotEquals("Builder 2 and 3 should have different IDs", id2, id3);
    }

    @Test
    public void testIdAllocatorIntegration() {
        ResourceLocation location = new ResourceLocation("mymod", "integrated_item");

        // Allocate ID through IdAllocator first
        int expectedId = IdAllocator.getInstance().allocate(location);

        // Create builder - should get same ID
        ItemTemplateBuilder builder = new ItemTemplateBuilder(location);

        try {
            Field templateIdField = ItemTemplateBuilder.class.getDeclaredField("templateId");
            templateIdField.setAccessible(true);
            int actualId = templateIdField.getInt(builder);

            assertEquals("Builder should use IdAllocator for ID allocation", expectedId, actualId);
        } catch (Exception e) {
            fail("Failed to access templateId field: " + e.getMessage());
        }
    }

    @Test
    public void testDeprecationAnnotationPresent() throws Exception {
        // Verify the deprecated constructor is properly annotated
        java.lang.reflect.Constructor<?> stringConstructor =
            ItemTemplateBuilder.class.getConstructor(String.class);

        assertTrue("String constructor should have @Deprecated annotation",
                   stringConstructor.isAnnotationPresent(Deprecated.class));
    }

    @Test
    public void testNewConstructorNotDeprecated() throws Exception {
        // Verify the new constructor is NOT deprecated
        java.lang.reflect.Constructor<?> locationConstructor =
            ItemTemplateBuilder.class.getConstructor(ResourceLocation.class);

        assertFalse("ResourceLocation constructor should NOT be deprecated",
                    locationConstructor.isAnnotationPresent(Deprecated.class));
    }
}
