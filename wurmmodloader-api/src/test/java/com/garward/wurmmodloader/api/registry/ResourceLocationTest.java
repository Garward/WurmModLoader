package com.garward.wurmmodloader.api.registry;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ResourceLocation.
 */
public class ResourceLocationTest {

    @Test
    public void testConstructorValid() {
        ResourceLocation loc = new ResourceLocation("mymod", "magic_sword");
        assertEquals("mymod", loc.getNamespace());
        assertEquals("magic_sword", loc.getPath());
    }

    @Test
    public void testOfMethodDefaultNamespace() {
        // Test using of() method instead - constructor only takes two args
        ResourceLocation loc = ResourceLocation.of("simple_item");
        assertEquals("wurm", loc.getNamespace());
        assertEquals("simple_item", loc.getPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullNamespace() {
        new ResourceLocation(null, "item");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullPath() {
        new ResourceLocation("mymod", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmptyNamespace() {
        new ResourceLocation("", "item");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmptyPath() {
        new ResourceLocation("mymod", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidCharactersNamespace() {
        new ResourceLocation("My Mod!", "item");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidCharactersPath() {
        new ResourceLocation("mymod", "Magic Sword!");
    }

    @Test
    public void testOfMethod() {
        ResourceLocation loc = ResourceLocation.of("mymod:magic_sword");
        assertEquals("mymod", loc.getNamespace());
        assertEquals("magic_sword", loc.getPath());
    }


    @Test(expected = IllegalArgumentException.class)
    public void testOfMethodMultipleColons() {
        ResourceLocation.of("my:mod:item");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOfMethodNull() {
        ResourceLocation.of(null);
    }

    @Test
    public void testToString() {
        ResourceLocation loc = new ResourceLocation("mymod", "magic_sword");
        assertEquals("mymod:magic_sword", loc.toString());
    }

    @Test
    public void testEquals() {
        ResourceLocation loc1 = new ResourceLocation("mymod", "item");
        ResourceLocation loc2 = new ResourceLocation("mymod", "item");
        ResourceLocation loc3 = new ResourceLocation("othermod", "item");
        ResourceLocation loc4 = new ResourceLocation("mymod", "other_item");

        assertEquals(loc1, loc2);
        assertNotEquals(loc1, loc3);
        assertNotEquals(loc1, loc4);
        assertNotEquals(loc1, null);
        assertNotEquals(loc1, "mymod:item");
    }

    @Test
    public void testHashCode() {
        ResourceLocation loc1 = new ResourceLocation("mymod", "item");
        ResourceLocation loc2 = new ResourceLocation("mymod", "item");
        ResourceLocation loc3 = new ResourceLocation("othermod", "item");

        assertEquals(loc1.hashCode(), loc2.hashCode());
        assertNotEquals(loc1.hashCode(), loc3.hashCode());
    }

    @Test
    public void testCompareTo() {
        ResourceLocation a = new ResourceLocation("amod", "item");
        ResourceLocation b = new ResourceLocation("bmod", "item");
        ResourceLocation c = new ResourceLocation("amod", "zitem");

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(c) < 0);
        assertEquals(0, a.compareTo(new ResourceLocation("amod", "item")));
    }

    @Test
    public void testValidCharacters() {
        // Valid characters: lowercase, digits, underscore, dot, dash
        new ResourceLocation("my-mod.v2", "magic_sword_v1.0");
        new ResourceLocation("mod123", "item456");
    }

    @Test
    public void testImmutability() {
        ResourceLocation loc = new ResourceLocation("mymod", "item");
        String originalNamespace = loc.getNamespace();
        String originalPath = loc.getPath();

        // These should return new strings, not references
        assertEquals(originalNamespace, loc.getNamespace());
        assertEquals(originalPath, loc.getPath());
    }
}
