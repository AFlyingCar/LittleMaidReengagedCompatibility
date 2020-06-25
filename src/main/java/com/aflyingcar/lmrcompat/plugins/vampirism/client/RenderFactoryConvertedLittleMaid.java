package com.aflyingcar.lmrcompat.plugins.vampirism.client;

import com.aflyingcar.lmrcompat.plugins.vampirism.EntityConvertedLittleMaid;
import net.blacklab.lmr.entity.EntityLittleMaid;
import net.blacklab.lmr.entity.renderfactory.RenderFactoryLittleMaid;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.client.registry.IRenderFactory;

public class RenderFactoryConvertedLittleMaid implements IRenderFactory<EntityConvertedLittleMaid> {
    @Override
    public Render<? super EntityConvertedLittleMaid> createRenderFor(RenderManager manager) {
        return new RenderConvertedLittleMaid(manager, 0.3F);
    }
}
