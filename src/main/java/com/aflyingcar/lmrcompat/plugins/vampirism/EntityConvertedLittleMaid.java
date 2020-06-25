package com.aflyingcar.lmrcompat.plugins.vampirism;

import com.aflyingcar.lmrcompat.LMRCompat;
import com.aflyingcar.lmrcompat.LMRCompatItems;
import com.aflyingcar.lmrcompat.items.ItemBloodySugar;
import de.teamlapen.vampirism.api.EnumStrength;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.IExtendedCreatureVampirism;
import de.teamlapen.vampirism.api.entity.convertible.IConvertedCreature;
import de.teamlapen.vampirism.api.entity.convertible.IConvertingHandler;
import de.teamlapen.vampirism.api.entity.vampire.IVampire;
import de.teamlapen.vampirism.entity.ai.EntityAIMoveIndoorsDay;
import de.teamlapen.vampirism.entity.ai.VampireAIBiteNearbyEntity;
import de.teamlapen.vampirism.entity.ai.VampireAIFleeSun;
import de.teamlapen.vampirism.entity.ai.VampireAIMoveToBiteable;
import net.blacklab.lmr.api.item.IItemSpecialSugar;
import net.blacklab.lmr.client.entity.EntityLittleMaidAvatarSP;
import net.blacklab.lmr.entity.EntityLittleMaid;
import net.blacklab.lmr.entity.EntityLittleMaidAvatarMP;
import net.blacklab.lmr.util.helper.CommonHelper;
import net.blacklab.lmr.util.helper.OwnableEntityHelper;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIRestrictSun;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class EntityConvertedLittleMaid extends EntityLittleMaidVampirism implements IConvertedCreature<EntityLittleMaid> {
    // TODO: Should we have a way for maids to get blood without using bloody sugar?
    //   i.e: Should they be able to drink blood from creatures?

    private boolean sundamage;
    private EnumStrength garlic_damage;

    public EntityConvertedLittleMaid(World par1World) {
        super(par1World);

        // Add tasks to avoid the sun and to possibly try biting other entities (but it should be low on their priority list compared with doing their job)

        // Mode ID => Pair<Default Tasks, Mode-Specific Tasks>
        for(Map.Entry<Integer, EntityAITasks[]> maidMode : maidModeList.entrySet()) {
            // Setup sun/water avoidance AI (tasks)
            if(maidMode.getValue()[0] != null) {
                // Note: that these tasks are very low priority: They will prioritize doing their jobs over their own lives.
//                maidMode.getValue()[0].addTask(34, new EntityAIRestrictSun(this));
                // TODO: Should they try to avoid hunters?
                //  If so, then that should probably have special rules against players and for if they are told to attack hostiles
                //  Maybe we should make special sub-classes for these AI tasks??
                maidMode.getValue()[0].addTask(35, new EntityAIAvoidEntity<>(this, EntityCreature.class, VampirismAPI.factionRegistry().getPredicate(getFaction(), true, true, false, false, VReference.HUNTER_FACTION), 10, 0.45F, 0.55F));
                maidMode.getValue()[0].addTask(36, new EntityAIMoveIndoorsDay(this));
                maidMode.getValue()[0].addTask(37, new VampireAIFleeSun(this, 0.6F, true));
            }
        }
    }

    @Override
    public boolean doesResistGarlic(EnumStrength strength) {
        return false;
    }

    /** インベントリ内の砂糖を食べる。左上から消費する。
     *  Override of consumeSugar to do the same, but we will _not_ use normal sugar anymore
     *  Vampire Maids need blood
     * @param mode EnumConsumeSugar型の定数
     */
    @Override
    protected void consumeSugar(EnumConsumeSugar mode){
        NonNullList<ItemStack> stacklist = getMaidInventory().mainInventory;
        ItemStack stack = ItemStack.EMPTY;
        Item item = null;
        int index = -1;
        for(int i=0;i<stacklist.size();i++){
            ItemStack ts = stacklist.get(i);
            if(ts.isEmpty())continue;
            Item ti = ts.getItem();
            // TODO: Should we be doing something for IItemSpecialSugar? Do we actually need to worry about other mods which interface with this one?
            if(ti instanceof ItemBloodySugar || ti instanceof IItemSpecialSugar) {
                stack = ts;
                item = ti;
                index = i;
                break;
            }
        }
        // No need to check index, since if item == null then index == -1 as well, making checking it redundant
        if(item == null || stack.isEmpty()) return;

        if(item == LMRCompatItems.BLOODY_SUGAR){
            eatSugar(true, true, mode==EnumConsumeSugar.RECONTRACT);
        }else if(item instanceof IItemSpecialSugar){
            //モノグサ実装。良い子の皆さんはちゃんとif使うように…
            eatSugar(((IItemSpecialSugar)item).onSugarEaten(this, mode, stack), true, mode==EnumConsumeSugar.RECONTRACT);
        }
        if (mode == EnumConsumeSugar.RECONTRACT) {
            addMaidExperience(3.5f);
        }
        getMaidInventory().decrStackSize(index, Math.min(1, mode == EnumConsumeSugar.OTHER ? 1 : getExpBooster()));
    }

    @Override
    public void drinkBlood(int amt, float saturationMod, boolean useRemaining) {
        // Drinking blood will act like eating sugar
        // However, motion is false because we don't want them swinging their arms as they drink
        // TODO: Should they renew contract with this???
        this.eatSugar(false, false, true);

        // Maids will start to regenerate when they drink blood
        addPotionEffect(new PotionEffect(MobEffects.REGENERATION, amt * 20));
    }

    @Nonnull
    @Override
    public EnumStrength isGettingGarlicDamage(boolean forceRefresh) {
        return forceRefresh ? (garlic_damage = VampirismAPI.getGarlicChunkHandler(world).getStrengthAtChunk(new ChunkPos(getPosition()))) : garlic_damage;
    }

    @Override
    public boolean isGettingSundamage(boolean forceRefresh) {
        return forceRefresh ? (sundamage = !world.isRaining() && world.isDaytime()) : sundamage;
    }

    @Override
    public boolean isIgnoringSundamage() {
        return false;
    }

    @Override
    public void onLivingUpdate() {
        // Should we force a refresh of sundamage/garlicdamage?
        if(ticksExisted % VampirismCompat.REFRESH_GARLIC_TICKS == 1) {
            isGettingGarlicDamage(true);
        }
        if(ticksExisted % VampirismCompat.REFRESH_SUNDAMAGE_TICKS == 2) {
            isGettingSundamage(true);
        }

        // Most of this is similar to VampirismIntegrations code for MCA villagers
        if(!world.isRemote) {
            if(isGettingSundamage() && ticksExisted % VampirismCompat.SUNDAMAGE_POTION_EFFECTS_PERIOD == 11) {
                addPotionEffect(new PotionEffect(MobEffects.WEAKNESS));
                addPotionEffect(new PotionEffect(MobEffects.SLOWNESS));

                // The maid's contract will expire twice as fast while they are taking sundamage
                //   Keep them well shaded!
                maidContractLimit--;
            }
        }

        super.onLivingUpdate();
    }

    @Override
    public boolean useBlood(int amt, boolean allowPartial) {
        return false;
    }

    @Override
    public boolean wantsBlood() {
        // Is there only one day left for the contract?
        // TODO: Wait, what if they are unemployed? What should they do then?
        return getContractLimitDays() <= 1F;
    }

    @Override
    public EntityLivingBase getRepresentingEntity() {
        return this;
    }

    @Nullable
    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }

    /**
     * Sets the maid master entity for the given maid
     * @param _token An unused object. You _must_ have this token in order to use this method
     * @param maid The maid to assign a new master to
     * @param newMaster The new master
     */
    public static void setMaidMasterEntity(@Nonnull ConvertingHandler.Token _token, EntityConvertedLittleMaid maid, EntityPlayer newMaster) {
        maid.mstatMasterEntity = newMaster;
    }

    public static class ConvertingHandler implements IConvertingHandler<EntityLittleMaid> {
        private class Token { }

        @Override
        public IConvertedCreature<EntityLittleMaid> createFrom(EntityLittleMaid entity) {
            LMRCompat.getLogger().info("Converting a maid into a vampire!");

            NBTTagCompound nbt = new NBTTagCompound();

            LMRCompat.getLogger().info("Writing down our previous NBT data for transfer.");
            entity.writeEntityToNBT(nbt);

            LMRCompat.getLogger().info("Creating a new EntityConvertedLittleMaid");
            EntityConvertedLittleMaid converted = new EntityConvertedLittleMaid(entity.world);

            LMRCompat.getLogger().info("Reading from previous NBT data.");
            converted.readEntityFromNBT(nbt);

            LMRCompat.getLogger().info("Giving ourselves a brand new UUID");
            converted.setUniqueId(MathHelper.getRandomUUID());

            LMRCompat.getLogger().info("Make sure the new maid has the same master as the previous one");

            converted.setContract(true);
            OwnableEntityHelper.setOwner(converted, entity.getOwnerId());
            entity.world.setEntityState(converted, (byte)7);
            EntityConvertedLittleMaid.setMaidMasterEntity(new Token(), converted, entity.getMaidMasterEntity());

            BlockPos position = entity.getPosition();
            converted.setLocationAndAngles(position.getX(), position.getY(), position.getZ(), entity.rotationYaw, entity.rotationPitch);
            LMRCompat.getLogger().info("Converted Maid is at XYZ: " + converted.getPosition());

            LMRCompat.getLogger().info("Original Maid had UUID of " + entity.getUniqueID());
            LMRCompat.getLogger().info("Converted Maid has UUID of " + converted.getUniqueID());

            /*
            LMRCompat.getLogger().info("Attempting to pull the old switch-roo on the maid avatar");
            // Attempt to reset the references in the maid's internal player-like object
            EntityPlayer avatar = converted.getMaidAvatar();
            boolean createNewAvatar = false;

            if(avatar instanceof EntityLittleMaidAvatarMP) {
                ((EntityLittleMaidAvatarMP)avatar).avatar = converted;
            } else if(avatar instanceof EntityLittleMaidAvatarSP) {
                ((EntityLittleMaidAvatarSP)avatar).avatar = converted;
            } else {
                // This should never happen, as getMaidAvatar() should only ever create an EntityLittleMaidAvatarMP or
                //  an EntityLittleMaidAvatarSP
                LMRCompat.getLogger().error("Cannot reset internal values of maid avatar! Must now create a new one :(");
                createNewAvatar = true;
            }

            // If we were able to successfully reset it, then make sure both converted and entity have their own references to the entity reset
            if(!createNewAvatar) {
                ObfuscationReflectionHelper.setPrivateValue(EntityConvertedLittleMaid.class, converted, entity.getMaidAvatar(), "maidAvatar");
                ObfuscationReflectionHelper.setPrivateValue(EntityConvertedLittleMaid.class, converted, true, "gottenAvatarAlready");

                // Remove the ability for the original entity to have its avatar
                ObfuscationReflectionHelper.setPrivateValue(EntityLittleMaid.class, entity, null, "maidAvatar");
                ObfuscationReflectionHelper.setPrivateValue(EntityLittleMaid.class, entity, false, "gottenAvatarAlready");
            }
             */

            return converted;
        }
    }
}
