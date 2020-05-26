package com.aflyingcar.lmrcompat;

import com.aflyingcar.lmrcompat.items.ItemBloodySugar;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

@GameRegistry.ObjectHolder(LMRCompat.MODID)
public class LMRCompatItems {
    public static final Item BLOODY_SUGAR;

    static {
        BLOODY_SUGAR = new ItemBloodySugar();
    }

    public static void registerAll(IForgeRegistry<Item> registry) {
        LMRCompat.getLogger().info("Registering " + BLOODY_SUGAR.getRegistryName());
        registry.register(BLOODY_SUGAR);
    }
}
