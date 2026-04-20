package com.garward.wurmmodloader.mods.upgradetree.pets;

import com.garward.wurmmodloader.api.capability.Capability;
import com.garward.wurmmodloader.api.registry.ResourceLocation;

/**
 * Capability that persists the player's pet roster. Max slot count is
 * derived at read time from upgrade-tree effects, so a slot cap reduction
 * (e.g., respec) applies instantly without re-serializing.
 */
public class PlayerPetsCapability implements Capability<PlayerPets> {

    public static final PlayerPetsCapability INSTANCE = new PlayerPetsCapability();

    private static final ResourceLocation ID = new ResourceLocation("upgradetree", "player_pets");

    private PlayerPetsCapability() {}

    @Override public Class<PlayerPets> getType() { return PlayerPets.class; }
    @Override public ResourceLocation getId() { return ID; }
    @Override public PlayerPets createDefaultInstance() { return new PlayerPets(); }

    @Override
    public String serialize(PlayerPets pets) {
        if (pets.size() == 0) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Long id : pets.getWurmIds()) {
            if (!first) sb.append(',');
            sb.append(id);
            first = false;
        }
        return sb.toString();
    }

    @Override
    public PlayerPets deserialize(String data) {
        PlayerPets pets = new PlayerPets();
        if (data == null || data.isEmpty()) return pets;
        for (String part : data.split(",")) {
            try {
                pets.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return pets;
    }
}
