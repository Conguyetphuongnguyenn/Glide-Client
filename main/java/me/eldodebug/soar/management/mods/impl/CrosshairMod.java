package me.eldodebug.soar.management.mods.impl;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRenderCrosshair;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.utils.GlUtils;
import me.eldodebug.soar.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;

public class CrosshairMod extends Mod {

	private BooleanSetting hideThirdPersonViewSetting;
	private BooleanSetting customCrosshairSetting;
	private BooleanSetting redOnTargetSetting;
	private NumberSetting typeSetting;
	private NumberSetting scaleSetting;
	
	public CrosshairMod() {
		super(TranslateText.CROSSHAIR, TranslateText.CROSSHAIR_DESCRIPTION, ModCategory.RENDER);
	}

	@Override
	public void setup() {
		this.hideThirdPersonViewSetting = new BooleanSetting(TranslateText.HIDE_THIRD_PERSON_VIEW, this, false);
		this.customCrosshairSetting = new BooleanSetting(TranslateText.CUSTOM_CROSSHAIR, this, false);
		this.redOnTargetSetting = new BooleanSetting(TranslateText.RED_ON_TARGET, this, true);
		this.typeSetting = new NumberSetting(TranslateText.TYPE, this, 1, 0, 8, true);
		this.scaleSetting = new NumberSetting(TranslateText.SCALE, this, 1, 0.3, 2, false);
	}

	@EventTarget
	public void onRenderCrosshair(EventRenderCrosshair event) {
		
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        
        if(hideThirdPersonViewSetting != null && hideThirdPersonViewSetting.isToggled() && mc.gameSettings.thirdPersonView != 0) {
        	event.setCancelled(true);
        }
        
        if(customCrosshairSetting != null && customCrosshairSetting.isToggled()) {
        	
        	event.setCancelled(true);
        	
        	if((!hideThirdPersonViewSetting.isToggled()) || (hideThirdPersonViewSetting.isToggled() && mc.gameSettings.thirdPersonView == 0)) {
        		
        		int index = typeSetting.getValueInt();
        		
        		boolean aimingAtEntity = false;
        		if(redOnTargetSetting != null && redOnTargetSetting.isToggled() && mc.objectMouseOver != null) {
        			if(mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY 
        				&& mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
        				aimingAtEntity = true;
        			}
        		}
        		
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(775, 769, 1, 0);
                GlStateManager.enableAlpha();
                GlStateManager.enableDepth();
                
                if(aimingAtEntity) {
                	GlStateManager.color(1.0F, 0.0F, 0.0F, 1.0F);
                } else {
                	GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                }
                
            	mc.getTextureManager().bindTexture(new ResourceLocation("soar/crosshair.png"));
            	GlUtils.startScale(sr.getScaledWidth() / 2 - 7, sr.getScaledHeight() / 2 - 7, 16, 16, scaleSetting.getValueFloat());
            	RenderUtils.drawTexturedModalRect(sr.getScaledWidth() / 2 - 7, sr.getScaledHeight() / 2 - 7, index * 16, 0, 16, 16);
            	GlUtils.stopScale();
            	
            	GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        	}
        }
	}
}