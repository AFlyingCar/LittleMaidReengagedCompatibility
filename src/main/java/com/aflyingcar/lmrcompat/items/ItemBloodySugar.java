package com.aflyingcar.lmrcompat.items;

import com.aflyingcar.lmrcompat.LMRCompat;
import de.teamlapen.vampirism.VampirismMod;
import net.minecraft.item.Item;

// TODO: Should this implement IItemSpecialSugar?
public class ItemBloodySugar extends Item{
    public ItemBloodySugar() {
        // We want this item specifically to show up in the vampirism creative tab (well, probably the little maids one
        //  would be better, but we don't actually _have_ an lmr tab)
        setCreativeTab(VampirismMod.creativeTab);
        setRegistryName(LMRCompat.MODID, "bloody_sugar");
        setUnlocalizedName(LMRCompat.MODID + ".bloody_sugar");
    }
}
