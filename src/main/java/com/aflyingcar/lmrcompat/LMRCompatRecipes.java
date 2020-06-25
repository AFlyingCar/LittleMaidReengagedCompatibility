package com.aflyingcar.lmrcompat;

import com.aflyingcar.lmrcompat.recipes.BloodySugarRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.registries.IForgeRegistry;

public class LMRCompatRecipes {
    private static final BloodySugarRecipe BLOODY_SUGAR_RECIPE;

    static {
        BLOODY_SUGAR_RECIPE = new BloodySugarRecipe();
    }

    public static void registerAll(IForgeRegistry<IRecipe> registry) {
        registry.register(BLOODY_SUGAR_RECIPE);
    }

}
