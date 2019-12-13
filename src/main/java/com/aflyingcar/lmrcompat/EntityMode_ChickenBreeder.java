package com.aflyingcar.lmrcompat;

import com.setycz.chickens.entity.EntityChickensChicken;
import com.setycz.chickens.registry.ChickensRegistry;
import com.setycz.chickens.registry.ChickensRegistryItem;
import com.timwoodcreates.roost.data.DataChicken;
import com.timwoodcreates.roost.data.DataChickenModded;
import com.timwoodcreates.roost.item.ItemChicken;
import com.timwoodcreates.roost.tileentity.TileEntityBreeder;
import com.timwoodcreates.roost.tileentity.TileEntityChickenContainer;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.util.*;

public class EntityMode_ChickenBreeder extends EntityModeBlockBase {
    public static final int MMODE_CHICKENS = 0x0091;

    public static class ChickenData {
        public String type;
        public int index;
        public ItemStack stack;
        public int sum;
        public double stddev;

        int gain;
        int strength;
        int growth;

        public ChickenData(String newType, int newIndex, ItemStack newStack, int newSum, double newStddev, int newGain, int newStrength, int newGrowth) {
            type = newType;
            index = newIndex;
            stack = newStack;
            sum = newSum;
            stddev = newStddev;
            gain = newGain;
            strength = newStrength;
            growth = newGrowth;
        }

        public String toString() {
            return "{" + type + "," + index + "," + stack + "," + sum + "," + stddev + "," + gain + "," + strength + "," + growth + "}";
        }
    }

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

    Map<String, ArrayList<ChickenData>> inventoryChickens = new HashMap<>();

