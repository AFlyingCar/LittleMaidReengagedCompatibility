package com.aflyingcar.lmrcompat;

import com.aflyingcar.lmrcompat.plugins.vampirism.EntityLittleMaidVampirism;
import com.aflyingcar.lmrcompat.plugins.vampirism.VampirismCompat;
import com.aflyingcar.lmrcompat.proxies.CommonProxy;
import de.teamlapen.vampirism.core.ModItems;
import de.teamlapen.vampirism.items.ItemInjection;
import de.teamlapen.vampirism.potion.PotionSanguinare;
import de.teamlapen.vampirism.potion.PotionSanguinareEffect;
import net.blacklab.lmr.api.mode.EntityModeRegistrar;
import net.blacklab.lmr.entity.EntityLittleMaid;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = LMRCompat.MODID, name = LMRCompat.MODNAME, version = LMRCompat.VERSION, dependencies = LMRCompat.DEPENDENCIES)
public class LMRCompat {
    public static final String MODID = "lmrcompat";
    public static final String MODNAME = "LittleMaidReengaged Compatibility";
    public static final String VERSION = "1.1.0";
    public static final String DEPENDENCIES = "after:vampirism;after:techguns;after:chickens;after:roost";

    private static Logger logger;

    public static boolean hasTechGuns = false;
    public static boolean hasChickens = false;
    public static boolean hasRoost = false;
    public static boolean hasVampirism = false;

    @SidedProxy(clientSide = "com.aflyingcar.lmrcompat.proxies.ClientProxy", serverSide = "com.aflyingcar.lmrcompat.proxies.CommonProxy")
    protected static CommonProxy proxy;

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

        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if(hasTechGuns) {
            EntityModeRegistrar.registerMaidMode(EntityMode_Gunslinger.class);
        }

        if(hasChickens && hasRoost) {
            EntityModeRegistrar.registerMaidMode(EntityMode_ChickenBreeder.class);
        }

        if(hasVampirism) {
            VampirismCompat.init();
            // EntityModeRegistrar.registerMaidMode(EntityMode_VampireHunter.class);
        }

        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class RegistrationEventHandler {
        @SubscribeEvent
        public static void onRegisterItems(RegistryEvent.Register<Item> event) {
            logger.info("Registering all items");
            LMRCompatItems.registerAll(event.getRegistry());
        }

        @SubscribeEvent
        public static void onRegisterRecipes(RegistryEvent.Register<IRecipe> event) {
            logger.info("Registering all recipes.");
            LMRCompatRecipes.registerAll(event.getRegistry());
        }
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class EventHandler {
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onRightClick(PlayerInteractEvent.EntityInteract event) {
            if(!hasVampirism) return;

            ItemStack stack = event.getItemStack();
            if(!stack.isEmpty() && stack.getItem().equals(ModItems.injection))
            {
                if(event.getTarget() instanceof EntityLittleMaid && !(event.getTarget() instanceof EntityLittleMaidVampirism)) {
                    if(stack.getMetadata() == ItemInjection.META_GARLIC) {
                        // TODO: Convert Maid to a hunter variant (disables ability to become a vampire)
                        //    Enable stake job???
                    } else if(stack.getMetadata() == ItemInjection.META_SANGUINARE) {
                        // TODO: This for some reason spawns an empty husk ???
                        //  It's not a real maid, that's for certain
                        /*
                            PotionSanguinare.addRandom((EntityLivingBase) event.getTarget(), false);
                            event.setCancellationResult(EnumActionResult.SUCCESS);
                         */
                    }
                }
            }
        }
    }
}
