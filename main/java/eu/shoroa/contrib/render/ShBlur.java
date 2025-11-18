package eu.shoroa.contrib.render;

import eu.shoroa.contrib.shader.UIShader;
import eu.shoroa.contrib.shader.uniform.Uniform;
import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.impl.InternalSettingsMod;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.Util;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.io.IOException;
import java.nio.FloatBuffer;

public class ShBlur {
    
    // ========================================
    // CONSTANTS
    // ========================================
    private static final ShBlur INSTANCE = new ShBlur();
    
    // Shader uniforms (extracted duplicates)
    private static final String UNIFORM_TEXTURE = "texture";
    private static final String UNIFORM_DIRECTION = "direction";
    private static final String UNIFORM_TEXEL_SIZE = "texelSize";
    private static final String UNIFORM_KERNELS = "kernels";
    private static final String UNIFORM_IGNORE_ALPHA = "ignoreAlpha";
    
    // Shader paths
    private static final String VERTEX_SHADER_PATH = "soar/shaders/vertex.vert";
    private static final String FRAGMENT_SHADER_PATH = "soar/shaders/blur.frag";
    
    // Framebuffer sizes
    private static final int FB_DIVISOR_SMALL = 2;
    private static final int FB_DIVISOR_LARGE = 6;
    
    // Timing
    private static final long UPDATE_INTERVAL_MS = 15;
    
    // Blur settings
    private static final float DEFAULT_RADIUS = 4.0F;
    private static final int WEIGHT_BUFFER_SIZE = 128;
    private static final float SIGMA_DIVISOR = 2.0F;
    
    // Math constants
    private static final double PI = 3.141592653;
    
    // ========================================
    // FIELDS
    // ========================================
    private int nvgImage = -1;
    private final Minecraft mc = Minecraft.getMinecraft();
    
    private Framebuffer framebuffer;
    private Framebuffer framebuffer1;
    private Framebuffer framebuffer2;
    private Framebuffer framebuffer3;
    
    private FloatBuffer weightBuffer;
    private UIShader shader;
    
    private long lastUpdate = System.currentTimeMillis();
    private float radius = DEFAULT_RADIUS;
    
    // ========================================
    // CONSTRUCTOR
    // ========================================
    private ShBlur() {
        // Singleton
    }
    
    public static ShBlur getInstance() {
        return INSTANCE;
    }
    
    // ========================================
    // INITIALIZATION
    // ========================================
    public void init() {
        if (mc == null) {
            throw new IllegalStateException("Minecraft not initialized");
        }
        
        int displayWidth = mc.displayWidth;
        int displayHeight = mc.displayHeight;
        
        framebuffer = createFramebuffer(displayWidth / FB_DIVISOR_SMALL, displayHeight / FB_DIVISOR_SMALL);
        framebuffer1 = createFramebuffer(displayWidth / FB_DIVISOR_SMALL, displayHeight / FB_DIVISOR_SMALL);
        framebuffer2 = createFramebuffer(displayWidth / FB_DIVISOR_LARGE, displayHeight / FB_DIVISOR_LARGE);
        framebuffer3 = createFramebuffer(displayWidth / FB_DIVISOR_LARGE, displayHeight / FB_DIVISOR_LARGE);
        
        framebuffer3.setFramebufferFilter(GL11.GL_LINEAR);
        
        cacheRadius(radius);
        
        shader = new UIShader(VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH);
        try {
            shader.init();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize blur shader", e);
        }
    }
    
    private Framebuffer createFramebuffer(int width, int height) {
        Framebuffer fb = new Framebuffer(width, height, false);
        fb.createFramebuffer(width, height);
        return fb;
    }
    
    public void cacheRadius(float radius) {
        weightBuffer = BufferUtils.createFloatBuffer(WEIGHT_BUFFER_SIZE);
        
        for (int i = 0; i < radius; i++) {
            weightBuffer.put(calculateGaussian(i, radius / SIGMA_DIVISOR));
        }
        
        weightBuffer.rewind();
    }
    
