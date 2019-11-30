package com.aflyingcar.lmrcompat.entity.ai;

import net.blacklab.lmr.entity.EntityLittleMaid;
import net.blacklab.lmr.entity.ai.EntityAILMFollowOwner;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

public class EntityAILMCFollowButDontWatchOwner extends EntityAILMFollowOwner {
    private EntityLittleMaid theMaid;

    public EntityAILMCFollowButDontWatchOwner(EntityLittleMaid par1EntityLittleMaid, float pSpeed, double pSprintDistSQ) {
        super(par1EntityLittleMaid, pSpeed, pSprintDistSQ);

        theMaid = par1EntityLittleMaid;
    }

    @Override
    public void updateTask() {
        super.updateTask();

        EntityLivingBase target = theMaid.getAttackTarget();
        if(target == null)
            target = theMaid.getRevengeTarget();

        if(target != null)
            theMaid.getLookHelper().setLookPositionWithEntity(target, theMaid.getHorizontalFaceSpeed(), theMaid.getVerticalFaceSpeed());
    }
}
