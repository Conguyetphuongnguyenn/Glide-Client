package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.ColorManager;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import me.eldodebug.soar.management.nanovg.font.Font;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.TargetUtils;
import me.eldodebug.soar.utils.animation.normal.Animation;
import me.eldodebug.soar.utils.animation.normal.Direction;
import me.eldodebug.soar.utils.animation.normal.easing.EaseBackIn;
import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;
import me.eldodebug.soar.utils.buffer.ScreenAnimation;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

public class TargetInfoMod extends HUDMod {

    // CACHED COLORS - CRITICAL PERFORMANCE FIX
    private static final Color BAR_BASE = new Color(255, 255, 255, 45);
    private static final Color HP_BASIC = new Color(255, 255, 255, 220);
    
    // LAYOUT CONSTANTS - Avoid recalculation
    private static final int PADDING = 8;
    private static final int AVATAR_SIZE = 28;
    private static final int SPACING = 10;
    private static final int MIN_WIDTH = 120;
    private static final int CARD_HEIGHT = 46;

    private final SimpleAnimation healthAnimation = new SimpleAnimation();
    private final ScreenAnimation screenAnimation = new ScreenAnimation();
    private Animation introAnimation;

    // Cached target data - DEFENSIVE SNAPSHOT
    private String name;
    private float health, armor;
    private ResourceLocation head;
    
    // PERFORMANCE: Cache formatted string
    private String cachedHpText = "";
    private float lastFormattedHp = -1;
    
    // PERFORMANCE: Cache per-frame values
    private Font cachedTitleFont;
    private Font cachedSubFont;
    private Color cachedTitleColor;
    private Color cachedSubColor;
    private Color cachedHpFill;
    private long lastCacheUpdate = 0;
    private static final long CACHE_INTERVAL = 100; // 100ms

    private final ComboSetting hpColorSetting = new ComboSetting(
            TranslateText.COLOR,
            this,
            TranslateText.ACCENT_COLOR,
            new ArrayList<Option>(Arrays.asList(
                    new Option(TranslateText.ACCENT_COLOR),
                    new Option(TranslateText.BASIC)
            ))
    );

    public TargetInfoMod() {
        super(TranslateText.TARGET_INFO, TranslateText.TARGET_INFO_DESCRIPTION, "targethud", true);
    }

    @Override
    public void setup() {
        introAnimation = new EaseBackIn(450, 1.0F, 2.0F);
        introAnimation.setDirection(Direction.BACKWARDS);
        updateCachedValues();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        
        if (introAnimation != null) {
            introAnimation.setDirection(Direction.BACKWARDS);
        }
        updateCachedValues();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        // CRITICAL: Cleanup cached data
        name = null;
        head = null;
        health = 0;
        armor = 0;
        cachedHpText = "";
        lastFormattedHp = -1;
        
        // Clear cached UI values
        cachedTitleFont = null;
        cachedSubFont = null;
        cachedTitleColor = null;
        cachedSubColor = null;
        cachedHpFill = null;
        lastCacheUpdate = 0;
        
        // Reset animations
        if (healthAnimation != null) {
            healthAnimation.setValue(0);
        }
        if (introAnimation != null) {
            introAnimation.setDirection(Direction.BACKWARDS);
        }
    }
    
