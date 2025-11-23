package me.eldodebug.soar.hooks;

import me.eldodebug.soar.management.mods.impl.ItemPhysicsMod;
import me.eldodebug.soar.management.mods.impl.Items2DMod;
import me.eldodebug.soar.management.mods.impl.UHCOverlayMod;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

public class RenderEntityItemHook {

	private static final float ITEM_VERTICAL_OFFSET = 0.15F;
	private static final float FLAT_ITEM_VERTICAL_OFFSET = 0.02F;
	private static final float ITEM_SCALE_OFFSET = 0.25F;
	private static final int ROTATION_PERIOD = 360 * 20;
	
	public static int func_177077_a(EntityItem itemIn, double p_177077_2_, double p_177077_4_, double p_177077_6_, float p_177077_8_, IBakedModel p_177077_9_, int func_177078_a) {
		
		if (itemIn == null) return 0;
		
		ItemStack itemstack = itemIn.getEntityItem();
		if (itemstack == null) return 0;
		
		Item item = itemstack.getItem();
		if (item == null) return 0;
		
		Block block = Block.getBlockFromItem(item);
		ItemPhysicsMod mod = ItemPhysicsMod.getInstance();
		
		if (mod == null) {
			return renderVanillaItem(itemIn, p_177077_2_, p_177077_4_, p_177077_6_, p_177077_8_, p_177077_9_, func_177078_a, itemstack);
		}
		
		boolean flag = p_177077_9_.isGui3d();
		int i = func_177078_a;
		
		applyInitialTranslation(mod, block, p_177077_2_, p_177077_4_, p_177077_6_, itemIn, p_177077_8_, p_177077_9_);
		applyRotation(mod, itemIn, p_177077_8_, p_177077_9_);
		
		if (!flag) {
			applyStackOffset(i);
		}
		
		if (mod.isToggled() && !itemIn.onGround) {
			applyPhysicsRotation(mod);
		}
		
		applyUHCScaling(item, block, mod.isToggled(), i);
		
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		return i;
	}
	
