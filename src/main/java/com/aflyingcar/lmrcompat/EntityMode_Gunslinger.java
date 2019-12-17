package com.aflyingcar.lmrcompat;


import com.aflyingcar.lmrcompat.entity.ai.EntityAILMCFollowButDontWatchOwner;
import com.aflyingcar.lmrcompat.entity.ai.EntityAILMCWatchTarget;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.blacklab.lmr.util.Statics;
import net.blacklab.lmr.achievements.AchievementsLMRE;
import net.blacklab.lmr.entity.EntityLittleMaid;
import net.blacklab.lmr.entity.ai.EntityAILMHurtByTarget;
import net.blacklab.lmr.entity.ai.EntityAILMNearestAttackableTarget;
import net.blacklab.lmr.entity.mode.EntityModeBase;
import net.blacklab.lmr.inventory.InventoryLittleMaid;
import net.blacklab.lmr.util.EnumSound;
import net.blacklab.lmr.util.TriggerSelect;
import net.blacklab.lmr.util.helper.MaidHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.client.audio.Sound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.FakePlayer;
import techguns.TGItems;
import techguns.TGSounds;
import techguns.api.capabilities.AttackTime;
import techguns.api.guns.GunHandType;
import techguns.client.audio.TGSoundCategory;
import techguns.items.guns.GenericGun;
import techguns.items.guns.ammo.AmmoType;
import techguns.items.guns.ammo.AmmoVariant;
import techguns.util.InventoryUtil;
import techguns.util.SoundUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

public class EntityMode_Gunslinger extends EntityModeBase {
    public static final int MMODE_GUNSLINGER = 0x0090;

    private static Field AI_AttackTimeField;
    private static Field minFiretimeField;
    private static Field reloadTimeField;
    private static Field reloadSoundField;

    private static final int MIN_BURST_COUNT = 2;
    private static final int MAX_BURST_COUNT = 5;

    static {
        try {
            AI_AttackTimeField = GenericGun.class.getDeclaredField("AI_attackTime");
            AI_AttackTimeField.setAccessible(true);

            System.out.println("Successfully loaded AI_AttackTime field!");
        } catch(NoSuchFieldException e) {
            System.out.println("Failed to load AI_AttackTime field!");
            e.printStackTrace();
        }

        try {
            reloadTimeField = GenericGun.class.getDeclaredField("reloadtime");
            reloadTimeField.setAccessible(true);

            System.out.println("Successfully loaded reloadtime field!");
        } catch(NoSuchFieldException e) {
            System.out.println("Failed to load reloadtime field!");
            e.printStackTrace();
        }

        try {
            minFiretimeField = GenericGun.class.getDeclaredField("minFiretime");
            minFiretimeField.setAccessible(true);

            System.out.println("Successfully loaded minFiretime field!");
        } catch(NoSuchFieldException e) {
            System.out.println("Failed to load minFiretime field!");
            e.printStackTrace();
        }

        try {
            reloadSoundField = GenericGun.class.getDeclaredField("reloadsound");
            reloadSoundField.setAccessible(true);

            System.out.println("Successfully loaded reloadsound field!");
        } catch(NoSuchFieldException e) {
            System.out.println("Failed to load reloadsound field!");
            e.printStackTrace();
        }
    }

    // First == main hand, Second == Offhand
    private AttackTime[] attackTimes = { new AttackTime(), new AttackTime()};
    private int reloadingTimer = 0;
    private int attackTimer = 0;
    private int burst = 0; // Amount of times that can be fired for automatic weapons before a pause
    private int burstCount = 0;

    private int getNextBurstCount() {
        if(owner != null && owner.getRNG() != null)
            return owner.getRNG().nextInt(MAX_BURST_COUNT - MIN_BURST_COUNT + 1) + MIN_BURST_COUNT;
        else
            return 3;
    }

    public EntityMode_Gunslinger(EntityLittleMaid pEntity) {
        super(pEntity);
        isAnytimeUpdate = true;

        // System.out.println("EntityMode_Gunslinger()");

        burstCount = getNextBurstCount();

        // Util.loadResources();
    }

