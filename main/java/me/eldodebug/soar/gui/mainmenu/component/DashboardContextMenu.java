package me.eldodebug.soar.gui.mainmenu.component;

import java.awt.Color;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.utils.mouse.MouseUtils;
import net.minecraft.client.Minecraft;

public class DashboardContextMenu {

    private static final Minecraft mc = Minecraft.getMinecraft();
    
    private static final float MENU_WIDTH = 200.0F;
    private static final float MENU_ITEM_HEIGHT = 32.0F;
    private static final float MENU_RADIUS = 8.0F;
    private static final float MENU_PADDING = 8.0F;
    
    private static final Color MENU_BG = new Color(30, 30, 35, 240);
    private static final Color ITEM_HOVER = new Color(60, 60, 70, 255);
    private static final Color TEXT_COLOR = new Color(255, 255, 255, 255);
    
    private float x, y;
    private String worldName;
    private boolean hasCustom;
    private Runnable onUpdate;
    
    private int hoveredIndex = -1;
    
    public DashboardContextMenu(float x, float y, String worldName, boolean hasCustom, Runnable onUpdate) {
        this.x = x;
        this.y = y;
        this.worldName = worldName;
        this.hasCustom = hasCustom;
        this.onUpdate = onUpdate;
    }
    
    public void draw(NanoVGManager nvg, int mouseX, int mouseY) {
        if (nvg == null) return;
        
        int itemCount = hasCustom ? 2 : 1;
        float menuHeight = (MENU_ITEM_HEIGHT * itemCount) + (MENU_PADDING * 2);
        
        nvg.drawShadow(x, y, MENU_WIDTH, menuHeight, MENU_RADIUS);
        nvg.drawRoundedRect(x, y, MENU_WIDTH, menuHeight, MENU_RADIUS, MENU_BG);
        
        hoveredIndex = -1;
        
        float itemY = y + MENU_PADDING;
        
        if (drawMenuItem(nvg, "Set Custom Screenshot", itemY, mouseX, mouseY, 0)) {
            hoveredIndex = 0;
        }
        itemY += MENU_ITEM_HEIGHT;
        
        if (hasCustom) {
            if (drawMenuItem(nvg, "Remove Screenshot", itemY, mouseX, mouseY, 1)) {
                hoveredIndex = 1;
            }
        }
    }
    
    private boolean drawMenuItem(NanoVGManager nvg, String text, float itemY, int mouseX, int mouseY, int index) {
        if (nvg == null || text == null) return false;
        
        boolean hovered = MouseUtils.isInside(mouseX, mouseY, x, itemY, MENU_WIDTH, MENU_ITEM_HEIGHT);
        
        if (hovered) {
            nvg.drawRoundedRect(x + 4, itemY, MENU_WIDTH - 8, MENU_ITEM_HEIGHT, 4, ITEM_HOVER);
        }
        
        nvg.drawText(text, x + 12, itemY + 10, TEXT_COLOR, 13, Fonts.REGULAR);
        
        return hovered;
    }
    
    public boolean handleClick(int mouseX, int mouseY) {
        if (hoveredIndex == -1) {
            return false;
        }
        
        if (hoveredIndex == 0) {
            openFileChooser();
        } else if (hoveredIndex == 1) {
            removeScreenshot();
        }
        
        return true;
    }
    
    private void openFileChooser() {
        if (mc == null || mc.mcDataDir == null) {
            GlideLogger.error("[ContextMenu] mc or mcDataDir is null");
            return;
        }
        
        Thread thread = new Thread(() -> {
            try {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select Screenshot for " + worldName);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Images (PNG, JPG)", "png", "jpg", "jpeg");
                fileChooser.setFileFilter(filter);
                
                File screenshotsDir = new File(mc.mcDataDir, "screenshots");
                if (screenshotsDir.exists()) {
                    fileChooser.setCurrentDirectory(screenshotsDir);
                }
                
                int result = fileChooser.showOpenDialog(null);
                
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    
                    if (selectedFile != null && selectedFile.exists()) {
                        Glide instance = Glide.getInstance();
                        if (instance == null) return;
                        
                        instance.getWorldScreenshotManager().setScreenshot(worldName, selectedFile);
                        
                        if (onUpdate != null) {
                            onUpdate.run();
                        }
                        
                        GlideLogger.info("[ContextMenu] Screenshot set for: " + worldName);
                    }
                }
                
            } catch (Exception e) {
                GlideLogger.error("[ContextMenu] File chooser error", e);
            }
        }, "FileChooser-Thread");
        
        thread.setDaemon(true);
        thread.start();
    }
    
    private void removeScreenshot() {
        if (worldName == null || worldName.isEmpty()) {
            return;
        }
        
        Glide instance = Glide.getInstance();
        if (instance == null) return;
        
        instance.getWorldScreenshotManager().removeScreenshot(worldName);
        
        if (onUpdate != null) {
            onUpdate.run();
        }
        
        GlideLogger.info("[ContextMenu] Screenshot removed for: " + worldName);
    }
    
    public boolean isInside(int mouseX, int mouseY) {
        int itemCount = hasCustom ? 2 : 1;
        float menuHeight = (MENU_ITEM_HEIGHT * itemCount) + (MENU_PADDING * 2);
        
        return MouseUtils.isInside(mouseX, mouseY, x, y, MENU_WIDTH, menuHeight);
    }
}