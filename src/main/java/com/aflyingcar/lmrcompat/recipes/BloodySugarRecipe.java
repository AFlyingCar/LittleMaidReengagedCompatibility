package com.aflyingcar.lmrcompat.recipes;

import com.aflyingcar.lmrcompat.LMRCompat;
import com.aflyingcar.lmrcompat.LMRCompatItems;
import com.aflyingcar.lmrcompat.items.ItemBloodySugar;
import de.teamlapen.vampirism.core.ModItems;
import de.teamlapen.vampirism.items.ItemBloodBottle;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.ArrayList;
import java.util.List;

public class BloodySugarRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    public BloodySugarRecipe() {
        setRegistryName(LMRCompat.MODID, "bloody_sugar");
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        boolean foundBloodBottle = false;
        boolean foundSugar = false;

        for(int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack stack = inv.getStackInSlot(i);

            if(!stack.isEmpty()) {
                if(stack.getItem() == Items.SUGAR) {
                    foundSugar = true;
                } else if(stack.getItem() instanceof ItemBloodBottle && stack.getItemDamage() > 0) {
                    // The recipe will _not_ work if 2 blood bottles are found
                    if(foundBloodBottle) return false;

                    foundBloodBottle = true;
                }
            }
        }

        return foundBloodBottle && foundSugar;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.from(Ingredient.EMPTY, Ingredient.fromItem(Items.SUGAR), Ingredient.fromStacks(new ItemStack(ModItems.blood_bottle, 1, 9)));
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        int numSugar = 0;
        ItemStack bloodBottle = null;

        for(int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack stack = inv.getStackInSlot(i);

            if(!stack.isEmpty()) {
                if(stack.getItem() == Items.SUGAR) {
                    ++numSugar;
                } else if(stack.getItem() instanceof ItemBloodBottle && stack.getItemDamage() > 0) {
                    bloodBottle = stack;
                }
            }
        }

        // This should never happen, as matches() should verify the recipe for us
        //  But it never hurts to be extra careful
        if(bloodBottle == null || numSugar == 0)
            return null;

        // We will only make as much bloody sugar as we have blood for or as we have sugar for
        int amountOfSugarToMake = Math.min(numSugar, bloodBottle.getItemDamage());

        // Now go ahead and return as much bloody sugar as we are able to make
        return new ItemStack(LMRCompatItems.BLOODY_SUGAR, amountOfSugarToMake);
    }

    @Override
    public boolean canFit(int width, int height) {
        // Can fit so long as there are at least 2 slots
        return width >= 2 || height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        // TODO: What about the size of the stack that gets returned???
        return new ItemStack(LMRCompatItems.BLOODY_SUGAR);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        // Get the normal recipe
        NonNullList<ItemStack> remaining = IRecipe.super.getRemainingItems(inv);
//        NonNullList<ItemStack> remaining = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        int bloodBottleSlot = 0;

        // There has to be a better way of getting this without doing this loop _again_
        ItemStack bloodBottle = null;
        int numSugar = 0;
        for(int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack stack = inv.getStackInSlot(i);

            if(!stack.isEmpty()) {
                if(stack.getItem() == Items.SUGAR) {
                    ++numSugar;
                } else if(stack.getItem() instanceof ItemBloodBottle && stack.getItemDamage() > 0) {
                    bloodBottle = stack.copy();
                    bloodBottleSlot = i;
                }
            }
        }

        // This should never happen, as matches() should verify the recipe for us
        //  But it never hurts to be extra careful
        if(bloodBottle == null || numSugar == 0)
            return null;

        // We will only make as much bloody sugar as we have blood for or as we have sugar for
        int amountOfSugarToMake = Math.min(numSugar, bloodBottle.getItemDamage());

        // TODO: For some reason this is duplicating sugar item stacks
        //  I tried understanding SlotCrafting::onTake, but I can't figure out what the hell that method is doing
        //  So for now, some sugar will get wasted, but that's okay, since it's better than it getting _duplicated_
        /*
        // Only consume the amount of sugar that we actually end up making
        //  It doesn't make sense to consume more than that
        int sugarConsumed = 0;
        for(int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack stack = inv.getStackInSlot(i);
            if(!stack.isEmpty()) {
                if(stack.getItem() == Items.SUGAR) {
                    ++sugarConsumed;
                    if(sugarConsumed <= amountOfSugarToMake) {
                        stack.shrink(1);
                    }
                    remaining.set(i, stack.copy());
                }
            }
        }
         */

        // Make sure to reduce the amount of blood in the bottle first
        bloodBottle.setItemDamage(bloodBottle.getItemDamage() - amountOfSugarToMake);
        remaining.set(bloodBottleSlot, bloodBottle);

        return remaining;
    }
}