    @Override
    public int priority() {
        return 3200; // Same as the Archer
    }

    @Override
    public void init() {
        TriggerSelect.appendTriggerItem((UUID)null, "Gun", "");
    }

    @Override
    public void addEntityMode(EntityAITasks defaultMove, EntityAITasks defaultTargeting) {
        // Gunslinger:0x0090
        EntityAITasks[] ltasks = new EntityAITasks[2];
        ltasks[0] = new EntityAITasks(owner.aiProfiler);
        ltasks[1] = new EntityAITasks(owner.aiProfiler);

        // We need to re-add all of the default ones so we don't have the gunslinger leaping at enemies

        // default
        ltasks[0].addTask(1, owner.aiSwiming);
        ltasks[0].addTask(2, owner.getAISit());
//		ltasks[0].addTask(3, aiJumpTo);
        ltasks[0].addTask(4, owner.aiFindBlock);
        ltasks[0].addTask(5, owner.aiAttack);
        ltasks[0].addTask(6, owner.aiShooting);
        //ltasks[0].addTask(8, aiPanic);
        ltasks[0].addTask(10, owner.aiBeg);
        ltasks[0].addTask(11, owner.aiBegMove);
        // TODO Needed?
//		ltasks[0].addTask(20, aiAvoidPlayer);
        ltasks[0].addTask(21, owner.aiFreeRain);
        ltasks[0].addTask(22, owner.aiCollectItem);
        // 移動用AI
        ltasks[0].addTask(30, owner.aiTracer);
        // ltasks[0].addTask(31, owner.aiFollow);
        ltasks[0].addTask(31, new EntityAILMCFollowButDontWatchOwner(owner, 1.0F, 81D));
        ltasks[0].addTask(32, owner.aiWander);
        // ltasks[0].addTask(33, new EntityAILeapAtTarget(this, 0.3F));
        // Mutexの影響しない特殊行動
        ltasks[0].addTask(40, owner.aiCloseDoor);
        ltasks[0].addTask(41, owner.aiOpenDoor);
        ltasks[0].addTask(42, owner.aiRestrictRain);
        // 首の動き単独
        ltasks[0].addTask(51, new EntityAILMCWatchTarget(owner, EntityLivingBase.class, 10F));
        // ltasks[0].addTask(52, owner.aiWatchClosest);
        // ltasks[0].addTask(52, new EntityAILookIdle(owner));

//		ltasks[1].addTask(1, new EntityAIOwnerHurtByTarget(owner));
//		ltasks[1].addTask(2, new EntityAIOwnerHurtTarget(owner));
        ltasks[1].addTask(1, new EntityAILMHurtByTarget(owner, true));
        ltasks[1].addTask(2, new EntityAILMNearestAttackableTarget(owner, EntityLivingBase.class, 0, true));

        owner.addMaidMode(ltasks, "Gunslinger", MMODE_GUNSLINGER);
    }

