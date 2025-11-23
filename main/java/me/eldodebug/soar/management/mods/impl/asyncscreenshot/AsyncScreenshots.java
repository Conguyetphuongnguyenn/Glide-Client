package me.eldodebug.soar.management.mods.impl.asyncscreenshot;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.mods.impl.AsyncScreenshotMod;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

public class AsyncScreenshots extends Thread {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    
    private final int width;
    private final int height;
    private final int[] pixelValues;
    
    public AsyncScreenshots(int width, int height, int[] pixelValues) {
        super("Glide-ScreenshotThread");
        this.width = width;
        this.height = height;
        this.pixelValues = pixelValues;
    }
    
    @Override
    public void run() {
        if (mc == null || pixelValues == null) {
            GlideLogger.error("[AsyncScreenshot] mc or pixelValues is null");
            return;
        }
        
        try {
            processPixelValues(pixelValues, width, height);
            
            File screenshotFile = getTimestampedPNGFile();
            if (screenshotFile == null) {
                GlideLogger.error("[AsyncScreenshot] Failed to create screenshot file");
                return;
            }
            
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, width, height, pixelValues, 0, width);
            
            ImageIO.write(image, "png", screenshotFile);
            
            sendChatMessage(screenshotFile);
            
            handleClipboard(screenshotFile);
            
        } catch (Exception e) {
            GlideLogger.error("[AsyncScreenshot] Failed to save screenshot", e);
        }
    }
    
    private void sendChatMessage(File screenshot) {
        if (mc.ingameGUI == null || mc.ingameGUI.getChatGUI() == null) {
            return;
        }
        
        AsyncScreenshotMod mod = AsyncScreenshotMod.getInstance();
        if (mod == null || mod.getMessageSetting() == null) {
            return;
        }
        
        if (!mod.getMessageSetting().isToggled()) {
            return;
        }
        
        String fileName = screenshot.getName();
        
        ChatComponentText main = new ChatComponentText(EnumChatFormatting.UNDERLINE + "Saved screenshot" + EnumChatFormatting.RESET + " ");
        
        ChatComponentText openBtn = new ChatComponentText("[Open] ");
        openBtn.setChatStyle(new ChatStyle()
            .setColor(EnumChatFormatting.GOLD)
            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ".soarcmd screenshot open " + fileName)));
        
        ChatComponentText copyBtn = new ChatComponentText("[Copy] ");
        copyBtn.setChatStyle(new ChatStyle()
            .setColor(EnumChatFormatting.BLUE)
            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ".soarcmd screenshot copy " + fileName)));
        
        ChatComponentText delBtn = new ChatComponentText("[Delete]");
        delBtn.setChatStyle(new ChatStyle()
            .setColor(EnumChatFormatting.RED)
            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ".soarcmd screenshot del " + fileName)));
        
        main.appendSibling(openBtn).appendSibling(copyBtn).appendSibling(delBtn);
        
        mc.ingameGUI.getChatGUI().printChatMessage(main);
    }
    
    private void handleClipboard(File screenshot) {
        if (mc.thePlayer == null) {
            return;
        }
        
        AsyncScreenshotMod mod = AsyncScreenshotMod.getInstance();
        if (mod == null || mod.getClipboardSetting() == null) {
            return;
        }
        
        if (mod.getClipboardSetting().isToggled()) {
            mc.thePlayer.sendChatMessage(".soarcmd screenshot copy " + screenshot.getName());
        }
    }
    
    private void processPixelValues(int[] pixels, int displayWidth, int displayHeight) {
        if (pixels == null || displayWidth <= 0 || displayHeight <= 0) {
            return;
        }
        
        int[] tempRow = new int[displayWidth];
        int halfHeight = displayHeight >> 1;
        
        for (int y = 0; y < halfHeight; y++) {
            int topRowStart = y * displayWidth;
            int bottomRowStart = (displayHeight - 1 - y) * displayWidth;
            
            System.arraycopy(pixels, topRowStart, tempRow, 0, displayWidth);
            System.arraycopy(pixels, bottomRowStart, pixels, topRowStart, displayWidth);
            System.arraycopy(tempRow, 0, pixels, bottomRowStart, displayWidth);
        }
    }
    
    private File getTimestampedPNGFile() {
        Glide glide = Glide.getInstance();
        if (glide == null || glide.getFileManager() == null) {
            GlideLogger.error("[AsyncScreenshot] Glide or FileManager is null");
            return null;
        }
        
        File screenshotDir = glide.getFileManager().getScreenshotDir();
        if (screenshotDir == null) {
            GlideLogger.error("[AsyncScreenshot] Screenshot directory is null");
            return null;
        }
        
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs();
        }
        
        String dateStamp = DATE_FORMAT.format(new Date());
        int counter = 1;
        
        File screenshot;
        
        while (true) {
            String suffix = (counter == 1) ? "" : ("_" + counter);
            screenshot = new File(screenshotDir, dateStamp + suffix + ".png");
            
            if (!screenshot.exists()) {
                break;
            }
            
            counter++;
            
            if (counter > 9999) {
                GlideLogger.error("[AsyncScreenshot] Too many screenshots with same timestamp");
                return null;
            }
        }
        
        return screenshot;
    }
    
    // ========================================
    // BACKWARD COMPATIBILITY: Static method for Mixin
    // ========================================
    public static File getTimestampedPNGFileForDirectory() {
        Glide glide = Glide.getInstance();
        if (glide == null || glide.getFileManager() == null) {
            return null;
        }
        
        File screenshotDir = glide.getFileManager().getScreenshotDir();
        if (screenshotDir == null) {
            return null;
        }
        
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs();
        }
        
        String dateFormatting = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        int screenshotCount = 1;
        File screenshot;

        while (true) {
            screenshot = new File(
                screenshotDir,
                dateFormatting + ((screenshotCount == 1) ? "" : ("_" + screenshotCount)) + ".png"
            );
            
            if (!screenshot.exists()) {
                break;
            }

            ++screenshotCount;
            
            if (screenshotCount > 9999) {
                break;
            }
        }

        return screenshot;
    }
}