package com.aflyingcar.lmrcompat.proxies;

import com.aflyingcar.lmrcompat.LMRCompat;
import com.aflyingcar.lmrcompat.LMRCompatItems;
import com.aflyingcar.lmrcompat.plugins.vampirism.EntityConvertedLittleMaid;
import com.aflyingcar.lmrcompat.plugins.vampirism.client.RenderFactoryConvertedLittleMaid;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        registerRenderers();
    }

    @Override
    public void init() { }

    @Override
    public void postInit() { }

    protected void registerRenderers() {
        // We want to do this at the same time as when little maids does it
        // TODO: We should put this in a LMRCompatEntities class
        if(LMRCompat.hasVampirism) {
            RenderingRegistry.registerEntityRenderingHandler(EntityConvertedLittleMaid.class, new RenderFactoryConvertedLittleMaid());
        }

        LMRCompatItems.registerAllItemModels();
    }
}