    private static int getDataChickenGain(DataChickenModded data) {
        if(dataChickenGainField != null && data != null) {
            try {
                return (Integer)dataChickenGainField.get(data);
            } catch(IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    private static int getDataChickenGrowth(DataChickenModded data) {
        if(dataChickenGrowthField != null && data != null) {
            try {
                return (Integer)dataChickenGrowthField.get(data);
            } catch(IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    private static int getDataChickenStrength(DataChickenModded data) {
        if(dataChickenStrengthField != null && data != null) {
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

                clearInventoryDataTree();
                updateInventoryDataTree();
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

    protected ItemStack insertStackIntoSlot(TileEntityBreeder te, ItemStack stack, int slotIdx) {
        ItemStack current = te.func_70301_a(slotIdx);
        te.func_70299_a(slotIdx, stack);
        return current;
    }

    protected boolean tryInsertSeeds(TileEntityBreeder breeder, int breederSeedsIdx, int maidSeedsIdx) {
        ItemStack breederSeedsStack = breeder.func_70301_a(breederSeedsIdx);

        if(breederSeedsStack.isEmpty() || breederSeedsStack.getCount() < 5) {
            if(maidSeedsIdx >= 0) {
                ItemStack maidSeedsStack = owner.getMaidInventory().func_70301_a(maidSeedsIdx);

                if(breederSeedsStack.isEmpty()) {
                    breeder.func_70299_a(breederSeedsIdx, new ItemStack(Items.WHEAT_SEEDS));
                } else {
                    breederSeedsStack.grow(1);
                }

                maidSeedsStack.shrink(1);
                if(maidSeedsStack.getCount() <= 0)
                    maidSeedsStack = ItemStack.EMPTY;
            } else {
                return false; // TODO: Should we return early??
            }
        }

        return true;
    }

    protected boolean removeOneOutputStack(TileEntityBreeder breeder) {
        int i = breeder.getSizeChickenInventory() + 1;
        int maxInventorySize = breeder.func_70302_i_();
        for(; i < maxInventorySize; ++i) {
            ItemStack outputChickenStack = breeder.func_70301_a(i);
            if(!outputChickenStack.isEmpty() && owner.getMaidInventory().addItemStackToInventory(outputChickenStack)) {
                breeder.func_70299_a(i, ItemStack.EMPTY);
                owner.playSound("entity.item.pickup");
                owner.setSwing(5, EnumSound.cookingOver, false);
                owner.addMaidExperience(0.25f);
                break;
            }
        }
        return i >= maxInventorySize;
    }

    protected boolean isBreederOutputEmpty(TileEntityBreeder breeder) {
        int i = breeder.getSizeChickenInventory() + 1;
        int maxInventorySize = breeder.func_70302_i_();
        for(; i < maxInventorySize; ++i) {
            if(!isSlotEmpty(breeder, i)) return false;
        }

        return true;
    }

    protected boolean isSlotEmpty(TileEntityBreeder breeder, int slotIdx) {
        return breeder.func_70301_a(slotIdx).isEmpty();
    }

    protected boolean haveBetterChicken(TileEntityBreeder breeder, int slotIdx) {
        return haveBetterChicken(breeder.func_70301_a(slotIdx));
    }

    protected boolean haveBetterChicken(ItemStack stack) {
        if(stack.isEmpty()) return true;

        if(stack.getItem() instanceof ItemChicken) {
            ChickenData data = createChickenDataFrom(stack, -1);

            // We have a better chicken if we already have a max stack of top-tier chickens
            //  And if we have another type of chicken
            if(data.sum == 30 && data.stack.getCount() == data.stack.getMaxStackSize() && inventoryChickens.size() > 1)
                return true;

            // System.out.println("Stack=(\"" + data.type + "\",num=" + stack.getCount() + ",sum=" + data.sum + ",stddev=" + data.stddev + ")");
            if(inventoryChickens.containsKey(data.type) && !inventoryChickens.get(data.type).isEmpty()) {
                ChickenData topData = inventoryChickens.get(data.type).get(0);

                // If we have this exact same stack in our inventory, then say we do have a better chicken, as we might
                //  try to combine them later
                int topGain = topData.gain;
                int topGrowth = topData.growth;
                int topStrength = topData.strength;
                if(topGain == data.gain && topGrowth == data.growth && topStrength == data.strength) {
                    return true;
                }

                int topSum = topData.sum;
                double topStddev = topData.stddev;
                // System.out.println("IC(0)=(\"" + data.type + "\",num=" + inventoryChickens.get(data.type).get(0).stack.getCount() + ",sum=" + inventoryChickens.get(data.type).get(0).sum + ",stddev=" + inventoryChickens.get(data.type).get(0).stddev + ")");
                return topSum > data.sum || (topSum == data.sum && topStddev > data.stddev);
            }
        }

        return false;
    }

    /*
    protected boolean canCrossBreedChickens(ChickenData data1, ChickenData data2) {
        ChickensRegistryItem cri1 = getDataChickenChicken(data1);
        ChickensRegistryItem cri2 = getDataChickenChicken(data2);

        return ChickensRegistry.getRandomChild(cri1, cri2) != null;
    }

    protected String getCrossBreedResult(ChickenData data1, ChickenData data2) {
        ChickensRegistryItem cri1 = getDataChickenChicken(data1);
        ChickensRegistryItem cri2 = getDataChickenChicken(data2);

        return ChickensRegistry.getRandomChild(cri1, cri2).toString();
    }
     */

    protected boolean tryInsertChicken(TileEntityBreeder breeder, int slotIdx) {
        ItemStack slot = breeder.func_70301_a(slotIdx);

        for(Map.Entry<String, ArrayList<ChickenData>> entry : inventoryChickens.entrySet()) {
            // We can only insert this type of chicken if there is more than one of it
            if(entry.getValue().size() > 0 && (entry.getValue().get(0).stack.getCount() >= 2 || entry.getValue().size() > 1)) {
                ChickenData data = entry.getValue().get(0);

                // System.out.println(entry.getKey() + ":");

                // If we already have a full stack of this type of chicken, then don't bother breeding this type
                if(data.sum == 30 && data.stack.getCount() >= data.stack.getMaxStackSize()) {
                    continue;
                }

                if(!slot.isEmpty()) {
                    ChickenData slotData = createChickenDataFrom(slot, -1);

                    // System.out.println(slotData.type + ": (" + slotData.gain + "," + slotData.growth + "," + slotData.strength + ")");
                    // System.out.println(data.type + ": (" + data.gain + "," + data.growth + "," + data.strength + ")");

                    // Only combine the stacks if all attributes of a chicken are the same
                    if(slotData.type.equals(data.type) && slotData.gain == data.gain && slotData.growth == data.growth && slotData.strength == data.strength) {
                        // System.out.println("We should combine these stacks!");
                        Util.combineStacks(slotData.stack, data.stack);
                        return true;
                    }

                    // No point in breeding with this stack over the other one, so just stick with what we currently have
                    if(slotData.type.equals(data.type) && slotData.sum == data.sum && slotData.stddev == data.stddev) {
                        return true;
                    }
                }

                if(data.stack.getCount() > 1) {
                    // System.out.println("stack.getCount() == " + data.stack.getCount());
                    ItemStack toInsert = data.stack.splitStack(data.stack.getCount() / 2);
                    // System.out.println("toInsert.getCount() == " + toInsert.getCount());

                    if(slot.isEmpty() || owner.getMaidInventory().addItemStackToInventory(slot)) {
                        breeder.func_70299_a(slotIdx, toInsert);
                        entry.getValue().remove(0);
                        return true;
                    } else {
                        data.stack.grow(toInsert.getCount());
                        continue;
                    }
                } else {
                    breeder.func_70299_a(slotIdx, data.stack);
                    owner.getMaidInventory().setInventorySlotContents(data.index, slot);
                    entry.getValue().remove(0);
                    return true;
                }
            }
        }

        return false;
    }

    public enum State {
        WAIT,
        SEEDS,
        OUTPUT,
        INSERT,
        INSERT_LEFT,
        INSERT_RIGHT
    }

    private State prevState = State.OUTPUT;
    private State nextState = State.OUTPUT;
    private int insertedSeedCount;

    protected void setNextState(State newState) {
        prevState = nextState;
        nextState = newState;
    }

    @Override
    public boolean executeBlock(int pMode, int px, int py, int pz) {
        TileEntityBreeder breeder = (TileEntityBreeder)owner.maidTileEntity;

        if(breeder == null || owner.world.getTileEntity(new BlockPos(px, py, pz)) != breeder)
            return false;

        SwingStatus swingStatus = owner.getSwingStatusDominant();

        final int chicken1Slot = 0;
        final int chicken2Slot = 1;
        final int seedsSlot = 2;

        // What to return from this function
        boolean result = true;

        if(swingStatus.canAttack()) {
            switch(nextState) {
                case WAIT:
                    if(!isBreederOutputEmpty(breeder)) {
                        setNextState(State.OUTPUT);
                    } else if(isSlotEmpty(breeder, seedsSlot) || breeder.func_70301_a(seedsSlot).getCount() < 5) {
                        setNextState(State.SEEDS);
                    } else {
                        clearInventoryDataTree();
                        updateInventoryDataTree();

                        // Handle the empty ones first
                        if(isSlotEmpty(breeder, chicken1Slot)) {
                            if(haveBetterChicken(breeder, chicken1Slot)) {
                                setNextState(State.INSERT_LEFT);
                            } else if(haveBetterChicken(breeder, chicken2Slot)) {
                                setNextState(State.INSERT_RIGHT);
                            }
                        } else {
                            if(haveBetterChicken(breeder, chicken2Slot)) {
                                setNextState(State.INSERT_RIGHT);
                            } else if(haveBetterChicken(breeder, chicken1Slot)) {
                                setNextState(State.INSERT_LEFT);
                            }
                        }
                    }
                break;
                case SEEDS:
                    // System.out.println("Insert seeds");
                    // Insert 1 seed
                    result = tryInsertSeeds(breeder, seedsSlot, owner.getMaidInventory().getInventorySlotContainItem(Items.WHEAT_SEEDS));

                    if(result) insertedSeedCount++;

                    if(insertedSeedCount >= 5) {
                        insertedSeedCount = 0;
                        // setNextState(State.OUTPUT);
                    }
                    setNextState(State.WAIT);
                    break;
                case OUTPUT:
                    // Remove one chicken stack
                    // System.out.println("Remove chickens from output");

                    // If we removed all of the chicken stacks from the output, then switch over to INSERT state
                    //  otherwise, keep removing stacks
                    removeOneOutputStack(breeder);

                    // Go back to the WAIT state
                    setNextState(State.WAIT);

                    break;
                case INSERT:
                    /*
                    System.out.println("Insert chickens");

                    // Rebuild Chicken DataBase
                    if(prevState != State.INSERT) {
                        clearInventoryDataTree();
                        updateInventoryDataTree();
                    }

                    // insert two chickens
                    tryInsertChicken(breeder, chicken1Slot, chicken2Slot);

                    setNextState(State.SEEDS);
                    break;
                     */
                case INSERT_LEFT:
                    // System.out.println("Insert chickens (left slot)");

                    tryInsertChicken(breeder, chicken1Slot);
                    setNextState(State.WAIT);
                    break;
                case INSERT_RIGHT:
                    // System.out.println("Insert chickens (right slot)");

                    tryInsertChicken(breeder, chicken2Slot);
                    setNextState(State.WAIT);
                    break;
            }
        }

        return result;
    }

    // @Override
    public boolean executeBlockOld(int pMode, int px, int py, int pz) {
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

            // Grab the completed chickens
            if(!lflag) {
                for(int i = breeder.getSizeChickenInventory() + 1; i < breeder.func_70302_i_(); ++i) {
                    ItemStack outputChickenStack = breeder.func_70301_a(i);
                    if(!outputChickenStack.isEmpty() && owner.getMaidInventory().addItemStackToInventory(outputChickenStack)) {
                        breeder.func_70299_a(i, ItemStack.EMPTY);
                        owner.playSound("entity.item.pickup");
                        owner.setSwing(5, EnumSound.cookingOver, false);
                        owner.addMaidExperience(0.25f);
                        clearInventoryDataTree();
                        updateInventoryDataTree(); // We've modified the items in our inventory, so we should rebuild the tree now
                        lflag = true;
                    }
                }

                /*if(!lflag) {
                    owner.getNextEquipItem();
                }*/
            }

            // Input new chickens into the chicken breeder
            if(!lflag) {
                // System.out.println("Inputting new chickens!");
                clearInventoryDataTree();
                updateInventoryDataTree(); // We've modified the items in our inventory, so we should rebuild the tree now

                ItemStack slot1 = breeder.func_70301_a(chicken1Slot);
                ItemStack slot2 = breeder.func_70301_a(chicken2Slot);

                // Okay, now we have the chickens sorted properly
                for(Map.Entry<String, ArrayList<ChickenData>> entry : inventoryChickens.entrySet()) {
                    ChickenData firstFound = null;
                    // System.out.println(entry.getKey() + ":");

                    // Every type will be mapped to a list of at least one _stack_
                    for(ChickenData data : entry.getValue()) {
                        // System.out.println("\t" + data);

                        // Use the best chickens first if we can
                        // But if we already have a full stack of 10 10 10 chickens, then ignore this type
                        if(data.stack.getCount() == data.stack.getMaxStackSize() && data.sum == 30) {
                            // System.out.println("Skipping chickens of type " + entry.getKey() + " as we have a full stack of 10 10 10.");
                            break;
                        } else if(data.stack.getCount() > 1) {
                            // If we could add it to our inventory
                            if(owner.getMaidInventory().addItemStackToInventory(slot1)) {
                                // System.out.println("Successfully moved contents of slot1 into our inventory!");
                                ItemStack half;

                                // Split the stack in half before placing it in the breeder
                                //  But if we found one earlier, then we'll go ahead and use the entire stack
                                if(firstFound == null) {
                                    // System.out.println("Splitting stack at index #" + data.index + ". New stack of size: " + data.stack.getCount() / 2);
                                    half = data.stack.splitStack(data.stack.getCount() / 2);
                                } else {
                                    // System.out.println("Using this whole stack and a previous one we found");
                                    half = firstFound.stack;
                                }

                                // Place both halves of the stack into the breeder
                                breeder.func_70299_a(chicken1Slot, data.stack);
                                breeder.func_70299_a(chicken2Slot, half);

                                // We just removed something from our inventory, so it is safe to just overwrite that slot
                                owner.getMaidInventory().setInventorySlotContents(data.index, slot2);
                                clearInventoryDataTree();
                                updateInventoryDataTree();

                                lflag = true;
                            }
                        } else {
                            // If we have only found one chicken, and this is the first time, then that means we didn't
                            //  find a better stack earlier, so we can just continue on with this
                            if(firstFound == null) {
                                // System.out.println("Stack is of size 1, going to save it and use it with the next stack of similar type");
                                firstFound = data;
                            } else {
                                // System.out.println("Stack is of size 1, and we have another stack of size 1 that we found previously. Going to use both now.");
                                // If we've hit this case, then we've hit 2 stacks in a row of size 1
                                breeder.func_70299_a(chicken1Slot, firstFound.stack);
                                breeder.func_70299_a(chicken2Slot, data.stack);

                                owner.getMaidInventory().setInventorySlotContents(firstFound.index, slot1);
                                owner.getMaidInventory().setInventorySlotContents(data.index, slot1);
                                clearInventoryDataTree();
                                updateInventoryDataTree();
                            }
                        }
                    }

                    if(lflag)
                        break;
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

    private double calcMean(int[] values) {
        double sum = 0;
        for(int v : values) sum += v;

        return sum / (double)values.length;
    }

    private double calcStandardDev(int[] values) {
        double mean = calcMean(values);

        double sum = 0;
        for(int v : values) sum = sum + ((v - mean) * (v - mean));

        return Math.sqrt(Math.abs(sum / (values.length - 1)));
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

    protected void clearInventoryDataTree() {
        inventoryChickens.clear();
    }

    protected ChickenData createChickenDataFrom(ItemStack stack, int index) {
        DataChickenModded data = (DataChickenModded)DataChicken.getDataFromStack(stack);
        int gain = getDataChickenGain(data);
        int growth = getDataChickenGrowth(data);
        int strength = getDataChickenStrength(data);

        int sum = gain + growth + strength;
        double stddev = calcStandardDev(new int[]{gain, growth, strength});

        String type = data.getChickenType();

        return new ChickenData(type, index, stack, sum, stddev, gain, growth, strength);
    }

    protected void updateInventoryDataTree() {
        for(int i = 0; i < owner.getMaidInventory().getSizeInventory(); ++i) {
            ItemStack stack = owner.getMaidInventory().func_70301_a(i);

            // Only add Chickens to the tree
            if(!stack.isEmpty() && stack.getItem() instanceof ItemChicken) {
                ChickenData data = createChickenDataFrom(stack, i);

                // System.out.println("Stack #" + i + "=(\"" + type + "\",num=" + stack.getCount() + ",sum=" + sum + ",stddev=" + stddev + ")");

                if(!inventoryChickens.containsKey(data.type)) {
                    inventoryChickens.put(data.type, new ArrayList<>());
                }
                inventoryChickens.get(data.type).add(data);
            }
        }

        for(Map.Entry<String, ArrayList<ChickenData>> entry : inventoryChickens.entrySet()) {
            if(entry.getValue().isEmpty()) {
                inventoryChickens.remove(entry);
                continue;
            }
            Collections.sort(entry.getValue(), new Comparator<ChickenData>() {
                @Override
                public int compare(ChickenData t1, ChickenData t2) {
                    if(t1.sum < t2.sum) {
                        return 1;
                    } else if(t1.sum == t2.sum) {
                        if(t1.stddev < t2.stddev) {
                            return 1;
                        } else if(t1.stddev > t2.stddev) {
                            return -1;
                        }
                        return 0;
                    }
                    return -1;
                }
            });
        }

        // System.out.println("inventoryChickens.size() == " + inventoryChickens.size());
    }
}
