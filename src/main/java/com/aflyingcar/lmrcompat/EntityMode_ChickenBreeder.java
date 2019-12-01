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
import net.blacklab.lmr.util.SwingStatus;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
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
                    Util.grantAchievement(pentityplayer, "chickenbreeder");
                    // AchievementsLMRE.grantAdvancement(pentityplayer, "chickenbreeder");
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
    public boolean isTriggerItem(int mode, ItemStack stack) {
        if(stack.isEmpty()) return false;

        return stack.getItem() instanceof ItemSeeds;
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
                (((TileEntityBreeder)owner.maidTileEntity).func_174887_a_(0) > 0 ||
                        (!owner.getCurrentEquippedItem().isEmpty()));
    }

    @Override
    public boolean checkBlock(int pMode, int px, int py, int pz) {
        if(owner.getCurrentEquippedItem().isEmpty())
            return false;

        TileEntity ltile = owner.world.getTileEntity(new BlockPos(px, py, pz));
        if (!(ltile instanceof TileEntityBreeder)) {
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
        TileEntityBreeder breeder = (TileEntityBreeder)owner.maidTileEntity;

        // getStackInSlot == func_70301_a
        // setInventorySlotContents == func_70299_a
        // getField == func_174887_a_

        if(breeder == null || owner.world.getTileEntity(new BlockPos(px, py, pz)) != breeder)
            return false;

        boolean lflag = false;
        SwingStatus swingStatus = owner.getSwingStatusDominant();

        final int chicken1Slot = 0;
        final int chicken2Slot = 1;
        final int seedsSlot = 2;

        if(!breeder.func_70301_a(seedsSlot).isEmpty() || !breeder.func_70301_a(chicken1Slot).isEmpty() || !breeder.func_70301_a(chicken2Slot).isEmpty())
            owner.setWorking(true);

        int seedsPosition = owner.getMaidInventory().getInventorySlotContainItem(Items.WHEAT_SEEDS);
        if(swingStatus.canAttack()) {
            ItemStack breederSeedsStack = breeder.func_70301_a(seedsSlot);

            if(breederSeedsStack.isEmpty() || breederSeedsStack.getCount() < 2) {
                if(seedsPosition >= 0) {
                    ItemStack maidSeedsStack = owner.getMaidInventory().func_70301_a(seedsPosition);

                    if(breederSeedsStack.isEmpty()) {
                        breeder.func_70299_a(seedsSlot, new ItemStack(Items.WHEAT_SEEDS));
                    } else {
                        breederSeedsStack.grow(1);
                    }

                    maidSeedsStack.shrink(1);
                    if(maidSeedsStack.getCount() <= 0)
                        maidSeedsStack = ItemStack.EMPTY;
                    lflag = true;
                } else {
                    return false; // TODO: Should we return early??
                }
            }
        } else {
            lflag = true;
        }

        if(breeder.func_174887_a_(0) > 0) {
            owner.setWorking(true);
            lflag = true;
        }

        return lflag;
    }

    @Override
    public void startBlock(int pMode) {
        // TODO
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
