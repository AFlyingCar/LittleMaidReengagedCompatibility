package com.aflyingcar.lmrcompat;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = "lmrcompat", name = "LittleMaidReengaged Compatibility", version = LMRCompat.VERSION)
public class LMRCompat {
    public static final String VERSION = "1.0.0";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        System.out.println("LMRCompat:PreInit");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        System.out.println("LMRCompat:Init");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        System.out.println("LMRCompat:PostInit");
    }
}
