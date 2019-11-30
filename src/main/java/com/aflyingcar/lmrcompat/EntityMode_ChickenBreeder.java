package com.aflyingcar.lmrcompat;

import com.timwoodcreates.roost.data.DataChicken;
import com.timwoodcreates.roost.data.DataChickenModded;
import com.timwoodcreates.roost.item.ItemChicken;
import com.timwoodcreates.roost.tileentity.TileEntityBreeder;
import com.timwoodcreates.roost.tileentity.TileEntityChickenContainer;
import net.blacklab.lib.obj.Pair;
import net.blacklab.lmr.achievements.AchievementsLMRE;
import net.blacklab.lmr.entity.EntityLittleMaid;
import net.blacklab.lmr.entity.mode.EntityModeBlockBase;
import net.blacklab.lmr.inventory.InventoryLittleMaid;
import net.blacklab.lmr.util.EnumSound;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSeeds;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.lang.reflect.Field;
import java.util.*;

public class EntityMode_ChickenBreeder extends EntityModeBlockBase {
    public static final int MMODE_CHICKENS = 0x0091;

    protected static Field dataChickenGainField;
    protected static Field dataChickenGrowthField;
    protected static Field dataChickenStrengthField;

    static {
        try {
            dataChickenGainField = DataChickenModded.class.getDeclaredField("gain");
            dataChickenGainField.setAccessible(true);

            dataChickenGrowthField = DataChickenModded.class.getDeclaredField("growth");
            dataChickenGrowthField.setAccessible(true);

            dataChickenStrengthField = DataChickenModded.class.getDeclaredField("strength");
            dataChickenStrengthField.setAccessible(true);
        } catch(NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    // TODO: We should be calculating the stats and storing them in here
    protected Map<Integer, Pair<Integer, Double>> chicken_stats = new HashMap<>();

    private static int getDataChickenGain(DataChickenModded data) {
        if(dataChickenGainField != null) {
            try {
                return (Integer)dataChickenGainField.get(data);
            } catch(IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    private static int getDataChickenGrowth(DataChickenModded data) {
        if(dataChickenGrowthField != null) {
            try {
                return (Integer)dataChickenGrowthField.get(data);
            } catch(IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    private static int getDataChickenStrength(DataChickenModded data) {
        if(dataChickenStrengthField != null) {
            try {
                return (Integer)dataChickenStrengthField.get(data);
            } catch(IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    public EntityMode_ChickenBreeder(EntityLittleMaid pEntity) {
        super(pEntity);
    }

    @Override
    public void init() { }

    @Override
    public void addEntityMode(EntityAITasks pDefaultMove, EntityAITasks pDefaultTargeting) {
        // Chickens:0x0091
        EntityAITasks[] ltasks = new EntityAITasks[2];
        ltasks[0] = pDefaultMove;
        ltasks[1] = new EntityAITasks(owner.aiProfiler);

        owner.addMaidMode(ltasks, "ChickenBreeder", MMODE_CHICKENS);
    }

    @Override
    public int priority() {
        return 6000;
    }

    @Override
    public boolean changeMode(EntityPlayer pentityplayer) {
        ItemStack litemstack = owner.getHandSlotForModeChange();
        if (!litemstack.isEmpty()) {
            if (litemstack.getItem() instanceof ItemSeeds) {
                owner.setMaidMode("ChickenBreeder");
                if (pentityplayer != null) {
                    AchievementsLMRE.grantAdvancement(pentityplayer, "chickenbreeder");
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setMode(int pMode) {
        switch (pMode) {
            case MMODE_CHICKENS :
                owner.setBloodsuck(false);
//			owner.aiJumpTo.setEnable(false);
                owner.aiFollow.setEnable(false);
                owner.aiAvoidPlayer.setEnable(false);
                owner.aiAttack.setEnable(false);
                owner.aiShooting.setEnable(false);
                return true;
        }

        return false;
    }

    @Override
    public int getNextEquipItem(int pMode) {
        int li;
        // モードに応じた識別判定、速度優先
        switch (pMode) {
            case MMODE_CHICKENS :
                for (li = 0; li < owner.getMaidInventory().getSizeInventory(); li++) {
                    // 調理
                    if (TileEntityChickenContainer.isSeed(owner.getMaidInventory().getStackInSlot(li))) {
                        swapItemIntoMainHandSlot(li);
                        return InventoryLittleMaid.handInventoryOffset;
                    }
                }
                break;
        }

        return -1;
    }

    @Override
    public boolean checkItemStack(ItemStack pItemStack) {
        return TileEntityChickenContainer.isSeed(pItemStack) || pItemStack.getItem() instanceof ItemChicken;
    }


    @Override
    public boolean isSearchBlock() {
        if (!super.isSearchBlock()) return false;

        if (!owner.getCurrentEquippedItem().isEmpty() &&
            (owner.getMaidInventory().getInventorySlotContainItem(ItemSeeds.class) > -1 ||
             owner.getMaidInventory().getInventorySlotContainItem(ItemChicken.class) > -1))
        {
            fDistance = Double.MAX_VALUE;
            owner.clearTilePos();
            owner.setSneaking(false);
            return true;
        }
        return false;
    }



    @Override
    public boolean shouldBlock(int pMode) {
        return owner.maidTileEntity instanceof TileEntityBreeder &&
               (TileEntityChickenContainer.isSeed(owner.getCurrentEquippedItem()));
    }

    @Override
    public boolean checkBlock(int pMode, int px, int py, int pz) {
        TileEntity ltile = owner.world.getTileEntity(new BlockPos(px, py, pz));
        if (!(ltile instanceof TileEntityFurnace)) {
            return false;
        }

        // 世界のメイドから
        if (checkWorldMaid(ltile)) return false;
        // 使用していた竈ならそこで終了
        if (owner.isUsingTile(ltile)) return true;

        double ldis = owner.getDistanceTilePosSq(ltile);
        if (fDistance > ldis) {
            owner.setTilePos(ltile);
            fDistance = ldis;
        }

        return false;
    }

    @Override
    public boolean executeBlock(int pMode, int px, int py, int pz) {
        if (!owner.isEqualTile()) {
            return false;
        }

        TileEntityBreeder tile = (TileEntityBreeder)owner.maidTileEntity;

        // Flag for if this operation was a success
        // Will be true if we took something out of the breeder or if we
        boolean lflag = false;

        if(owner.getSwingStatusDominant().canAttack()) {
            for(int i = tile.getSizeChickenInventory(); i < tile.func_70302_i_(); ++i) {
                ItemStack output_chickens = tile.func_70301_a(i);
                int li = output_chickens.getCount();

                if(!output_chickens.isEmpty()) {
                    // Don't bother pulling anything out if we don't have space for it
                    if(owner.getMaidInventory().hasItemStack(ItemStack.EMPTY)) {
                        // Get chickens out of the breeder

                        // We can successfully take something out of the chicken breeder
                        if(owner.getMaidInventory().addItemStackToInventory(output_chickens)) {
                            dropExpOrb(output_chickens, li - output_chickens.getCount());
                            owner.playSound("entity.item.pickup");
                            owner.setSwing(5, EnumSound.cookingOver, false);
                            owner.addMaidExperience(4.2f);
                            owner.getNextEquipItem();
                            lflag = true;

                            // TODO: Do we need to do this? Or should we be using a different method for grabbing this out
                            tile.func_70299_a(i, ItemStack.EMPTY);
                        }
                    }
                }

                // We didn't take anything out the breeder, so now lets see about putting things in
                if(!lflag) {
                    // Get the 3 input slots
                    ItemStack seed_stack = tile.func_70301_a(0);
                    ItemStack chicken1_stack = tile.func_70301_a(1);
                    ItemStack chicken2_stack = tile.func_70301_a(2);

                    // Check to see if we can insert seeds
                    if(seed_stack.isEmpty() || seed_stack.getCount() < 64) {
                        // Do we actually _have_ any seeds?
                        int seed_inventory_index = owner.getMaidInventory().getInventorySlotContainItem(ItemSeeds.class);
                        if(seed_inventory_index > -1) {
                            // We will want to fill up this slot with as many seeds as possible
                            int needed_seeds = 64 - seed_stack.getCount();

                            // Get our own seeds
                            ItemStack maidSeeds = owner.getMaidInventory().getStackInSlot(seed_inventory_index);

                            // Split off just the number of seeds that we need
                            ItemStack transfer_seeds = maidSeeds.splitStack(needed_seeds);

                            // Set the number of seeds
                            transfer_seeds.setCount(transfer_seeds.getCount() + seed_stack.getCount());
                            tile.func_70299_a(0, transfer_seeds);

                            lflag = true;
                        }
                    } else { // Now, check if we can insert chickens
                        // ChickenType -> [index1, index2, ...]
                        Map<String, List<Integer>> held_chicken_types = new HashMap<>();
                        addToHeldChickenTypes(chicken1_stack, held_chicken_types, -1);
                        addToHeldChickenTypes(chicken2_stack, held_chicken_types, -2);

                        // Iterate over manually, since we need to check each item
                        for(int j = 0; j < owner.getMaidInventory().getSizeInventory(); ++j) {
                            ItemStack slot = owner.getMaidInventory().getStackInSlot(j);
                            DataChicken data = DataChicken.getDataFromStack(slot);

                            if(!(data instanceof DataChickenModded))
                                continue;

                            /*
                            // Hacky way of getting the stats of this chicken from the DataChicken, since we have
                            //  no actual way of getting them manually
                            String stat_string = data.getDisplaySummary().substring(data.getDisplayName().length() + 1);
                            String growth_string = stat_string.substring(0, stat_string.indexOf("/"));
                            String gain_string = growth_string.substring(0, growth_string.indexOf("/"));
                            String strength_string = gain_string.substring(0, gain_string.indexOf("/"));

                            // Make sure that the chicken is not perfect yet
                            if(Integer.getInteger(growth_string) < 10 &&
                                    Integer.getInteger(gain_string) < 10 &&
                                    Integer.getInteger(strength_string) < 10)
                            {
                                addToHeldChickenTypes(slot, held_chicken_types, j);
                            }
                             */

                            if(getDataChickenGrowth((DataChickenModded)data) < 10 &&
                               getDataChickenGain((DataChickenModded)data) < 10 &&
                               getDataChickenStrength((DataChickenModded)data) < 10)
                            {
                                addToHeldChickenTypes(slot, held_chicken_types, j);
                            }
                        }

                        // Use -3 as the failure value, since -1 == slot 1 contents, and -2 == slot 3 contents
                        final int FAILURE_CHICKEN_INDEX = -3;

                        int to_breed1 = FAILURE_CHICKEN_INDEX;
                        int to_breed2 = FAILURE_CHICKEN_INDEX;

                        // Okay, now that we have each type, figure out which ones we can breed
                        for(Map.Entry<String, List<Integer>> entry : held_chicken_types.entrySet()) {
                            // Skip all lists less than 2
                            if(entry.getValue().size() < 2) {
                                continue;
                            } else if(entry.getValue().size() == 2) { // Efficiency check, so we don't sort unecessarily
                                to_breed1 = entry.getValue().get(0);
                                to_breed2 = entry.getValue().get(1);
                            } else {
                                // we want to use our _best_ chickens for breeding
                                Collections.sort(entry.getValue());
                                to_breed1 = entry.getValue().get(entry.getValue().size());
                                to_breed2 = entry.getValue().get(entry.getValue().size() - 1);
                            }
                        }

                        // If we still have the failure values, then that means we failed to find chickens to breed, so
                        //  do nothing
                        if(to_breed1 != FAILURE_CHICKEN_INDEX && to_breed2 != FAILURE_CHICKEN_INDEX) {
                            // Decide if we need to use the stacks that are already in there, or if we need to use one
                            //  from our own inventory
                            if(to_breed1 == -1)
                                tile.func_70299_a(1, chicken1_stack);
                            else if(to_breed1 == -2)
                                tile.func_70299_a(1, chicken2_stack);
                            else
                                tile.func_70299_a(1, owner.getMaidInventory().getStackInSlot(to_breed1));

                            if(to_breed2 == -1)
                                tile.func_70299_a(1, chicken1_stack);
                            else if(to_breed2 == -2)
                                tile.func_70299_a(1, chicken2_stack);
                            else
                                tile.func_70299_a(1, owner.getMaidInventory().getStackInSlot(to_breed2));

                            lflag = true;
                        }
                    }
                }
            }
        }

        return lflag;
    }


    @Override
    public void resetBlock(int pMode) {
        owner.setSneaking(false);
//		owner.setWorking(false);
    }

    protected void dropExpOrb(ItemStack pItemStack, int pCount) {
        if (!owner.world.isRemote) {
            float var3 = pItemStack.getItem().getSmeltingExperience(pItemStack);
            int var4;

            if (var3 == 0.0F) {
                pCount = 0;
            } else if (var3 < 1.0F) {
                var4 = MathHelper.floor(pCount * var3);

                if (var4 < MathHelper.ceil(pCount * var3) && (float)Math.random() < pCount * var3 - var4) {
                    ++var4;
                }

                pCount = var4 == 0 ? 1 : var4;
            }

            while (pCount > 0) {
                var4 = EntityXPOrb.getXPSplit(pCount);
                pCount -= var4;
                owner.world.spawnEntity(new EntityXPOrb(owner.world, owner.posX, owner.posY + 0.5D, owner.posZ + 0.5D, var4));
            }
        }
    }

    protected void addToHeldChickenTypes(ItemStack stack, Map<String, List<Integer>> type_map, int index) {
        if(!stack.isEmpty() && ItemChicken.class.isAssignableFrom(stack.getItem().getClass())) {
            // We are just going to assume that the chickens mod is there
            DataChicken data = DataChicken.getDataFromStack(stack);

            // Add to the mapping of chicken types that we have
            if(!type_map.containsKey(data.getChickenType()))
                type_map.put(data.getChickenType(), new ArrayList<>());
            type_map.get(data.getChickenType()).add(index);
        }
    }
}
