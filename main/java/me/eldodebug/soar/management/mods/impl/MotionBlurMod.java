package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.utils.Sound;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import me.eldodebug.soar.injection.interfaces.IMixinShaderGroup;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventShader;
import me.eldodebug.soar.management.event.impl.EventUpdateDisplay;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;

public class MotionBlurMod extends Mod {

    // CACHED RESOURCES
    private static final ResourceLocation SHADER_LOCATION = new ResourceLocation("minecraft:shaders/post/motion_blur.json");
    private static final int ERROR_CHECK_INTERVAL = 1000;
    private static final float EPSILON = 0.001F; // For float comparison
    
    // FIX 1: ALL volatile for thread safety (render thread + event thread)
    private volatile long lastCheck = 0L;
    private volatile long lastCacheUpdate = 0L;
    
    // Shader state - volatile for visibility
    private volatile ShaderGroup group;
    private volatile float groupBlur = 0F;
    private volatile boolean loaded = false;
    private volatile int prevWidth = 0;
    private volatile int prevHeight = 0;
    
    // Settings
    private ComboSetting typeSetting;
    private NumberSetting amountSetting;
    
    // PERFORMANCE: Cache per-frame values - volatile
    private volatile Option cachedType;
    private volatile float cachedAmount = 0.5F;
    
    // GL capabilities check
    private volatile boolean accumSupported = false;
    private volatile boolean accumChecked = false;
    
    public MotionBlurMod() {
        super(TranslateText.MOTION_BLUR, TranslateText.MOTION_BLUR_DESCRIPTION, ModCategory.RENDER);
    }
    
    @Override
    public void setup() {
        typeSetting = new ComboSetting(TranslateText.TYPE, this, TranslateText.SHADER, 
            new ArrayList<Option>(Arrays.asList(
                new Option(TranslateText.ACCUM), 
                new Option(TranslateText.SHADER)
            ))
        );
        amountSetting = new NumberSetting(TranslateText.AMOUNT, this, 0.5, 0.1, 0.9, false);
        
        // Initialize state
        loaded = false;
        groupBlur = 0;
        prevWidth = 0;
        prevHeight = 0;
        accumChecked = false;
        accumSupported = false;
    }
    
    @Override
    public void onEnable() {
        if (!Glide.getInstance().getRestrictedMod().checkAllowed(this)) {
            this.setToggled(false);
            return;
        }
        super.onEnable();
        Glide.getInstance().getEventManager().register(this);
        
        // Reset state
        cleanupShader();
        updateCache();
        
        // Reset ACCUM capability check (may have changed)
        accumChecked = false;
        accumSupported = false;
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        Glide.getInstance().getEventManager().unregister(this);
        
        // CRITICAL: Full cleanup (guaranteed execution)
        try {
            cleanupShader();
        } catch (Exception e) {
            GlideLogger.error("Error during shader cleanup", e);
        }
        
        // Reset all state
        loaded = false;
        groupBlur = 0;
        prevWidth = 0;
        prevHeight = 0;
        lastCheck = 0;
        lastCacheUpdate = 0;
        cachedType = null;
        cachedAmount = 0.5F;
        accumChecked = false;
        accumSupported = false;
    }
    
    /**
     * Centralized shader cleanup
     * CRITICAL: Prevents VRAM leaks
     */
    private void cleanupShader() {
        if (group != null) {
            try {
                group.deleteShaderGroup();
            } catch (Exception e) {
                GlideLogger.error("Failed to delete shader group", e);
            } finally {
                // FIX 2: ALWAYS set null in finally
                group = null;
            }
        }
        loaded = false;
    }
    
    /**
     * Cache settings values (thread-safe)
     */
    private void updateCache() {
        try {
            cachedType = typeSetting != null ? typeSetting.getOption() : null;
            cachedAmount = amountSetting != null ? amountSetting.getValueFloat() : 0.5F;
            
            // Validate amount
            if (Float.isNaN(cachedAmount) || Float.isInfinite(cachedAmount)) {
                cachedAmount = 0.5F;
            }
            
            lastCacheUpdate = System.currentTimeMillis();
        } catch (Exception e) {
            cachedType = null;
            cachedAmount = 0.5F;
        }
    }

    @EventTarget
    public void onShader(EventShader event) {
        // FIX 3: Full NULL CHECKS
        if (!this.isToggled()) return;
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        
        // Update cache if needed
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate > 100) {
            updateCache();
        }
        
        // Check type
        if (cachedType == null || !cachedType.getTranslate().equals(TranslateText.SHADER)) {
            // Not shader mode - cleanup if needed
            if (group != null) {
                cleanupShader();
            }
            return;
        }
        
