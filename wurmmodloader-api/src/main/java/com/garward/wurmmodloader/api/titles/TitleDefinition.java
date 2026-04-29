package com.garward.wurmmodloader.api.titles;

/**
 * Immutable description of a custom title queued through {@link TitleRegistry}.
 */
public final class TitleDefinition {

    private final int id;
    private final String maleName;
    private final String femaleName;
    private final int skillId;
    private final String type;

    public TitleDefinition(int id, String maleName, String femaleName, int skillId, String type) {
        if (maleName == null || maleName.isEmpty()) {
            throw new IllegalArgumentException("maleName required");
        }
        if (femaleName == null || femaleName.isEmpty()) {
            throw new IllegalArgumentException("femaleName required");
        }
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("type required");
        }
        this.id = id;
        this.maleName = maleName;
        this.femaleName = femaleName;
        this.skillId = skillId;
        this.type = type;
    }

    public int getId() { return id; }
    public String getMaleName() { return maleName; }
    public String getFemaleName() { return femaleName; }
    public int getSkillId() { return skillId; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return "TitleDefinition[id=" + id + ", male=\"" + maleName + "\", female=\"" + femaleName
            + "\", skill=" + skillId + ", type=" + type + "]";
    }
}
