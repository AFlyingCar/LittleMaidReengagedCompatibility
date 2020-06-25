package com.aflyingcar.lmrcompat;

import com.aflyingcar.lmrcompat.items.ItemBloodySugar;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
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
        if(LMRCompat.hasVampirism) {
            LMRCompat.getLogger().info("Registering " + BLOODY_SUGAR.getRegistryName());
            registry.register(BLOODY_SUGAR);
        }
    }

    public static void registerAllItemModels() {
        if(LMRCompat.hasVampirism) {
            ModelLoader.setCustomModelResourceLocation(BLOODY_SUGAR, 0, new ModelResourceLocation(new ResourceLocation(LMRCompat.MODID, "bloody_sugar"), "inventory"));
        }
    }
}
