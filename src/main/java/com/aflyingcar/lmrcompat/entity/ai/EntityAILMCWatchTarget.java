package com.aflyingcar.lmrcompat.entity.ai;

import net.blacklab.lmr.entity.ai.EntityAILMWatchClosest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;

public class EntityAILMCWatchTarget extends EntityAILMWatchClosest {
    public EntityAILMCWatchTarget(EntityLiving entitylivingIn, Class<? extends Entity> watchTargetClass, float maxDistance, float chanceIn) {
        super(entitylivingIn, watchTargetClass, maxDistance, chanceIn);
    }

    public EntityAILMCWatchTarget(EntityLiving entitylivingIn, Class<? extends Entity> watchTargetClass, float maxDistance) {
        super(entitylivingIn, watchTargetClass, maxDistance);
    }

    @Override
    public boolean shouldExecute() {
        if(entity.getAttackTarget() != null) {
            return super.shouldExecute() && closestEntity == entity.getAttackTarget();
        } else {
            return super.shouldExecute();
        }
    }

    @Override
    public boolean shouldContinueExecuting() {
        if(entity.getAttackTarget() != null) {
            return super.shouldContinueExecuting() && closestEntity == entity.getAttackTarget();
        } else {
            return super.shouldContinueExecuting();
        }
    }
}
