package com.aflyingcar.lmrcompat.plugins.vampirism;

import java.util.HashMap;
import java.util.Map;

import com.aflyingcar.lmrcompat.LMRCompat;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.blacklab.lmr.entity.EntityLittleMaid;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class VampirismCompat {
    @GameRegistry.ObjectHolder("vampirism:player.bite")
    static final SoundEvent bite_sfx = null;

    public final static int REFRESH_GARLIC_TICKS = 40;
    public final static int REFRESH_SUNDAMAGE_TICKS = 8;

    public final static int SUNDAMAGE_POTION_EFFECTS_PERIOD = 40;

    public static void preinit() {
        EntityRegistry.registerModEntity(new ResourceLocation("lmreengaged", "ConvertedLittleMaid"), EntityConvertedLittleMaid.class, EntityConvertedLittleMaid.class.getSimpleName(), 0, LMRCompat.getInstance(), 50, 2, true);
    }

    public static void init() {
        VampirismAPI.entityRegistry().addBloodValue(new ResourceLocation("lmreengaged", "LittleMaid"), EntityLittleMaidVampirism.ENTITY_MAID_BLOOD);
        VampirismAPI.entityRegistry().addConvertible(EntityLittleMaid.class, "lmrcompat:textures/vampirism/entity/overlay.png", new EntityConvertedLittleMaid.ConvertingHandler());
    }
}