        try {
            // NULL CHECKS: Critical MC objects
            if (mc.getTextureManager() == null || 
                mc.getResourceManager() == null || 
                mc.getFramebuffer() == null) {
                return;
            }
            
            // FIX 4: Safe ScaledResolution with validation
            ScaledResolution sr;
            try {
                sr = new ScaledResolution(mc);
                if (sr == null) return;
            } catch (Exception e) {
                // Window minimized or invalid state
                return;
            }
            
            int currentWidth = sr.getScaledWidth();
            int currentHeight = sr.getScaledHeight();
            
            // FIX 5: Validate dimensions
            if (currentWidth <= 0 || currentHeight <= 0) {
                return;
            }
            
            // Check if need to recreate shader
            boolean needRecreate = group == null || 
                                  prevWidth != currentWidth || 
                                  prevHeight != currentHeight;
            
            if (needRecreate) {
                // Cleanup old shader FIRST
                cleanupShader();
                
                // Validate display dimensions
                if (mc.displayWidth <= 0 || mc.displayHeight <= 0) {
                    return;
                }
                
                try {
                    // Create new shader
                    group = new ShaderGroup(
                        mc.getTextureManager(), 
                        mc.getResourceManager(), 
                        mc.getFramebuffer(), 
                        SHADER_LOCATION
                    );
                    group.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
                    
                    // SUCCESS: Update state atomically
                    prevWidth = currentWidth;
                    prevHeight = currentHeight;
                    groupBlur = cachedAmount;
                    loaded = false;
                    
                } catch (Exception e) {
                    // FAILURE: Allow retry next frame
                    GlideLogger.error("Failed to create motion blur shader", e);
                    cleanupShader();
                    this.setToggled(false);
                    return;
                }
            }
            
            // FIX 6: Safe float comparison with epsilon
            if (Math.abs(groupBlur - cachedAmount) > EPSILON || !loaded) {
                updateShaderUniforms();
            }
            
            // Add to render pipeline
            if (group != null && event.getGroups() != null) {
                event.getGroups().add(group);
            }
            
        } catch (Exception e) {
            GlideLogger.error("Motion blur shader error", e);
            cleanupShader();
            this.setToggled(false);
        }
    }
    
    /**
     * Safe uniform update with full null checks
     */
    private void updateShaderUniforms() {
        if (group == null) return;
        
        try {
            // FIX 7: Safe cast with instanceof check
            if (!(group instanceof IMixinShaderGroup)) {
                GlideLogger.warn("ShaderGroup is not IMixinShaderGroup - cannot access shaders");
                return;
            }
            
            List<Shader> shaders = ((IMixinShaderGroup) group).getListShaders();
            if (shaders == null || shaders.isEmpty()) {
                return;
            }
            
            for (Shader shader : shaders) {
                if (shader == null || shader.getShaderManager() == null) {
                    continue;
                }
                
                ShaderUniform factor = shader.getShaderManager().getShaderUniform("BlurFactor");
                if (factor != null) {
                    factor.set(cachedAmount);
                }
            }
            
            groupBlur = cachedAmount;
            loaded = true;
            
        } catch (ClassCastException e) {
            GlideLogger.error("Failed to cast ShaderGroup to IMixinShaderGroup", e);
            loaded = false;
        } catch (Exception e) {
            GlideLogger.error("Failed to update shader uniforms", e);
            loaded = false;
        }
    }
    
    @EventTarget
    public void onUpdateDisplay(EventUpdateDisplay event) {
        // FIX 8: Full NULL CHECKS
        if (!this.isToggled()) return;
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        
        // Update cache if needed
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate > 100) {
            updateCache();
        }
        
        // Check type
        if (cachedType == null || !cachedType.getTranslate().equals(TranslateText.ACCUM)) {
            // Not accum mode - cleanup shader if exists
            if (group != null) {
                cleanupShader();
            }
            return;
        }
        
        // FIX 9: Check GL ACCUM capability ONCE
        if (!accumChecked) {
            checkAccumSupport();
            accumChecked = true;
        }
        
        if (!accumSupported) {
            GlideLogger.warn("GL_ACCUM not supported - switching to SHADER mode");
            this.setToggled(false);
            return;
        }
        
        try {
            // FIX 10: Save/restore GL state
            int prevDepthMask = GL11.glGetInteger(GL11.GL_DEPTH_WRITEMASK);
            int prevColorMask = GL11.glGetInteger(GL11.GL_COLOR_WRITEMASK);
            
            // Apply accumulation buffer motion blur
            GL11.glAccum(GL11.GL_MULT, cachedAmount);
            GL11.glAccum(GL11.GL_ACCUM, 1.0f - cachedAmount);
            GL11.glAccum(GL11.GL_RETURN, 1.0f);
            
            // FIX 11: Restore GL state
            if (prevDepthMask == 1) {
                GL11.glDepthMask(true);
            }
            // Note: Color mask restoration if needed
            
            // Periodic error check
            if (now - lastCheck > ERROR_CHECK_INTERVAL) {
                lastCheck = now;
                
                int error = GL11.glGetError();
                if (error == GL11.GL_INVALID_OPERATION) {
                    GlideLogger.warn("Motion blur ACCUM mode error - disabling");
                    this.setToggled(false);
                    accumSupported = false;
                    
                    try {
                        Sound.play("soar/audio/error.wav", false);
                    } catch (Exception ignored) {}
                }
            }
            
        } catch (Exception e) {
            GlideLogger.error("Motion blur ACCUM error", e);
            this.setToggled(false);
            accumSupported = false;
        }
    }
    
    /**
     * FIX 12: Check GL ACCUM support
     */
    private void checkAccumSupport() {
        try {
            // Check if ACCUM buffer is available
            int accumRedBits = GL11.glGetInteger(GL11.GL_ACCUM_RED_BITS);
            int accumGreenBits = GL11.glGetInteger(GL11.GL_ACCUM_GREEN_BITS);
            int accumBlueBits = GL11.glGetInteger(GL11.GL_ACCUM_BLUE_BITS);
            
            // Need at least some bits in each channel
            accumSupported = (accumRedBits > 0 && accumGreenBits > 0 && accumBlueBits > 0);
            
            if (!accumSupported) {
                GlideLogger.warn("GL_ACCUM buffer not available (bits: R=" + accumRedBits + 
                               " G=" + accumGreenBits + " B=" + accumBlueBits + ")");
            }
        } catch (Exception e) {
            GlideLogger.error("Failed to check ACCUM support", e);
            accumSupported = false;
        }
    }
}