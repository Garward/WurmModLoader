package org.gotti.wurmunlimited.modsupport;

/**
 * Legacy IdType enum re-export for backward compatibility.
 * This is the same enum as the new implementation.
 */
public enum IdType implements IIdType {
    ITEMTEMPLATE(1000),
    CREATURETEMPLATE(2000),
    ACTION(3000);

    private int lastUsedId;

    IdType(int firstId) {
        this.lastUsedId = firstId;
    }

    @Override
    public int startValue() {
        // Not used in new implementation, but required by interface
        return 0;
    }

    @Override
    public boolean isCountingDown() {
        // Not used in new implementation, but required by interface
        return false;
    }

    @Override
    public String typeName() {
        return name();
    }

    @Override
    public int getLastUsedId() {
        return lastUsedId;
    }

    @Override
    public void updateLastUsedId(int id) {
        if (id > lastUsedId) {
            lastUsedId = id;
        }
    }

    /**
     * Convert to internal IdType for delegation.
     */
    public com.garward.wurmmodloader.modsupport.IdType toInternal() {
        return com.garward.wurmmodloader.modsupport.IdType.valueOf(this.name());
    }

    /**
     * Convert from internal IdType.
     */
    public static IdType fromInternal(com.garward.wurmmodloader.modsupport.IdType internal) {
        return IdType.valueOf(internal.name());
    }
}
