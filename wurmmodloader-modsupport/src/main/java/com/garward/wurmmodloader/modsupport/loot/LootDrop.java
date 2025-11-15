package com.garward.wurmmodloader.modsupport.loot;

import com.wurmonline.server.FailedException;
import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.players.Player;
import com.wurmonline.shared.constants.ItemMaterials;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

/**
 * Builder for creating item drops with various properties.
 *
 * <p>This builder allows you to create items with specific properties such as quality,
 * rarity, material, etc. You can use constant values or functions that generate values
 * based on the dead creature and killer.</p>
 *
 * @since 1.0.0
 */
public class LootDrop implements LootFunction<Collection<Item>> {
    private final LootFunction<Integer> templateGen;
    private LootFunction<Integer> repeatGen = (c, k) -> 1;
    private LootFunction<Float> qlGen = (c, k) -> 99f;
    private LootFunction<Byte> rarityGen = (c, k) -> MiscConstants.COMMON;
    private LootFunction<Byte> materialGen = (c, k) -> ItemMaterials.MATERIAL_UNDEFINED;
    private LootFunction<String> creatorGen = (c, k) -> k.getName();

    private LootFunction<Byte> auxGen = null;
    private LootFunction<Integer> data1Gen = null;
    private LootFunction<Integer> data2Gen = null;
    private LootFunction<Integer> weightGen = null;
    private LootFunction<Float> damageGen = null;
    private LootFunction<String> nameGen = null;
    private LootFunction<Integer> realTplGen = null;

    private LootDrop(LootFunction<Integer> templateId) {
        this.templateGen = templateId;
    }

    /**
     * Set number of drops from function
     *
     * @param repeat The function that returns the number of items to drop
     * @return This builder for chaining
     */
    public LootDrop repeat(LootFunction<Integer> repeat) {
        this.repeatGen = repeat;
        return this;
    }

    /**
     * Set number of drops from constant
     *
     * @param _repeat The number of items to drop
     * @return This builder for chaining
     */
    public LootDrop repeat(int _repeat) {
        return repeat((c, k) -> _repeat);
    }

    /**
     * Set ql from function
     *
     * @param ql The function that returns the quality
     * @return This builder for chaining
     */
    public LootDrop ql(LootFunction<Float> ql) {
        this.qlGen = ql;
        return this;
    }

    /**
     * Set ql from constant
     *
     * @param _ql The quality value
     * @return This builder for chaining
     */
    public LootDrop ql(float _ql) {
        return ql((c, k) -> _ql);
    }

    /**
     * Set rarity from function
     *
     * @param rarity The function that returns the rarity
     * @return This builder for chaining
     */
    public LootDrop rarity(LootFunction<Byte> rarity) {
        this.rarityGen = rarity;
        return this;
    }

    /**
     * Set rarity from constant
     *
     * @param _rarity The rarity value
     * @return This builder for chaining
     */
    public LootDrop rarity(byte _rarity) {
        return rarity((c, k) -> _rarity);
    }

    /**
     * Set material from function
     *
     * @param material The function that returns the material
     * @return This builder for chaining
     */
    public LootDrop material(LootFunction<Byte> material) {
        this.materialGen = material;
        return this;
    }

    /**
     * Set material from constant
     *
     * @param _material The material value
     * @return This builder for chaining
     */
    public LootDrop material(byte _material) {
        return material((c, k) -> _material);
    }

    /**
     * Set creator from function
     *
     * @param creator The function that returns the creator name
     * @return This builder for chaining
     */
    public LootDrop creator(LootFunction<String> creator) {
        this.creatorGen = creator;
        return this;
    }

    /**
     * Set creator from constant
     *
     * @param _creator The creator name
     * @return This builder for chaining
     */
    public LootDrop creator(String _creator) {
        return creator((c, k) -> _creator);
    }

    /**
     * Set aux from function
     *
     * @param aux The function that returns the aux data
     * @return This builder for chaining
     */
    public LootDrop aux(LootFunction<Byte> aux) {
        this.auxGen = aux;
        return this;
    }

    /**
     * Set aux from constant
     *
     * @param _aux The aux data value
     * @return This builder for chaining
     */
    public LootDrop aux(byte _aux) {
        return aux((c, k) -> _aux);
    }

    /**
     * Set data1 from function
     *
     * @param data1 The function that returns data1
     * @return This builder for chaining
     */
    public LootDrop data1(LootFunction<Integer> data1) {
        this.data1Gen = data1;
        return this;
    }

    /**
     * Set data1 from constant
     *
     * @param _data1 The data1 value
     * @return This builder for chaining
     */
    public LootDrop data1(int _data1) {
        return data1((c, k) -> _data1);
    }

    /**
     * Set data2 from function
     *
     * @param data2 The function that returns data2
     * @return This builder for chaining
     */
    public LootDrop data2(LootFunction<Integer> data2) {
        this.data2Gen = data2;
        return this;
    }

    /**
     * Set data2 from constant
     *
     * @param _data2 The data2 value
     * @return This builder for chaining
     */
    public LootDrop data2(int _data2) {
        return data2((c, k) -> _data2);
    }

    /**
     * Set weight from function
     *
     * @param weight The function that returns the weight
     * @return This builder for chaining
     */
    public LootDrop weight(LootFunction<Integer> weight) {
        this.weightGen = weight;
        return this;
    }

