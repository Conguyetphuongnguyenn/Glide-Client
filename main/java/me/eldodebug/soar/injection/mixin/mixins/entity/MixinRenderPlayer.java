package me.eldodebug.soar.injection.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.eldodebug.soar.injection.interfaces.IMixinEntityPlayer;
import me.eldodebug.soar.injection.interfaces.IMixinRenderPlayer;
import me.eldodebug.soar.management.event.impl.EventRenderPlayer;
import me.eldodebug.soar.management.mods.impl.Skin3DMod;
import me.eldodebug.soar.management.mods.impl.skin3d.layers.BodyLayerFeatureRenderer;
import me.eldodebug.soar.management.mods.impl.skin3d.layers.HeadLayerFeatureRenderer;
import me.eldodebug.soar.management.mods.impl.skin3d.render.CustomizableModelPart;
import me.eldodebug.soar.management.mods.impl.waveycapes.layers.CustomCapeRenderLayer;
import me.eldodebug.soar.utils.SkinUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;

@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer extends RendererLivingEntity<AbstractClientPlayer> implements IMixinRenderPlayer {

    @Shadow
    private boolean smallArms;

    private HeadLayerFeatureRenderer headLayer;
    private BodyLayerFeatureRenderer bodyLayer;

    public MixinRenderPlayer(RenderManager p_i46156_1_, ModelBase p_i46156_2_, float p_i46156_3_) {
        super(p_i46156_1_, p_i46156_2_, p_i46156_3_);
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    public void onCreate(CallbackInfo info) {
        RenderPlayer self = (RenderPlayer)(Object)this;
        headLayer = new HeadLayerFeatureRenderer(self);
        bodyLayer = new BodyLayerFeatureRenderer(self);
        addLayer(new CustomCapeRenderLayer(self, getMainModel()));
    }

    @Inject(method = "setModelVisibilities", at = @At("HEAD"))
    private void setModelProperties(AbstractClientPlayer abstractClientPlayer, CallbackInfo info) {
        Skin3DMod skin3D = Skin3DMod.getInstance();
        if(skin3D == null || !skin3D.isToggled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if(mc == null || mc.thePlayer == null || abstractClientPlayer == null) return;

        ModelPlayer playerModel = (ModelPlayer) getMainModel();
        if(playerModel == null) return;

        double distanceLOD = skin3D.getRenderDistanceLOD();
        double distSq = mc.thePlayer.getPositionVector().squareDistanceTo(abstractClientPlayer.getPositionVector());

        if(distSq < distanceLOD * distanceLOD) {
            playerModel.bipedHeadwear.isHidden = playerModel.bipedHeadwear.isHidden;
            playerModel.bipedBodyWear.isHidden = playerModel.bipedBodyWear.isHidden;
            playerModel.bipedLeftArmwear.isHidden = playerModel.bipedLeftArmwear.isHidden;
            playerModel.bipedRightArmwear.isHidden = playerModel.bipedRightArmwear.isHidden;
            playerModel.bipedLeftLegwear.isHidden = playerModel.bipedLeftLegwear.isHidden;
            playerModel.bipedRightLegwear.isHidden = playerModel.bipedRightLegwear.isHidden;
        } else {
            if(!abstractClientPlayer.isSpectator()) {
                playerModel.bipedHeadwear.isHidden = false;
                playerModel.bipedBodyWear.isHidden = false;
                playerModel.bipedLeftArmwear.isHidden = false;
                playerModel.bipedRightArmwear.isHidden = false;
                playerModel.bipedLeftLegwear.isHidden = false;
                playerModel.bipedRightLegwear.isHidden = false;
            }
        }
    }

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true)
    public void preDoRender(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if(entity == null) {
            ci.cancel();
            return;
        }
        EventRenderPlayer event = new EventRenderPlayer(entity, x, y, z, partialTicks);
        event.call();
        if(event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderRightArm", at = @At("RETURN"))
    public void renderRightArm(AbstractClientPlayer player, CallbackInfo info) {
        Skin3DMod skin3D = Skin3DMod.getInstance();
        if(skin3D == null || !skin3D.isToggled() || player == null) return;
        IMixinEntityPlayer mixin = (IMixinEntityPlayer)player;
        renderFirstPersonArm(player, 3, mixin);
    }

    @Inject(method = "renderLeftArm", at = @At("RETURN"))
    public void renderLeftArm(AbstractClientPlayer player, CallbackInfo info) {
        Skin3DMod skin3D = Skin3DMod.getInstance();
        if(skin3D == null || !skin3D.isToggled() || player == null) return;
        IMixinEntityPlayer mixin = (IMixinEntityPlayer)player;
        renderFirstPersonArm(player, 2, mixin);
    }

    public void renderFirstPersonArm(AbstractClientPlayer player, int layerId, IMixinEntityPlayer settings) {
        if(player == null || settings == null) return;

        ModelPlayer modelplayer = (ModelPlayer) getMainModel();
        if(modelplayer == null) return;

        Skin3DMod skin3D = Skin3DMod.getInstance();
        if(skin3D == null) return;

        float pixelScaling = skin3D.getBaseVoxelSize();

        if(settings.getSkinLayers() == null && !setupModel(player, settings)) {
            return;
        }

        CustomizableModelPart[] skinLayers = settings.getSkinLayers();
        if(skinLayers == null || layerId >= skinLayers.length || skinLayers[layerId] == null) {
            return;
        }

        GlStateManager.pushMatrix();
        modelplayer.bipedRightArm.postRender(0.0625F);
        GlStateManager.scale(0.0625, 0.0625, 0.0625);
        GlStateManager.scale(pixelScaling, pixelScaling, pixelScaling);

        if(!smallArms) {
            skinLayers[layerId].x = -0.998f * 16f;
        } else {
            skinLayers[layerId].x = -0.499f * 16;
        }

        skinLayers[layerId].render(false);
        GlStateManager.popMatrix();
    }

    private boolean setupModel(AbstractClientPlayer abstractClientPlayerEntity, IMixinEntityPlayer settings) {
        if(abstractClientPlayerEntity == null || settings == null) return false;
        if(!SkinUtils.hasCustomSkin(abstractClientPlayerEntity)) {
            return false;
        }
        SkinUtils.setup3dLayers(abstractClientPlayerEntity, settings, smallArms, null);
        return true;
    }

    @Redirect(method = "renderRightArm", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/ModelPlayer;isSneak:Z", ordinal = 0))
    private void resetArmState(ModelPlayer modelPlayer, boolean value) {
        if(modelPlayer != null) {
            modelPlayer.isRiding = modelPlayer.isSneak = false;
        }
    }

    @Override
    public HeadLayerFeatureRenderer getHeadLayer() {
        return headLayer;
    }

    @Override
    public BodyLayerFeatureRenderer getBodyLayer() {
        return bodyLayer;
    }

    @Override
    public boolean hasThinArms() {
        return smallArms;
    }
}