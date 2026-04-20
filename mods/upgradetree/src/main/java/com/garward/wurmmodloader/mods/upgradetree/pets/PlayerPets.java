package com.garward.wurmmodloader.mods.upgradetree.pets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-player pet roster. Vanilla's single pet slot holds the "active" pet;
 * this list holds the full roster. Capacity is driven by upgrade-tree effects.
 */
public class PlayerPets {

    private final List<Long> wurmIds = new ArrayList<>();

    public List<Long> getWurmIds() {
        return Collections.unmodifiableList(wurmIds);
    }

    public int size() {
        return wurmIds.size();
    }

    public boolean contains(long wurmId) {
        return wurmIds.contains(wurmId);
    }

    public boolean add(long wurmId) {
        if (wurmIds.contains(wurmId)) return false;
        return wurmIds.add(wurmId);
    }

    public boolean remove(long wurmId) {
        return wurmIds.remove(Long.valueOf(wurmId));
    }
}
