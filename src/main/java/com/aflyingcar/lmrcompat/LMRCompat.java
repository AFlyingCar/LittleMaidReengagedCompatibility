package com.aflyingcar.lmrcompat;

import com.aflyingcar.lmrcompat.plugins.vampirism.VampirismCompat;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = LMRCompat.MODID, name = LMRCompat.MODNAME, version = LMRCompat.VERSION, dependencies = LMRCompat.DEPENDENCIES)
public class LMRCompat {
    public static final String MODID = "lmrcompat";
    public static final String MODNAME = "LittleMaidReengaged Compatibility";
    public static final String VERSION = "1.0.0";
    public static final String DEPENDENCIES = "after:vampirism;after:techguns;after:chickens;after:roost";

    private static Logger logger;

    public static boolean hasTechGuns = false;
    public static boolean hasChickens = false;
    public static boolean hasRoost = false;
    public static boolean hasVampirism = false;

    @Mod.Instance
    private static LMRCompat instance;

    public static LMRCompat getInstance() {
        return instance;
    }

    public static Logger getLogger() {
        return logger;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        if(Loader.isModLoaded("vampirism")) {
            logger.info("Vampirism detected: Loading compatibility...");
            VampirismCompat.preinit();

            hasVampirism = true;
        }

        if(Loader.isModLoaded("techguns")) {
            // TODO: Do we need to do any additional setup here?
            hasTechGuns = true;
        }

        if(Loader.isModLoaded("chickens")) {
            // TODO: Do we need to do any additional setup here?
            hasChickens = true;
        }

        if(Loader.isModLoaded("roost")) {
            // TODO: Do we need to do any additional setup here?
            hasRoost = true;
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if(hasVampirism) {
            VampirismCompat.init();
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class RegistrationEventHandler {
        @SubscribeEvent
        public static void onRegisterItems(RegistryEvent.Register<Item> event) {
            logger.info("Registering all items");
            LMRCompatItems.registerAll(event.getRegistry());
        }
    }
}