    public void resize() {
        if (mc == null) return;
        
        cleanupFramebuffers();
        
        int displayWidth = mc.displayWidth;
        int displayHeight = mc.displayHeight;
        
        framebuffer = createFramebuffer(displayWidth / FB_DIVISOR_SMALL, displayHeight / FB_DIVISOR_SMALL);
        framebuffer1 = createFramebuffer(displayWidth / FB_DIVISOR_SMALL, displayHeight / FB_DIVISOR_SMALL);
        framebuffer2 = createFramebuffer(displayWidth / FB_DIVISOR_LARGE, displayHeight / FB_DIVISOR_LARGE);
        framebuffer3 = createFramebuffer(displayWidth / FB_DIVISOR_LARGE, displayHeight / FB_DIVISOR_LARGE);
        
        framebuffer3.setFramebufferFilter(GL11.GL_LINEAR);
    }
    
    private void cleanupFramebuffers() {
        if (framebuffer != null) framebuffer.deleteFramebuffer();
        if (framebuffer1 != null) framebuffer1.deleteFramebuffer();
        if (framebuffer2 != null) framebuffer2.deleteFramebuffer();
        if (framebuffer3 != null) framebuffer3.deleteFramebuffer();
    }
    
    // ========================================
    // NVGIMAGE
    // ========================================
    private int nvgImageFromHandle(int texture, int width, int height) {
        long ctx = Glide.getInstance().getNanoVGManager().getContext();
        return NanoVGGL2.nvglCreateImageFromHandle(ctx, texture, width, height, NanoVG.NVG_IMAGE_FLIPY);
    }
    
    // ========================================
    // RENDERING
    // ========================================
    public void render() {
        if (!shouldRender()) return;
        
        if (nvgImage == -1 && framebuffer3 != null) {
            nvgImage = nvgImageFromHandle(
                framebuffer3.framebufferTexture, 
                mc.displayWidth, 
                mc.displayHeight
            );
        }
        
        ScaledResolution sr = new ScaledResolution(mc);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastUpdate > UPDATE_INTERVAL_MS) {
            lastUpdate = currentTime;
            performBlurPasses(sr);
        }
        