    // PERFORMANCE: Update cached values periodically
    private void updateCachedValues() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate < CACHE_INTERVAL) return;
        lastCacheUpdate = now;
        
        try {
            // Cache fonts (may be null)
            cachedTitleFont = getHudFont(2);
            cachedSubFont = getHudFont(1);
            
            // Cache colors (may be null)
            cachedTitleColor = this.getFontColor(230);
            cachedSubColor = this.getFontColor(210);
            
            // Cache HP bar color
            cachedHpFill = HP_BASIC; // Default
            
            if (hpColorSetting != null && hpColorSetting.getOption() != null) {
                if (hpColorSetting.getOption().getTranslate().equals(TranslateText.ACCENT_COLOR)) {
                    Glide instance = Glide.getInstance();
                    if (instance != null) {
                        ColorManager cm = instance.getColorManager();
                        if (cm != null) {
                            AccentColor ac = cm.getCurrentColor();
                            if (ac != null) {
                                Color acColor = ac.getInterpolateColor();
                                if (acColor != null) {
                                    cachedHpFill = ColorUtils.applyAlpha(acColor, 220);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Cache update failed - use defaults
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        
        // NULL CHECKS FIRST
        if (!this.isToggled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        // Animation null check
        if (introAnimation == null) {
            setup();
            if (introAnimation == null) return;
        }
        
        // Update cached values periodically
        updateCachedValues();

        AbstractClientPlayer target = null;
        
        if (this.isEditing()) {
            target = mc.thePlayer;
        } else {
            target = TargetUtils.getTarget();
        }

        introAnimation.setDirection(target == null ? Direction.BACKWARDS : Direction.FORWARDS);

        // DEFENSIVE COPY - Snapshot target data
        if (target != null) {
            try {
                String targetName = target.getName();
                if (targetName != null && !targetName.isEmpty()) {
                    name = targetName;
                }
                
                float targetHealth = target.getHealth();
                if (!Float.isNaN(targetHealth) && !Float.isInfinite(targetHealth)) {
                    health = Math.min(Math.max(targetHealth, 0F), 20F);
                }
                
                int targetArmor = target.getTotalArmorValue();
                armor = Math.min(Math.max(targetArmor, 0), 20);
                
                ResourceLocation targetSkin = target.getLocationSkin();
                if (targetSkin != null) {
                    head = targetSkin;
                }
            } catch (Exception e) {
                // Entity despawned - use cached values
            }
        }

        // Render if valid data
        if (name != null && head != null && screenAnimation != null) {
            try {
                screenAnimation.wrap(
                        this::drawCard,
                        this.getX(), this.getY(),
                        this.getWidth(), this.getHeight(),
                        2 - introAnimation.getValueFloat(),
                        introAnimation.getValueFloat()
                );
            } catch (Exception e) {
                // Rendering error - skip frame
            }
        }
    }

    private void drawCard() {

        // Smooth HP animation
        healthAnimation.setAnimation(health, 18);
        float hpAnimated = Math.max(0F, Math.min(20F, (float) healthAnimation.getValue()));
        
        // NaN/Infinite safety
        if (Float.isNaN(hpAnimated) || Float.isInfinite(hpAnimated)) {
            hpAnimated = 0F;
        }
        
        // Safe division
        float hpRatio = Math.min(1.0F, Math.max(0F, hpAnimated / 20F));

        // Cache HP text formatting
        if (Math.abs(hpAnimated - lastFormattedHp) > 0.02F) {
            try {
                cachedHpText = "HP: " + String.format(Locale.US, "%.1f", hpAnimated);
                lastFormattedHp = hpAnimated;
            } catch (Exception e) {
                cachedHpText = "HP: " + ((int) hpAnimated);
            }
        }

        // NULL CHECK: Ensure we have valid data
        if (name == null || name.isEmpty()) {
            name = "Unknown";
        }

        // Calculate text widths with NULL safety
        float nameWidth = 50; // Default
        float hpWidth = 30; // Default
        
        try {
            if (cachedTitleFont != null) {
                nameWidth = this.getTextWidth(name, 10.2F, cachedTitleFont);
            }
            if (cachedSubFont != null) {
                hpWidth = this.getTextWidth(cachedHpText, 9.2F, cachedSubFont);
            }
        } catch (Exception e) {
            // Text width calculation failed - use defaults
        }
        
        // Ensure positive widths
        nameWidth = Math.max(0, nameWidth);
        hpWidth = Math.max(0, hpWidth);

        // Calculate dimensions
        int calcWidth = (int) Math.max(MIN_WIDTH, PADDING + AVATAR_SIZE + SPACING + Math.max(nameWidth, hpWidth) + PADDING);
        int calcHeight = CARD_HEIGHT;
        
        // Ensure positive dimensions
        calcWidth = Math.max(MIN_WIDTH, calcWidth);

        // Draw background
        this.drawBackground(calcWidth, calcHeight);

        // Draw avatar
        if (head != null) {
            float avatarY = (calcHeight - AVATAR_SIZE) / 2.0F;
            this.drawPlayerHead(head, PADDING, avatarY, AVATAR_SIZE, AVATAR_SIZE, 5F);
        }

        // Text position
        float textX = PADDING + AVATAR_SIZE + SPACING;

        // Draw text with cached colors
        if (cachedTitleFont != null && cachedTitleColor != null) {
            this.drawText(name, textX, 10F, 10.2F, cachedTitleFont, cachedTitleColor);
        }
        
        if (cachedSubFont != null && cachedSubColor != null) {
            this.drawText(cachedHpText, textX, 24F, 9.2F, cachedSubFont, cachedSubColor);
        }

        // HP bar dimensions
        float barX = textX;
        float barY = calcHeight - 10F;
        float barW = Math.max(0, calcWidth - barX - PADDING);
        float barH = 7F;

        // Draw HP bar base
        this.drawRoundedRect(barX, barY, barW, barH, 3.5F, BAR_BASE);

        // Draw HP fill with cached color
        if (cachedHpFill != null) {
            float fillWidth = Math.max(0, barW * hpRatio);
            this.drawRoundedRect(barX, barY, fillWidth, barH, 3.5F, cachedHpFill);
        }

        // Update HUD dimensions
        this.setWidth(calcWidth);
        this.setHeight(calcHeight);
    }
}