package me.eldodebug.soar.gui.mainmenu.impl;

import java.awt.Color;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.mainmenu.GuiGlideMainMenu;
import me.eldodebug.soar.gui.mainmenu.MainMenuScene;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.utils.mouse.MouseUtils;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.ScaledResolution;

public class MainScene extends MainMenuScene {

	private static final Color TEXT_WHITE = new Color(255, 255, 255, 255);
	private static final int BUTTON_WIDTH = 180;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_SPACING = 26;
	private static final float BUTTON_RADIUS = 4.5F;

	public MainScene(GuiGlideMainMenu parent) {
		super(parent);
	}
	
	@Override
	public void initScene() {
		GlideLogger.info("[MainScene] Initialized");
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		Glide instance = Glide.getInstance();
		if (instance == null) return;
		
		if (instance.getSoar8Released()) {
			instance.setSoar8Released(false);
			this.setCurrentScene(this.getSceneByClass(DiscontinuedSoar8.class));
			return;
		}
		
		if (instance.getUpdateNeeded()) {
			instance.setUpdateNeeded(false);
			this.setCurrentScene(this.getSceneByClass(UpdateScene.class));
			return;
		}
		
		NanoVGManager nvg = instance.getNanoVGManager();
		if (nvg == null) return;

		nvg.setupAndDraw(() -> drawButtons(nvg));
	}
	
	private void drawButtons(NanoVGManager nvg) {
		
		if (mc == null || mc.currentScreen == null) return;
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		float centerX = sr.getScaledWidth() / 2;
		float centerY = sr.getScaledHeight() / 2;
		
		float logoY = centerY - (nvg.getTextHeight(LegacyIcon.SOAR, 54, Fonts.LEGACYICON) / 2) - 60;
		nvg.drawCenteredText(LegacyIcon.SOAR, centerX, logoY, TEXT_WHITE, 54, Fonts.LEGACYICON);
		
		float firstButtonY = centerY - 22;
		
		drawButton(nvg, centerX, firstButtonY, TranslateText.SINGLEPLAYER.getText());
		drawButton(nvg, centerX, firstButtonY + BUTTON_SPACING, TranslateText.MULTIPLAYER.getText());
		drawButton(nvg, centerX, firstButtonY + BUTTON_SPACING * 2, TranslateText.SETTINGS.getText());
	}
	
	private void drawButton(NanoVGManager nvg, float centerX, float y, String text) {
		float buttonX = centerX - (BUTTON_WIDTH / 2);
		
		nvg.drawRoundedRect(buttonX, y, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS, this.getBackgroundColor());
		nvg.drawCenteredText(text, centerX, y + 6.5F, TEXT_WHITE, 9.5F, Fonts.REGULAR);
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		
		if (mouseButton != 0 || mc == null || mc.currentScreen == null) return;
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		float centerX = sr.getScaledWidth() / 2;
		float centerY = sr.getScaledHeight() / 2;
		float firstButtonY = centerY - 22;
		float buttonX = centerX - (BUTTON_WIDTH / 2);
		
		if (MouseUtils.isInside(mouseX, mouseY, buttonX, firstButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
			mc.displayGuiScreen(new GuiSelectWorld(this.getParent()));
		}
		
		if (MouseUtils.isInside(mouseX, mouseY, buttonX, firstButtonY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)) {
			mc.displayGuiScreen(new GuiMultiplayer(this.getParent()));
		}
		
		if (MouseUtils.isInside(mouseX, mouseY, buttonX, firstButtonY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT)) {
			mc.displayGuiScreen(new GuiOptions(this.getParent(), mc.gameSettings));
		}
	}
}