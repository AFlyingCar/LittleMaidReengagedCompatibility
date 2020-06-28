package com.aflyingcar.lmrcompat;

import com.aflyingcar.lmrcompat.plugins.vampirism.EntityConvertedLittleMaid;
import de.teamlapen.vampirism.api.entity.vampire.IVampire;
import de.teamlapen.vampirism.entity.vampire.EntityBasicVampire;
import de.teamlapen.vampirism.items.ItemStake;
import net.blacklab.lmr.entity.EntityLittleMaid;
import net.blacklab.lmr.entity.ai.EntityAILMHurtByTarget;
import net.blacklab.lmr.entity.ai.EntityAILMNearestAttackableTarget;
import net.blacklab.lmr.entity.mode.EntityModeBase;
import net.blacklab.lmr.util.TriggerSelect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import techguns.items.guns.GenericGun;

import java.util.UUID;

public class EntityMode_VampireHunter extends EntityModeBase {
    public static final int MMODE_VAMPIREHUNTER = 0x0092;

    public EntityMode_VampireHunter(EntityLittleMaid pEntity) {
        super(pEntity);
    }

    @Override
    public int priority() {
        // Same as Fencer
        return 3000;
    }


    @Override
    public void init() {
        TriggerSelect.appendTriggerItem((UUID)null, "Stake", "");
    }

    @Override
    public void addEntityMode(EntityAITasks defaultMove, EntityAITasks defaultTargeting) {
        EntityAITasks[] ltasks = new EntityAITasks[]{defaultMove, new EntityAITasks(this.owner.aiProfiler)};
        ltasks[1].addTask(3, new EntityAILMHurtByTarget(this.owner, true));
        ltasks[1].addTask(4, new EntityAILMNearestAttackableTarget(this.owner, EntityLivingBase.class, 0, true));
        this.owner.addMaidMode(ltasks, "VampireHunter", MMODE_VAMPIREHUNTER);
    }

    @Override
    public boolean changeMode(EntityPlayer player) {
        ItemStack litemstack = owner.getHandSlotForModeChange();

        if(!litemstack.isEmpty()) {
            if(litemstack.getItem() instanceof ItemStake ||
                    TriggerSelect.checkTrigger(owner.getMaidMasterUUID(), "VampireHunter", litemstack.getItem()))
            {
                owner.setMaidMode("VampireHunter");

                Util.grantAchievement(player, "maidVampireHunter");

                return true;
            }
        }

        return false;
    }

    public boolean setMode(int pMode) {
        if(pMode == MMODE_VAMPIREHUNTER) {
            this.owner.aiAttack.setEnable(false);
            this.owner.setBloodsuck(false);
            return true;
        }
        return false;
    }

    public int getNextEquipItem(int pMode) {
        int li = super.getNextEquipItem(pMode);
        if(li >= 0) {
            return li;
        } else {
            if(pMode == MMODE_VAMPIREHUNTER) {
                for(li = 0; li < owner.getMaidInventory().getSizeInventory() - 1; ++li) {
                    ItemStack item = owner.getMaidInventory().getStackInSlot(li);
                    if(!item.isEmpty() && isTriggerItem(pMode, item)) {
                        return li;
                    }
                }
            }
        }

        return -1;
    }


    @Override
    protected boolean isTriggerItem(int pMode, ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        return itemStack.getItem() instanceof ItemStake || TriggerSelect.checkTrigger(owner.getMaidMasterUUID(), "Stake", itemStack.getItem());
    }

    @Override
    public boolean checkItemStack(ItemStack stack) {
        if(stack == null || stack.isEmpty()) {
            return false;
        }

        return stack.getItem() instanceof ItemStake || TriggerSelect.checkTrigger(owner.getMaidMasterUUID(), "Stake", stack.getItem());
    }

    @Override
    public boolean isSearchEntity() {
        return true;
    }

    @Override
    public boolean checkEntity(int pMode, Entity entity) {
        if(pMode != MMODE_VAMPIREHUNTER) return false;

        if(entity instanceof IVampire) {
            if(entity instanceof EntityConvertedLittleMaid) {
                return ((EntityConvertedLittleMaid)entity).getMaidMasterUUID() != owner.getMaidMasterUUID();
            }
            return true;
        }

        return false;
    }
}
