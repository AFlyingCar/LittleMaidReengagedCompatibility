package com.aflyingcar.lmrcompat.plugins.vampirism;

import com.aflyingcar.lmrcompat.LMRCompat;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.IExtendedCreatureVampirism;
import de.teamlapen.vampirism.api.entity.convertible.IConvertedCreature;
import de.teamlapen.vampirism.api.entity.vampire.IVampire;
import net.blacklab.lmr.entity.EntityLittleMaid;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityCreature;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;

public class EntityLittleMaidVampirism extends EntityLittleMaid {
    public static final int ENTITY_MAID_BLOOD = 15;

    public EntityLittleMaidVampirism(World par1World) {
        super(par1World);
    }

    // TODO: Have some stuff here about maids trying to avoid vampires
}
