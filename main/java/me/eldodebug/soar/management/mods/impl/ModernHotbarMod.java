package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.event.impl.EventRenderExpBar;
import me.eldodebug.soar.management.event.impl.EventRenderTooltip;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ModernHotbarMod extends HUDMod {

    // Cached colors
    private static final Color COLOR_OVERLAY = new Color(0, 0, 0, 100);
    private static final Color COLOR_WHITE_ALPHA = new Color(255, 255, 255, 140);
    private static final Color COLOR_SELECTION = new Color(230, 230, 230, 180);
    private static final Color COLOR_BG_DARK = new Color(20, 20, 20, 180);
    
    // Design types (tránh string comparison)
    private enum DesignType { NORMAL, SOAR, CHILL, CLIENT }
    private enum PickupType { POP, BREAD, VANILLA }
    
    // State
    private SimpleAnimation animation;
    private float barX, barY, barWidth, barHeight, selX;
    private volatile ScaledResolution cachedResolution;
    private volatile long lastResolutionUpdate = 0;
    
    // Cached per frame
    private int cachedCenterX, cachedCenterY;
    private DesignType cachedDesignType;
    private PickupType cachedPickupType;
    private boolean cachedIsText;
    
    // Settings
    private ComboSetting designSetting;
    private BooleanSetting smoothSetting;
    private ComboSetting pickupAnimation;
    
    public ModernHotbarMod() {
        super(TranslateText.MODERN_HOTBAR, TranslateText.MODERN_HOTBAR_DESCRIPTION);
        this.setDraggable(false);
    }
    
    @Override
    public void setup() {
        this.animation = new SimpleAnimation(0.0F);
        
        this.designSetting = new ComboSetting(TranslateText.DESIGN, this, TranslateText.CLIENT, 
            new ArrayList<Option>(Arrays.asList(
                new Option(TranslateText.NORMAL), 
                new Option(TranslateText.SOAR), 
                new Option(TranslateText.CHILL), 
                new Option(TranslateText.CLIENT)
            ))
        );
        
        this.smoothSetting = new BooleanSetting(TranslateText.SMOOTH, this, true);
        
        this.pickupAnimation = new ComboSetting(TranslateText.PICKUP_ANIM, this, TranslateText.PICKUP_POP, 
            new ArrayList<Option>(Arrays.asList(
                new Option(TranslateText.PICKUP_POP), 
                new Option(TranslateText.PICKUP_BREAD), 
                new Option(TranslateText.PICKUP_VANILLA)
            ))
        );
    }
    
    @Override
    public void onEnable() {
        if (!Glide.getInstance().getRestrictedMod().checkAllowed(this)) {
            this.setToggled(false);
            return;
        }
        super.onEnable();
        Glide.getInstance().getEventManager().register(this);
        
        if (animation == null) {
            animation = new SimpleAnimation(0.0F);
        }
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        Glide.getInstance().getEventManager().unregister(this);
        
        cachedResolution = null;
        animation = null;
        barX = barY = barWidth = barHeight = selX = 0;
        cachedCenterX = cachedCenterY = 0;
        lastResolutionUpdate = 0;
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!this.isToggled()) return;
        if (this.isEditing()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
        if (nvg == null) return;
        
        // Update cache (max 1/giây)
        long now = System.currentTimeMillis();
        if (cachedResolution == null || now - lastResolutionUpdate > 1000) {
            cachedResolution = new ScaledResolution(mc);
            lastResolutionUpdate = now;
        }
        if (cachedResolution == null) return;
        
        // Cache calculations (1 lần/frame)
        updateCache();
        
        nvg.setupAndDraw(() -> drawNanoVG(nvg));
        
        if (mc.getRenderViewEntity() instanceof EntityPlayer) {
            renderHotbarItems(event.getPartialTicks(), (EntityPlayer) mc.getRenderViewEntity());
        }
    }
    
    // OPTIMIZED: Cache mọi thứ 1 lần/frame thay vì 20+ lần
    private void updateCache() {
        cachedCenterX = cachedResolution.getScaledWidth() / 2;
        cachedCenterY = cachedResolution.getScaledHeight();
        
        // Convert option → enum (tránh string comparison trong loop)
        Option opt = designSetting.getOption();
        if (opt != null) {
            TranslateText t = opt.getTranslate();
            if (t.equals(TranslateText.SOAR)) cachedDesignType = DesignType.SOAR;
            else if (t.equals(TranslateText.CHILL)) cachedDesignType = DesignType.CHILL;
            else if (t.equals(TranslateText.CLIENT)) cachedDesignType = DesignType.CLIENT;
            else cachedDesignType = DesignType.NORMAL;
        } else {
            cachedDesignType = DesignType.NORMAL;
        }
        
        opt = pickupAnimation.getOption();
        if (opt != null) {
            TranslateText t = opt.getTranslate();
            if (t.equals(TranslateText.PICKUP_BREAD)) cachedPickupType = PickupType.BREAD;
            else if (t.equals(TranslateText.PICKUP_VANILLA)) cachedPickupType = PickupType.VANILLA;
            else cachedPickupType = PickupType.POP;
        } else {
            cachedPickupType = PickupType.POP;
        }
        
        Option theme = InternalSettingsMod.getInstance().getModThemeSetting().getOption();
        cachedIsText = theme != null && theme.getTranslate().equals(TranslateText.TEXT);
    }
    
    private void renderHotbarItems(float partialTicks, EntityPlayer player) {
        if (player == null || player.inventory == null) return;
        if (player.inventory.mainInventory == null) return;
        
        // OPTIMIZED: Tính ngoài loop
        int baseX = cachedCenterX - 90;
        int baseY = cachedCenterY - 19;
        int yAdjust = (cachedDesignType == DesignType.CHILL) ? 4 : 0;
        
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();

        try {
            for (int j = 0; j < 9; ++j) {
                renderHotBarItem(j, baseX + j * 20 + 2, baseY + yAdjust - 4, partialTicks, player);
            }
        } finally {
            // FIX: Always restore
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
        }
    }
    
    private void renderHotBarItem(int index, int xPos, int yPos, float partialTicks, EntityPlayer player) {
        if (player == null || player.inventory == null || player.inventory.mainInventory == null) return;
        if (index < 0 || index >= player.inventory.mainInventory.length) return;
        
        ItemStack itemstack = player.inventory.mainInventory[index];
        if (itemstack == null) return;
        
        // OPTIMIZED: Dùng cached type
        float take = (cachedPickupType == PickupType.BREAD) ? partialTicks / 2 : partialTicks;
        float progress = (float) itemstack.animationsToGo - take;
        
        if (progress > 0.0F) {
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(xPos + 8, yPos + 12, 0.0F);
                
                // OPTIMIZED: Switch thay vì if-else
                switch (cachedPickupType) {
                    case BREAD: {
                        float s = 1.0F + progress / 2.5F;
                        GlStateManager.scale(Math.max(1.0F, s / (1.0F / (s / 2))), s, 1.0F);
                        break;
                    }
                    case POP: {
                        float s = 1.0F + progress / 5.0F;
                        GlStateManager.scale(s, s, 1.0F);
                        break;
                    }
                    case VANILLA: {
                        float s = 1.0F + progress / 5.0F;
                        GlStateManager.scale(1.0F / s, (s + 1.0F) / 2.0F, 1.0F);
                        break;
                    }
                }
                
                GlStateManager.translate(-(xPos + 8), -(yPos + 12), 0.0F);
                mc.getRenderItem().renderItemAndEffectIntoGUI(itemstack, xPos, yPos);
            } finally {
                // FIX: Always pop
                GlStateManager.popMatrix();
            }
        } else {
            mc.getRenderItem().renderItemAndEffectIntoGUI(itemstack, xPos, yPos);
        }

        mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, itemstack, xPos, yPos);
    }
    
    private void drawNanoVG(NanoVGManager nvg) {
        if (!(mc.getRenderViewEntity() instanceof EntityPlayer)) return;
        
        EntityPlayer player = (EntityPlayer) mc.getRenderViewEntity();
        if (player == null || player.inventory == null) return;
        
        AccentColor color = Glide.getInstance().getColorManager().getCurrentColor();
        
        drawBackground(nvg, color);
        drawSelection(nvg, player);
    }
    
    // OPTIMIZED: Switch thay vì if-else chain
    private void drawBackground(NanoVGManager nvg, AccentColor color) {
        switch (cachedDesignType) {
            case CHILL:
                barX = 0;
                barY = cachedCenterY - 22;
                barWidth = cachedResolution.getScaledWidth();
                barHeight = 22;
                nvg.drawShadow(barX, barY, barWidth, barHeight, 0);
                nvg.drawRect(barX, barY, barWidth, barHeight, COLOR_BG_DARK);
                break;
            
            case SOAR:
                barX = cachedCenterX - 91;
                barY = cachedCenterY - 26;
                barWidth = 182;
                barHeight = 22;
                nvg.drawShadow(barX, barY, barWidth, barHeight, 6);
                nvg.drawGradientRoundedRect(barX, barY, barWidth, barHeight, 6, 
                    ColorUtils.applyAlpha(color.getColor1(), 190), 
                    ColorUtils.applyAlpha(color.getColor2(), 190));
                break;
            
            case CLIENT:
                barX = cachedCenterX - 91;
                barY = cachedCenterY - 26;
                barWidth = 182;
                barHeight = 22;
                if (cachedIsText) {
                    nvg.drawShadow(barX, barY, barWidth, barHeight, 6);
                }
                this.setScale(1f);
                this.setX((int) barX);
                this.setY((int) barY);
                drawBackground(barWidth, barHeight, 6);
                break;
            
            default: // NORMAL
                barX = cachedCenterX - 91;
                barY = cachedCenterY - 26;
                barWidth = 182;
                barHeight = 22;
                nvg.drawShadow(barX, barY, barWidth, barHeight, 6);
                nvg.drawRoundedRect(barX, barY, barWidth, barHeight, 6, COLOR_OVERLAY);
                break;
        }
    }
    
    private void drawSelection(NanoVGManager nvg, EntityPlayer player) {
        if (player == null || player.inventory == null) return;
        
        float targetX = cachedCenterX - 92 + player.inventory.currentItem * 20;
        
        if (smoothSetting.isToggled() && animation != null) {
            animation.setAnimation(targetX, 18);
            selX = animation.getValue();
        } else {
            selX = targetX;
        }

        switch (cachedDesignType) {
            case CHILL:
                nvg.drawRect(selX + 1, cachedCenterY - 22, 22, 22, COLOR_SELECTION);
                break;
            case SOAR:
                nvg.drawRoundedRect(selX + 1, cachedCenterY - 26, 22, 22, 6, COLOR_WHITE_ALPHA);
                break;
            default:
                nvg.drawRoundedRect(selX + 1, cachedCenterY - 26, 22, 22, 6, COLOR_OVERLAY);
                break;
        }
    }
    
    @EventTarget
    public void onRenderTooltip(EventRenderTooltip event) {
        if (this.isToggled()) {
            event.setCancelled(true);
        }
    }
    
    @EventTarget
    public void onRenderExpBar(EventRenderExpBar event) {
        if (this.isToggled()) {
            event.setCancelled(cachedDesignType != DesignType.CHILL);
        }
    }
}