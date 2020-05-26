package com.aflyingcar.lmrcompat;

import net.blacklab.lmr.entity.EntityLittleMaid;
import net.blacklab.lmr.util.FileList;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import techguns.plugins.ftbl.TeamSystemIntegration;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Util {
    private static boolean loadedAlready = false;

    public static void loadResources() {
        if(loadedAlready) return;

        List<File> mod_file = FileList.getModFile("lmrcompat", "lmrcompat");

        if(mod_file.isEmpty()) {
            System.err.println("Uh oh, for some reason we couldn't find ourselves! But if so, then how are we even running?");
            return;
        }

        // I'm just going to assume that we're the _only_ mod called lmrcompat. If we aren't, then, well, shit.
        File self = mod_file.get(0);

        // Copied from LMR's ManagerBase.java:59
        if (self.isFile() && (self.getName().endsWith(".zip") || self.getName().endsWith(".jar"))) {

            // Copied from LMR's ManagerBase.java:112
            try {
                FileInputStream fileinputstream = new FileInputStream(self);
                ZipInputStream zipinputstream = new ZipInputStream(fileinputstream);
                ZipEntry zipentry;

                do {
                    zipentry = zipinputstream.getNextEntry();
                    if(zipentry == null) {
                        break;
                    }
                    if (!zipentry.isDirectory()) {
                        String lname = zipentry.getName();

                        if(lname.contains("assets/")) {
                            // TODO: Load the assets here... somehow
                        }
                    }
                } while(true);

                zipinputstream.close();
                fileinputstream.close();
            }
            catch (Exception exception) {
                System.err.println("Failed to load the zip file.");
            }

        }

        loadedAlready = true;
    }

    public static void grantAchievement(EntityPlayer player, String achievementName) {
        if(player instanceof EntityPlayerMP) {
            AdvancementManager advancementManager = player.world.getMinecraftServer().getAdvancementManager();
            if(advancementManager != null) {
                Advancement advancement = advancementManager.getAdvancement(new ResourceLocation("lmrcompat", achievementName));
                if(advancement != null) {
                    ((EntityPlayerMP)player).getAdvancements().grantCriterion(advancement, "done");
                }
            }
        }
    }

    public static ItemStack combineStacks(ItemStack item1, ItemStack item2) {
        int count1 = item1.getCount();
        int count2 = item2.getCount();

        count1 += count2;

        if(count1 > item1.getMaxStackSize()) {
            count2 = count1 - item1.getMaxStackSize();
            count1 = item1.getMaxStackSize();

            if(count2 < 0) count2 = 0;
        } else {
            count2 = 0;
        }

        item1.setCount(count1);
        item2.setCount(count2);

        return item1;
    }

    public static boolean isPlayerTargetable(EntityPlayer player, EntityLittleMaid maid) {
        return TeamSystemIntegration.isEnemy(player.getUniqueID(), maid.getMaidMasterUUID());
    }
}