	private static int renderVanillaItem(EntityItem itemIn, double p_177077_2_, double p_177077_4_, double p_177077_6_, float p_177077_8_, IBakedModel p_177077_9_, int func_177078_a, ItemStack itemstack) {
		
		float f1 = MathHelper.sin(((float)itemIn.getAge() + p_177077_8_) / 10.0F + itemIn.hoverStart) * 0.1F + 0.1F;
		float f2 = p_177077_9_.getItemCameraTransforms().getTransform(ItemCameraTransforms.TransformType.GROUND).scale.y;
		
		GlStateManager.translate((float)p_177077_2_, (float)p_177077_4_ + f1 + ITEM_SCALE_OFFSET * f2, (float)p_177077_6_);
		
		Minecraft mc = Minecraft.getMinecraft();
		if (mc != null && mc.getRenderManager() != null && mc.getRenderManager().options != null) {
			if (p_177077_9_.isGui3d() || (Items2DMod.getInstance() != null && !Items2DMod.getInstance().isToggled())) {
				float f3 = (((float)itemIn.getAge() + p_177077_8_) / 20.0F + itemIn.hoverStart) * (180F / (float)Math.PI);
				GlStateManager.rotate(f3, 0.0F, 1.0F, 0.0F);
			} else {
				GlStateManager.rotate(180.0F - mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
				GlStateManager.rotate(mc.gameSettings.thirdPersonView == 2 ? mc.getRenderManager().playerViewX : -mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
			}
		}
		
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		return func_177078_a;
	}
	
	private static void applyInitialTranslation(ItemPhysicsMod mod, Block block, double x, double y, double z, EntityItem itemIn, float partialTicks, IBakedModel model) {
		
		if (mod.isToggled()) {
			if (block != null) {
				GlStateManager.translate((float)x, (float)y + ITEM_VERTICAL_OFFSET, (float)z);
			} else {
				GlStateManager.translate((float)x, (float)y + FLAT_ITEM_VERTICAL_OFFSET, (float)z);
				GlStateManager.rotate(-90F, 1F, 0F, 0F);
			}
		} else {
			float f1 = MathHelper.sin(((float)itemIn.getAge() + partialTicks) / 10.0F + itemIn.hoverStart) * 0.1F + 0.1F;
			float f2 = model.getItemCameraTransforms().getTransform(ItemCameraTransforms.TransformType.GROUND).scale.y;
			GlStateManager.translate((float)x, (float)y + f1 + ITEM_SCALE_OFFSET * f2, (float)z);
		}
	}
	
	private static void applyRotation(ItemPhysicsMod mod, EntityItem itemIn, float partialTicks, IBakedModel model) {
		
		if (mod.isToggled()) {
			return;
		}
		
		Minecraft mc = Minecraft.getMinecraft();
		
		if (mc == null || mc.getRenderManager() == null || mc.getRenderManager().options == null) {
			return;
		}
		
		Items2DMod items2DMod = Items2DMod.getInstance();
		
		if (model.isGui3d() || items2DMod == null || !items2DMod.isToggled()) {
			float f3 = (((float)itemIn.getAge() + partialTicks) / 20.0F + itemIn.hoverStart) * (180F / (float)Math.PI);
			GlStateManager.rotate(f3, 0.0F, 1.0F, 0.0F);
		} else {
			GlStateManager.rotate(180.0F - mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
			float viewX = mc.gameSettings.thirdPersonView == 2 ? mc.getRenderManager().playerViewX : -mc.getRenderManager().playerViewX;
			GlStateManager.rotate(viewX, 1.0F, 0.0F, 0.0F);
		}
	}
	
	private static void applyStackOffset(int stackSize) {
		
		float f6 = -0.0F * (float)(stackSize - 1) * 0.5F;
		float f4 = -0.0F * (float)(stackSize - 1) * 0.5F;
		float f5 = -0.046875F * (float)(stackSize - 1) * 0.5F;
		GlStateManager.translate(f6, f4, f5);
	}
	
	private static void applyPhysicsRotation(ItemPhysicsMod mod) {
		
		float speed = mod.getSpeedSetting().getValueFloat();
		float angle = System.currentTimeMillis() % ROTATION_PERIOD / (float) (4.5 - speed);
		GlStateManager.rotate(angle, 1F, 1F, 1F);
	}
	
	private static void applyUHCScaling(Item item, Block block, boolean itemPhysicsEnabled, int stackSize) {
		
		UHCOverlayMod uhcMod = UHCOverlayMod.getInstance();
		
		if (uhcMod == null || !uhcMod.isToggled()) {
			return;
		}
		
		float scale = getUHCScale(uhcMod, item, block);
		
		if (scale <= 0.0F) {
			return;
		}
		
		if (!itemPhysicsEnabled) {
			float offsetY = scale / 8;
			GlStateManager.translate(0, offsetY, 0);
		}
		
		GlStateManager.scale(scale, scale, scale);
	}
	
	private static float getUHCScale(UHCOverlayMod uhcMod, Item item, Block block) {
		
		if (item == Items.gold_ingot) {
			return uhcMod.getGoldIngotScaleSetting().getValueFloat();
		}
		
		if (item == Items.gold_nugget) {
			return uhcMod.getGoldNuggetScaleSetting().getValueFloat();
		}
		
		if (item == Items.golden_apple) {
			return uhcMod.getGoldAppleScaleSetting().getValueFloat();
		}
		
		if (block == Blocks.gold_ore) {
			return uhcMod.getGoldOreScaleSetting().getValueFloat();
		}
		
		if (item == Items.skull) {
			return uhcMod.getSkullScaleSetting().getValueFloat();
		}
		
		return 0.0F;
	}
	
	public static void oldItemRender(RenderItem instance, IBakedModel model, ItemStack stack) {
		
		if (instance == null || model == null || stack == null) {
			return;
		}
		
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || mc.getTextureManager() == null) {
			return;
		}
		
		GlStateManager.pushMatrix();
		mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
		mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
		RenderHelper.disableStandardItemLighting();
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableBlend();
		GlStateManager.disableCull();
		GlStateManager.enableAlpha();
		GlStateManager.alphaFunc(516, 0.1F);
		GlStateManager.blendFunc(770, 771);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		instance.renderItem(stack, model);
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableLighting();
		RenderHelper.enableStandardItemLighting();
		GlStateManager.popMatrix();
		mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
		mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
	}
}