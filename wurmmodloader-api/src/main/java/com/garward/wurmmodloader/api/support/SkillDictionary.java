package com.garward.wurmmodloader.api.support;

import com.wurmonline.server.skills.SkillSystem;

/**
 * Lightweight bridge into {@link SkillSystem} for mods that previously depended on SkillAssist.
 */
public final class SkillDictionary {

    private SkillDictionary() {
    }

    /**
     * Resolves a skill identifier by name, falling back to numeric parsing when necessary.
     *
     * @param token Canonical skill name or numeric id.
     * @return Skill id, or -1 if unknown.
     */
    public static int getSkillId(String token) {
        if (token == null || token.trim().isEmpty()) {
            return -1;
        }
        int fromName = SkillSystem.getSkillByName(token);
        if (fromName >= 0) {
            return fromName;
        }
        try {
            return Integer.parseInt(token.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * @param skillId the numeric id
     * @return Human-friendly skill name or {@code "unknown"} if not registered.
     */
    public static String getSkillName(int skillId) {
        return SkillSystem.getNameFor(skillId);
    }
}
