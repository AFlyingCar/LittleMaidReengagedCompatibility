package com.aflyingcar.lmrcompat.plugins.vampirism.client;

import com.aflyingcar.lmrcompat.LMRCompat;
import de.teamlapen.vampirism.client.render.entities.RenderConvertedCreature;
import net.blacklab.lmr.client.renderer.entity.RenderLittleMaid;
import net.blacklab.lmr.entity.maidmodel.ModelMulti_Stef;
import net.blacklab.lmr.entity.maidmodel.ModelMulti_Steve;
import net.blacklab.lmr.entity.maidmodel.ModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityCreature;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class RenderConvertedLittleMaid extends RenderLittleMaid {
    public static final ResourceLocation MAID_VAMPIRE_OVERLAY = new ResourceLocation("lmrcompat", "textures/vampirism/entity/overlay.png");

    // TODO: This should be one that maps better to players
    public static final ResourceLocation MAID_VAMPIRE_STEVE_OVERLAY = new ResourceLocation("vampirism", "textures/entity/vanilla/steve7.png");

    public RenderConvertedLittleMaid(RenderManager manager, float f) {
        super(manager, f);

        addLayer(new MMMLayerVampireEntity(this, false));
    }

    // A reimplementation of the Vampirism LayerVampireEntity class for MultiModels
    public class MMMLayerVampireEntity implements LayerRenderer<EntityCreature> {
        private RenderConvertedLittleMaid renderer;
        private boolean checkIfShouldRenderOverlay;

        public static final float renderScale = 0.0625F;

        /**
         * @param renderer The parent renderer
         */
        public MMMLayerVampireEntity(RenderConvertedLittleMaid renderer, boolean checkIfShouldRenderOverlay) {
            this.renderer = renderer;
            this.checkIfShouldRenderOverlay = checkIfShouldRenderOverlay;
        }

        @Override
        public void doRenderLayer(EntityCreature entityCreatureIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            if(!entityCreatureIn.isInvisible() && (!checkIfShouldRenderOverlay || RenderConvertedCreature.renderOverlay)) {
                 // LMRCompat.getLogger().info(entityCreatureIn.getUniqueID() + "::doRenderLayer(). The main model has " + modelMain.model.boxList.size() + " boxes that we can render to.");

                // Bind the new overlay texture
                if(modelMain.model instanceof ModelMulti_Steve || modelMain.model instanceof ModelMulti_Stef)
                    renderer.bindTexture(MAID_VAMPIRE_STEVE_OVERLAY);
                else
                    renderer.bindTexture(MAID_VAMPIRE_OVERLAY);

                // TODO: Check if lmrcompat:textures/vampirism/entity/${eclm.textureNameMain}_overlay.png exists
                //  Use that one instead if it exists, MAID_VAMPIRE_OVERLAY otherwise

                // Render the maid again
                modelMain.model.mainFrame.render(scale);
            }
        }

        @Override
        public boolean shouldCombineTextures() {
            return true;
        }
    }
}
