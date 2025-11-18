package me.eldodebug.soar.management.screenshot;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import me.eldodebug.soar.logger.GlideLogger;
import net.minecraft.client.Minecraft;

public class WorldScreenshotManager {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final File storageFile;
    private final Map<String, String> worldToScreenshot;
    
    public WorldScreenshotManager() {
        this.worldToScreenshot = new HashMap<>();
        
        if (mc == null || mc.mcDataDir == null) {
            GlideLogger.error("[ScreenshotManager] mc or mcDataDir is null!");
            this.storageFile = null;
            return;
        }
        
        File glideDir = new File(mc.mcDataDir, "glide");
        if (!glideDir.exists()) {
            glideDir.mkdirs();
        }
        
        this.storageFile = new File(glideDir, "world-screenshots.json");
        load();
    }
    
    public void setScreenshot(String worldName, File screenshotFile) {
        if (worldName == null || worldName.isEmpty()) {
            GlideLogger.warn("[ScreenshotManager] worldName is null/empty");
            return;
        }
        
        if (screenshotFile == null || !screenshotFile.exists()) {
            GlideLogger.warn("[ScreenshotManager] screenshotFile invalid");
            return;
        }
        
        String relativePath = screenshotFile.getAbsolutePath();
        worldToScreenshot.put(worldName, relativePath);
        
        save();
        
        GlideLogger.info("[ScreenshotManager] Set screenshot for '" + worldName + "': " + relativePath);
    }
    
    public void removeScreenshot(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return;
        }
        
        worldToScreenshot.remove(worldName);
        
        save();
        
        GlideLogger.info("[ScreenshotManager] Removed screenshot for '" + worldName + "'");
    }
    
    public File getScreenshotFile(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return null;
        }
        
        String path = worldToScreenshot.get(worldName);
        if (path == null) {
            return null;
        }
        
        File file = new File(path);
        if (!file.exists()) {
            GlideLogger.warn("[ScreenshotManager] Screenshot not found: " + path);
            worldToScreenshot.remove(worldName);
            save();
            return null;
        }
        
        return file;
    }
    
    public boolean hasCustomScreenshot(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return false;
        }
        
        return worldToScreenshot.containsKey(worldName);
    }
    
    private void load() {
        if (storageFile == null || !storageFile.exists()) {
            GlideLogger.info("[ScreenshotManager] No storage file, starting fresh");
            return;
        }
        
        FileReader reader = null;
        try {
            reader = new FileReader(storageFile);
            
            Map<String, String> loaded = GSON.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
            
            if (loaded != null) {
                worldToScreenshot.putAll(loaded);
                GlideLogger.info("[ScreenshotManager] Loaded " + loaded.size() + " mappings");
            }
            
        } catch (Exception e) {
            GlideLogger.error("[ScreenshotManager] Failed to load", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    private void save() {
        if (storageFile == null) {
            return;
        }
        
        FileWriter writer = null;
        try {
            writer = new FileWriter(storageFile);
            GSON.toJson(worldToScreenshot, writer);
            
            GlideLogger.info("[ScreenshotManager] Saved " + worldToScreenshot.size() + " mappings");
            
        } catch (Exception e) {
            GlideLogger.error("[ScreenshotManager] Failed to save", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    public void cleanup() {
        worldToScreenshot.clear();
        GlideLogger.info("[ScreenshotManager] Cleanup complete");
    }
}