    protected int getAIAttackTime() {
        if(AI_AttackTimeField == null) return 0;

        ItemStack gunStack = owner.getMaidInventory().getStackInSlot(InventoryLittleMaid.handInventoryOffset);
        GenericGun gun;

        // First lets make sure that whatever we are holding _is_ in fact a gun
        if(gunStack.getItem() instanceof GenericGun) {
            gun = (GenericGun)gunStack.getItem();
        } else {
            return 0;
        }

        try {
            // System.out.println("getAIAttackTime() -> " + AI_AttackTimeField.get(gun));
            return (Integer)AI_AttackTimeField.get(gun);
        } catch(IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    protected SoundEvent getReloadSound(GenericGun gun) {
        if(reloadSoundField == null) return null;

        try {
            return (SoundEvent)reloadSoundField.get(gun);
        } catch(IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected int getReloadTime() {
        if(reloadTimeField == null) return 0;

        ItemStack gunStack = owner.getMaidInventory().getStackInSlot(InventoryLittleMaid.handInventoryOffset);
        GenericGun gun;

        // First lets make sure that whatever we are holding _is_ in fact a gun
        if(gunStack.getItem() instanceof GenericGun) {
            gun = (GenericGun)gunStack.getItem();
        } else {
            return 0;
        }

        try {
            // System.out.println("getReloadTime() -> " + reloadTimeField.get(gun));
            return (Integer)reloadTimeField.get(gun);
        } catch(IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    protected int getMinFiretime() {
        // The default min firetime of GenericGun
        if(minFiretimeField == null) return 4;

        ItemStack gunStack = owner.getMaidInventory().getStackInSlot(InventoryLittleMaid.handInventoryOffset);
        GenericGun gun;

        // First lets make sure that whatever we are holding _is_ in fact a gun
        if(gunStack.getItem() instanceof GenericGun) {
            gun = (GenericGun)gunStack.getItem();
        } else {
            return 4;
        }

        try {
            // System.out.println("getMinFiretime() -> " + minFiretimeField.get(gun));
            return (Integer)minFiretimeField.get(gun);
        } catch(IllegalAccessException e) {
            e.printStackTrace();
            return 4;
        }
    }

    public boolean isReloading() {
        return reloadingTimer > 0;
    }

    public boolean isAttackCooldown() {
        return attackTimer > 0;
    }

    @Override
    public boolean changeMode(EntityPlayer player) {
        ItemStack litemstack = owner.getHandSlotForModeChange();

        if(!litemstack.isEmpty()) {
            if(litemstack.getItem() instanceof GenericGun ||
               TriggerSelect.checkTrigger(owner.getMaidMasterUUID(), "Gun", litemstack.getItem()))
            {
                // System.out.println("Mode has been changed to gunslinger!");

                owner.setMaidMode("Gunslinger");

                Util.grantAchievement(player, "gunslinger");

                return true;
            }
        }

        return false;
    }

    public boolean setMode(int pMode) {
        if(pMode == MMODE_GUNSLINGER) {
            this.owner.aiAttack.setEnable(false);
            this.owner.aiShooting.setEnable(true);
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
            if(pMode == MMODE_GUNSLINGER) {
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
        return itemStack.getItem() instanceof GenericGun || TriggerSelect.checkTrigger(owner.getMaidMasterUUID(), "Gun", itemStack.getItem());
    }

    @Override
    public boolean checkItemStack(ItemStack stack) {
        if(stack == null || stack.isEmpty()) {
            return false;
        }

        return stack.getItem() instanceof GenericGun || TriggerSelect.checkTrigger(owner.getMaidMasterUUID(), "Gun", stack.getItem());
    }

    @Override
    public boolean isSearchEntity() {
        return true;
    }

    @Override
    public boolean checkEntity(int pMode, Entity entity) {
        if(pMode != MMODE_GUNSLINGER) return false;

        ItemStack gunStack = owner.getMaidInventory().getStackInSlot(InventoryLittleMaid.handInventoryOffset);
        GenericGun gun;

        // First lets make sure that whatever we are holding _is_ in fact a gun
        if(gunStack.getItem() instanceof GenericGun) {
            gun = (GenericGun)gunStack.getItem();
        } else {
            return false;
        }

        // Is the gun loaded? If not, do we have any valid ammo in our inventory?
        if(gun.getAmmoLeft(gunStack) <= 0 && !hasAmmoInInventory(gun.getAmmoType().getAmmo(gun.getCurrentAmmoVariant(gunStack)))) {
            return false;
        } else if(!MaidHelper.isTargetReachable(this.owner, entity, gun.getAI_attackRange())) {
            // TODO: We should check for minimum range here too (If we have a rocket launcher, don't fire it at our feet)
            return false;
        }

        return !owner.getIFF(entity);
    }

    private boolean hasAmmoInInventory(ItemStack[] ammoTypes) {
        for(ItemStack ammoType : ammoTypes) {
            if(owner.getMaidInventory().getInventorySlotContainItem(ammoType.getItem().getClass()) >= 0)
                return true;
        }

        return false;
    }

    @Override
    public void onUpdate(int pMode) {
        if(pMode == MMODE_GUNSLINGER) {
            ItemStack itemstack = owner.getMaidInventory().getStackInSlot(InventoryLittleMaid.handInventoryOffset);
            GenericGun gun;

            // We need a gun in our hand for any of this to matter
            if(itemstack.getItem() instanceof GenericGun)
                gun = (GenericGun)itemstack.getItem();
            else
                return;

            // updateGuns();
            if(owner.world.isRemote)
                owner.getWeaponStatus();

            EntityLivingBase target = owner.getAttackTarget();
            if(target == null)
                target = owner.getRevengeTarget();

            if(target != null || gun.getGunHandType() == GunHandType.TWO_HANDED) {
                owner.mstatAimeBow = true;
                owner.setMaidFlags(true, Statics.dataWatch_Flags_Aimebow);
                owner.getSwingStatusDominant().attackTime = 100;
            }

            if(target != null)
                owner.getLookHelper().setLookPosition(target.posX, target.posY + (target.getEyeHeight() / 2),
                                                      target.posZ, owner.getHorizontalFaceSpeed(), owner.getVerticalFaceSpeed());

        }
    }

    @Override
    public void updateAITick(int pMode) {
        ItemStack itemstack = owner.getMaidInventory().getStackInSlot(InventoryLittleMaid.handInventoryOffset);
        GenericGun gun = null;
        if(itemstack.getItem() instanceof GenericGun) {
            gun = (GenericGun)itemstack.getItem();
        } else {
            owner.setAttackTarget(null);
            return;
        }

        EntityLivingBase target = owner.getAttackTarget();
        if(target == null)
            target = owner.getRevengeTarget();

        boolean updateFlag = true;

        if(isReloading()) {
            reloadingTimer--;
            updateFlag = false;
        }
        if(isAttackCooldown()) {
            attackTimer--;
            updateFlag = false;
        }

        if(updateFlag && target != null &&
           !isLookingAtPlayer(gun.getAI_attackRange()) && isLookingAtTarget(gun.getAI_attackRange()))
        {
            // Only fire our gun if we were able to consume ammunition
            if(consumeAmmo(gun, itemstack)) {
                // System.out.println("Firing weapon!");
                gun.fireWeaponFromNPC(owner, 1, 1);
                attackTimer = getAttackCooldownTime(gun.getAI_attackRange(), getMinFiretime());
                // System.out.println("Maid now on cooldown. Cannot fire for another " + attackTimer + " ticks.");
            } else {
                // If we couldn't fire our gun, then stop targeting
                owner.setAttackTarget(null);
                burst = 0;
            }
        }
    }

    private int getAttackCooldownTime(float maxRange, int minFiretime) {
        int maxWaitTime = getAIAttackTime();
        float variantWaitTime = maxWaitTime / 3.0f;
        float distanceToTarget = 0;

        if(owner.getAttackTarget() != null) {
            distanceToTarget = owner.getDistance(owner.getAttackTarget());
            distanceToTarget *= distanceToTarget; // Square it
            owner.getEntitySenses().canSee(owner.getAttackTarget());
        }

        float f = MathHelper.sqrt(distanceToTarget) / maxRange;

        if(burst <= burstCount) {
            ++burst;
            return minFiretime;
        } else {
            burstCount = getNextBurstCount();
            burst = 0;
            return Math.min(MathHelper.floor(f * (maxWaitTime - variantWaitTime) + variantWaitTime), minFiretime);
        }
    }

    private boolean isLookingAtTarget(double maxRange) {
        EntityLivingBase target = owner.getAttackTarget();
        if(target == null)
            target = owner.getRevengeTarget();

        // If we don't have a target then we obviously aren't looking at them
        if(target == null) return false;

        Vec3d ownerVec3d = new Vec3d(owner.posX, owner.posY + owner.getEyeHeight(), owner.posZ);
        Vec3d lookVec3d = ownerVec3d.addVector(owner.getLookVec().x + maxRange, owner.getLookVec().y + maxRange, owner.getLookVec().z + maxRange);
        Vec3d targetVec3d = new Vec3d(target.posX, target.posY + (double)target.getEyeHeight(), target.posZ);

        // Are we looking in vaguely the same direction as the target?
        //  And can they even be seen from our perspective?
        if(lookVec3d.dotProduct(targetVec3d) >= 0 &&
           owner.world.rayTraceBlocks(ownerVec3d, targetVec3d, false, true, false) == null)
        {
            // System.out.println("" + owner.getDistance(target) + "<" + maxRange + "?");
            return owner.getDistance(target) < maxRange;
        }
        return false;
    }

    private boolean isLookingAtPlayer(double maxRange) {
        EntityPlayer maidMasterEntity = owner.getMaidMasterEntity();

        // If we don't have a maidMasterEntity then we obviously aren't looking at them
        if(maidMasterEntity == null) return false;

        Vec3d ownerVec3d = new Vec3d(owner.posX, owner.posY + owner.getEyeHeight(), owner.posZ);
        Vec3d playerVec3d = new Vec3d(maidMasterEntity.posX, maidMasterEntity.posY + (double)maidMasterEntity.getEyeHeight(), maidMasterEntity.posZ);
        Vec3d lookVec3d = ownerVec3d.addVector(owner.getLookVec().x + maxRange, owner.getLookVec().y + maxRange, owner.getLookVec().z + maxRange);

        // Don't shoot anywhere this close to the player
        // 2.0 == Safety amount
        AxisAlignedBB playerBB = maidMasterEntity.getEntityBoundingBox().grow(2.0);

        RayTraceResult result = playerBB.calculateIntercept(ownerVec3d, ownerVec3d.add(lookVec3d));

        // If we didn't collide with the players bounding box, then we obviously aren't looking at them
        if(result == null) {
            // System.out.println("We did not collide with the player's hitbox!");
            return false;
        }

        // If we are inside of the players bounding box, and still hit the box, then
        if(playerBB.contains(ownerVec3d)) {
            Vec3d ownerToPlayer = playerVec3d.subtract(ownerVec3d);

            // System.out.println(ownerToPlayer + " dot " + lookVec3d + " = " + ownerToPlayer.dotProduct(lookVec3d));

            // If we are looking in the same direction as the player,
            return ownerToPlayer.dotProduct(lookVec3d) >= 0;
        } else {
            // If we aren't in the same cube as the player, then we will only hit them if there aren't blocks in the way
            return owner.world.rayTraceBlocks(ownerVec3d, playerVec3d, false, true, false) != null;
            // System.out.println("We are not inside of the player's hitbox!");
        }

        /*
        // If we are looking directly at the player's hitbox (or slightly around it)
        if()
        {
            System.out.println("" + owner.getDistance(maidMasterEntity) + "<" + maxRange + "?");
            // We can't see the player if they are outside the range of our gun
            //  Therefore, we are allowed to shoot if the player is outside of our range
            return true; //owner.getDistance(maidMasterEntity) < maxRange;
        }
        return false;
         */
    }

    private boolean consumeAmmo(GenericGun gun, ItemStack gunStack) {
        if(gun.getCurrentAmmo(gunStack) <= 0) {
            // System.out.println("Reloading!");
            doReload(gun, gunStack);
            reloadingTimer = getReloadTime();

            return false;
        }

        if(gun.getCurrentAmmo(gunStack) >= 1) {
            gun.useAmmo(gunStack, 1);
            return true;
        }

        return false;
    }

    // NOTE: This is mostly pulled from the NPCTurret class from TechGuns.
    //  So, basically, maids with guns are just mobile TG turrets that don't need a constant supply of power, just
    //  ammo and their salary
    private void doReload(GenericGun gun, ItemStack gunStack) {
        ItemStack[] ammo = gun.getAmmoType().getAmmo(gun.getCurrentAmmoVariant(gunStack));
        ItemStack[] emptyMag = gun.getAmmoType().getEmptyMag();

        boolean canConsumeAmmo = true;

        InventoryLittleMaid inventory = owner.getMaidInventory();

        for(int i = 0; i < emptyMag.length; ++i) {
            if(InventoryUtil.canConsumeItem(inventory.mainInventory, ammo[i], 0, inventory.mainInventory.size()) > 0) {
                // System.out.println("Can consume ammo at inventory location #" + i);
                canConsumeAmmo = false;
                break;
            }
        }

        if(canConsumeAmmo) {
            for(int i = 0; i < emptyMag.length; ++i) {
                // Attempt to consume the ammo at this inventory location
                if(InventoryUtil.consumeAmmo(inventory.mainInventory, ammo[i], 0, inventory.mainInventory.size())) {

                    // Create empty magazines in our inventory if possible, otherwise just drop them on the ground
                    if(!emptyMag[i].isEmpty()) {
                        int tooMuch = InventoryUtil.addItemToInventory(inventory.mainInventory, TGItems.newStack(emptyMag[i], 1), 0, inventory.mainInventory.size());
                        if(!owner.world.isRemote) {
                            owner.world.spawnEntity(new EntityItem(owner.world, owner.posX, owner.posY + 1, owner.posZ, TGItems.newStack(emptyMag[i], tooMuch)));
                        }
                    }

                    // If the gun has ammo in it
                    if(gun.getAmmoCount() > 1) {
                        int j = 1;
                        while(j < gun.getAmmoCount() && InventoryUtil.consumeAmmo(inventory.mainInventory, ammo[i], 0, inventory.mainInventory.size())) {
                            ++j;
                        }

                        if(i == 0) {
                            gun.reloadAmmo(gunStack, j);
                            // owner.playSound();
                            SoundEvent reloadSound = getReloadSound(gun);
                            if(reloadSound == null) {
                                reloadSound = TGSounds.PISTOL_RELOAD;
                            }

                            SoundUtil.playReloadSoundOnEntity(owner.world, owner, reloadSound, 1.0F, 1.0F, false, true, TGSoundCategory.RELOAD);
                        }
                    } else {
                        if(i == 0) {
                            gun.reloadAmmo(gunStack);
                            SoundEvent reloadSound = getReloadSound(gun);
                            if(reloadSound == null) {
                                reloadSound = TGSounds.PISTOL_RELOAD;
                            }

                            // System.out.println("Playing reload sound!");
                            SoundUtil.playReloadSoundOnEntity(owner.world, owner, reloadSound, 1.0F, 1.0F, false, true, TGSoundCategory.RELOAD);
                        }
                    }
                }
            }
        }
    }

    protected void updateGuns() {
        if(owner.getAttackTarget() == null || !owner.getAttackTarget().isEntityAlive()) {
            if(!owner.weaponReload) {
                if(owner.getMaidAvatar().isHandActive()) {
                    owner.getMaidAvatar().stopActiveHand();
                }
            } else {
                owner.mstatAimeBow = true;
            }
        }

        if(owner.weaponReload && !owner.getMaidAvatar().isHandActive()) {
            owner.getMaidInventory().getCurrentItem().useItemRightClick(owner.world, owner.getMaidAvatar(), EnumHand.MAIN_HAND);
            owner.mstatAimeBow = true;
        }
    }

    @Override
    public double getDistanceToSearchTargets() {
        ItemStack itemstack = owner.getMaidInventory().getStackInSlot(InventoryLittleMaid.handInventoryOffset);
        if(itemstack.getItem() instanceof GenericGun) {
            return ((GenericGun) itemstack.getItem()).getAI_attackRange();
        }

        return 24.0D;
    }

    @Override
    public double getLimitRangeSqOnFollow() {
        return 256.0D;
    }

    @Override
    public double getFreedomTrackingRangeSq() {
        return 441.0D;
    }
}