        if (mc.getFramebuffer() != null) {
            mc.getFramebuffer().bindFramebuffer(true);
        }
    }
    
    private boolean shouldRender() {
        if (!InternalSettingsMod.getInstance().getBlurSetting().isToggled()) {
            return false;
        }
        
        if (Util.getOSType() == Util.EnumOS.OSX) {
            return false;
        }
        
        return mc != null && mc.getFramebuffer() != null;
    }
    
    private void performBlurPasses(ScaledResolution sr) {
        if (shader == null || framebuffer == null || framebuffer1 == null || 
            framebuffer2 == null || framebuffer3 == null) {
            return;
        }
        
        float screenWidth = sr.getScaledWidth();
        float screenHeight = sr.getScaledHeight();
        
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        shader.attach();
        
        // Pass 1: Horizontal blur (input → framebuffer)
        renderBlurPass(
            framebuffer,
            mc.getFramebuffer().framebufferTexture,
            mc.getFramebuffer().framebufferWidth,
            mc.getFramebuffer().framebufferHeight,
            1.0F, 0.0F,
            screenWidth, screenHeight,
            true
        );
        
        // Pass 2: Vertical blur (framebuffer → framebuffer1)
        renderBlurPass(
            framebuffer1,
            framebuffer.framebufferTexture,
            framebuffer1.framebufferWidth,
            framebuffer1.framebufferHeight,
            0.0F, 1.0F,
            screenWidth, screenHeight,
            false
        );
        
        // Pass 3: Horizontal blur (framebuffer1 → framebuffer2)
        renderBlurPass(
            framebuffer2,
            framebuffer1.framebufferTexture,
            framebuffer2.framebufferWidth,
            framebuffer2.framebufferHeight,
            1.0F, 0.0F,
            screenWidth, screenHeight,
            false
        );
        
        // Pass 4: Vertical blur (framebuffer2 → framebuffer3)
        renderBlurPass(
            framebuffer3,
            framebuffer2.framebufferTexture,
            framebuffer3.framebufferWidth,
            framebuffer3.framebufferHeight,
            0.0F, 1.0F,
            screenWidth, screenHeight,
            false
        );
        
        shader.detach();
        GL11.glPopAttrib();
    }
    
    private void renderBlurPass(
        Framebuffer target,
        int sourceTexture,
        int texWidth,
        int texHeight,
        float dirX,
        float dirY,
        float renderWidth,
        float renderHeight,
        boolean ignoreAlpha
    ) {
        if (target == null || shader == null) return;
        
        target.bindFramebuffer(true);
        
        bindTexture(sourceTexture, 10);
        shader.uniform(Uniform.makeInt(UNIFORM_TEXTURE, 10));
        shader.uniform(Uniform.makeVec2(UNIFORM_DIRECTION, dirX, dirY));
        shader.uniform(Uniform.makeVec2(UNIFORM_TEXEL_SIZE, 1.0F / texWidth, 1.0F / texHeight));
        shader.uniform(Uniform.makeFloatBuffer(UNIFORM_KERNELS, weightBuffer));
        
        if (ignoreAlpha) {
            shader.uniform(Uniform.makeInt(UNIFORM_IGNORE_ALPHA, 1));
        }
        
        shader.rect(0.0F, 0.0F, renderWidth, renderHeight);
    }
    
    private void bindTexture(int texture, int id) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + id);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }
    
    // ========================================
    // GAUSSIAN CALCULATION
    // ========================================
    private float calculateGaussian(float x, float sigma) {
        double sigmaSq = sigma * sigma;
        double output = 1.0 / Math.sqrt(2.0 * PI * sigmaSq);
        return (float) (output * Math.exp(-(x * x) / (2.0 * sigmaSq)));
    }
    
    // ========================================
    // DRAW BLUR (PUBLIC API)
    // ========================================
    public void drawBlur(float x, float y, float w, float h, float radius) {
        if (!shouldRender()) return;
        
        Glide instance = Glide.getInstance();
        if (instance == null || instance.getNanoVGManager() == null) return;
        
        long ctx = instance.getNanoVGManager().getContext();
        ScaledResolution sr = new ScaledResolution(mc);
        
        ComboSetting setting = InternalSettingsMod.getInstance().getModThemeSetting();
        if (setting == null) return;
        
        Option theme = setting.getOption();
        if (theme == null) return;
        
        boolean rectShape = theme.getTranslate().equals(TranslateText.RECT) || 
                           theme.getTranslate().equals(TranslateText.GRADIENT_SIMPLE);
        
        NVGPaint paint = NVGPaint.calloc();
        
        NanoVG.nvgBeginPath(ctx);
        
        if (rectShape) {
            NanoVG.nvgRect(ctx, x, y, w, h);
        } else {
            NanoVG.nvgRoundedRect(ctx, x, y, w, h, radius);
        }
        
        NanoVG.nvgImagePattern(ctx, 0.0F, 0.0F, sr.getScaledWidth(), sr.getScaledHeight(), 0.0F, nvgImage, 1.0F, paint);
        NanoVG.nvgFillPaint(ctx, paint);
        NanoVG.nvgFill(ctx);
        NanoVG.nvgClosePath(ctx);
        
        paint.free();
    }
    
    public void drawBlur(Runnable r) {
        if (!shouldRender() || r == null) return;
        
        Glide instance = Glide.getInstance();
        if (instance == null || instance.getNanoVGManager() == null) return;
        
        long ctx = instance.getNanoVGManager().getContext();
        ScaledResolution sr = new ScaledResolution(mc);
        
        NVGPaint paint = NVGPaint.calloc();
        
        NanoVG.nvgBeginPath(ctx);
        r.run();
        NanoVG.nvgImagePattern(ctx, 0.0F, 0.0F, sr.getScaledWidth(), sr.getScaledHeight(), 0.0F, nvgImage, 1.0F, paint);
        NanoVG.nvgFillPaint(ctx, paint);
        NanoVG.nvgFill(ctx);
        NanoVG.nvgClosePath(ctx);
        
        paint.free();
    }
}