    /**
     * Set weight from constant
     *
     * @param _weight The weight value
     * @return This builder for chaining
     */
    public LootDrop weight(int _weight) {
        return weight((c, k) -> _weight);
    }

    /**
     * Set damage from function
     *
     * @param damage The function that returns the damage
     * @return This builder for chaining
     */
    public LootDrop damage(LootFunction<Float> damage) {
        this.damageGen = damage;
        return this;
    }

    /**
     * Set damage from constant
     *
     * @param _damage The damage value
     * @return This builder for chaining
     */
    public LootDrop damage(float _damage) {
        return damage((c, k) -> _damage);
    }

    /**
     * Set name from function
     *
     * @param name The function that returns the name
     * @return This builder for chaining
     */
    public LootDrop name(LootFunction<String> name) {
        this.nameGen = name;
        return this;
    }

    /**
     * Set name from constant
     *
     * @param _name The name value
     * @return This builder for chaining
     */
    public LootDrop name(String _name) {
        return name((c, k) -> _name);
    }

    /**
     * Set template from function
     *
     * @param template The function that returns the real template
     * @return This builder for chaining
     */
    public LootDrop realTemplate(LootFunction<Integer> template) {
        this.realTplGen = template;
        return this;
    }

    /**
     * Set template from constant
     *
     * @param _template The real template value
     * @return This builder for chaining
     */
    public LootDrop realTemplate(int _template) {
        return realTemplate((c, k) -> _template);
    }

    /**
     * Create a new loot drop builder with a function that generates the template ID.
     *
     * @param templateId The function that returns the template ID
     * @return A new loot drop builder
     */
    public static LootDrop create(LootFunction<Integer> templateId) {
        return new LootDrop(templateId);
    }

    /**
     * Create a new loot drop builder with a constant template ID.
     *
     * @param templateId The template ID
     * @return A new loot drop builder
     */
    public static LootDrop create(int templateId) {
        return new LootDrop((c, k) -> templateId);
    }

    /**
     * Helper for static drop with just item template id
     * the generated item will be 99ql, common and default material
     * WARNING: All values passed here will be evaluated only once,
     * if you want random or any kind of logic use a lambda instead
     *
     * @param templateId The template ID
     * @return A loot drop builder
     */
    public static LootDrop staticDrop(int templateId) {
        return create(templateId);
    }

    /**
     * Helper for static drop with item template id and ql
     * the generated item will be common and have default material
     * WARNING: All values passed here will be evaluated only once,
     * if you want random or any kind of logic use a lambda instead
     *
     * @param templateId The template ID
     * @param ql The quality
     * @return A loot drop builder
     */
    public static LootDrop staticDrop(int templateId, float ql) {
        return create(templateId).ql(ql);
    }

    /**
     * Helper for static drop with item template id, ql, rarity
     * the generated item will have default material
     * WARNING: All values passed here will be evaluated only once,
     * if you want random or any kind of logic use a lambda instead
     *
     * @param templateId The template ID
     * @param ql The quality
     * @param rarity The rarity
     * @return A loot drop builder
     */
    public static LootDrop staticDrop(int templateId, float ql, byte rarity) {
        return create(templateId).ql(ql).rarity(rarity);
    }

    /**
     * Helper for static drop with item template td, ql, rarity and material
     * WARNING: All values passed here will be evaluated only once,
     * if you want random or any kind of logic use a lambda instead
     *
     * @param templateId The template ID
     * @param ql The quality
     * @param rarity The rarity
     * @param material The material
     * @return A loot drop builder
     */
    public static LootDrop staticDrop(int templateId, float ql, byte rarity, byte material) {
        return create(templateId).ql(ql).rarity(rarity).material(material);
    }

    @Override
    public Collection<Item> apply(Creature deadCreature, Player killer) {
        int num = repeatGen.apply(deadCreature, killer);
        ArrayList<Item> res = new ArrayList<Item>(num);
        for (int i = 0; i < num; i++) {
            try {
                Item item = ItemFactory.createItem(
                        templateGen.apply(deadCreature, killer),
                        qlGen.apply(deadCreature, killer),
                        materialGen.apply(deadCreature, killer),
                        rarityGen.apply(deadCreature, killer),
                        creatorGen.apply(deadCreature, killer)
                );

                if (auxGen != null) item.setAuxData(auxGen.apply(deadCreature, killer));
                if (data1Gen != null) item.setData1(data1Gen.apply(deadCreature, killer));
                if (data2Gen != null) item.setData2(data2Gen.apply(deadCreature, killer));
                if (weightGen != null) item.setWeight(weightGen.apply(deadCreature, killer), false);
                if (damageGen != null) item.setDamage(damageGen.apply(deadCreature, killer));
                if (nameGen != null) item.setName(nameGen.apply(deadCreature, killer));
                if (realTplGen != null) item.setRealTemplate(realTplGen.apply(deadCreature, killer));

                res.add(item);
            } catch (FailedException e) {
                LootManager.logger.log(Level.SEVERE, String.format("Error creating drop for %s from %s", killer.getName(), deadCreature.getName()), e);
            } catch (NoSuchTemplateException e) {
                LootManager.logger.log(Level.SEVERE, String.format("Error creating drop for %s from %s", killer.getName(), deadCreature.getName()), e);
            }
        }
        return res;
    }
}
