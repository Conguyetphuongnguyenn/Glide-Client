package me.eldodebug.soar.gui.mainmenu.impl;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.utils.animation.normal.Animation;
import me.eldodebug.soar.utils.animation.normal.Direction;
import me.eldodebug.soar.utils.animation.normal.easing.EaseInOutCirc;
import org.lwjgl.input.Keyboard;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.mainmenu.GuiGlideMainMenu;
import me.eldodebug.soar.gui.mainmenu.MainMenuScene;
import me.eldodebug.soar.management.file.FileManager;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.management.profile.mainmenu.BackgroundManager;
import me.eldodebug.soar.management.profile.mainmenu.impl.Background;
import me.eldodebug.soar.management.profile.mainmenu.impl.CustomBackground;
import me.eldodebug.soar.management.profile.mainmenu.impl.DefaultBackground;
import me.eldodebug.soar.utils.Multithreading;
import me.eldodebug.soar.utils.file.FileUtils;
import me.eldodebug.soar.utils.mouse.MouseUtils;
import me.eldodebug.soar.utils.mouse.Scroll;
import net.minecraft.client.gui.ScaledResolution;

public class BackgroundScene extends MainMenuScene {

	private static final Color PANEL_BG = new Color(230, 230, 230, 120);
	private static final Color TEXT_WHITE = new Color(255, 255, 255, 255);
	private static final Color ITEM_SELECTED = new Color(255, 255, 255, 180);
	private static final Color ITEM_HOVER = new Color(255, 255, 255, 100);
	private static final Color TRASH_COLOR = new Color(255, 80, 80, 255);
	
	private static final int PANEL_WIDTH = 240;
	private static final int PANEL_HEIGHT = 148;
	private static final int ITEM_WIDTH = 102;
	private static final int ITEM_HEIGHT = 57;
	private static final int ITEM_SPACING = 12;
	private static final int ITEMS_PER_ROW = 2;

	private Animation introAnimation;
	private final Scroll scroll = new Scroll();
	
	private float scaleProgress = 1.0F;

	public BackgroundScene(GuiGlideMainMenu parent) {
		super(parent);
	}

	@Override
	public void initScene() {
		introAnimation = new EaseInOutCirc(250, 1.0F);
		introAnimation.setDirection(Direction.FORWARDS);
		scroll.setMaxScroll(0);
		scaleProgress = 1.0F;
		
		GlideLogger.info("[BackgroundScene] Initialized");
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		if (mc == null) return;
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		Glide instance = Glide.getInstance();
		if (instance == null) return;
		
		NanoVGManager nvg = instance.getNanoVGManager();
		if (nvg == null) return;
		
		scroll.onScroll();
		scroll.onAnimation();
		
		scaleProgress = 2.0F - introAnimation.getValueFloat();
		float alphaProgress = Math.min(introAnimation.getValueFloat(), 1.0F);
		
		nvg.setupAndDraw(() -> drawPanel(mouseX, mouseY, sr, instance, nvg, alphaProgress));
		
		if (introAnimation.isDone(Direction.BACKWARDS)) {
			this.setCurrentScene(this.getSceneByClass(DashboardScene.class));
		}
	}

	private void drawPanel(int mouseX, int mouseY, ScaledResolution sr, Glide instance, NanoVGManager nvg, float alpha) {
		
		BackgroundManager backgroundManager = instance.getProfileManager().getBackgroundManager();
		
		int panelX = sr.getScaledWidth() / 2 - (PANEL_WIDTH / 2);
		int panelY = sr.getScaledHeight() / 2 - (PANEL_HEIGHT / 2);
		
		nvg.save();
		nvg.scale(sr.getScaledWidth() / 2, sr.getScaledHeight() / 2, PANEL_WIDTH, PANEL_HEIGHT, scaleProgress);
		nvg.setAlpha(alpha);
		
		Color panelBg = new Color(PANEL_BG.getRed(), PANEL_BG.getGreen(), PANEL_BG.getBlue(), 
			(int)(PANEL_BG.getAlpha() * alpha));
		nvg.drawRoundedRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 8, panelBg);
		
		Color textColor = new Color(TEXT_WHITE.getRed(), TEXT_WHITE.getGreen(), TEXT_WHITE.getBlue(), 
			(int)(255 * alpha));
		nvg.drawCenteredText(TranslateText.SELECT_BACKGROUND.getText(), panelX + (PANEL_WIDTH / 2), 
			panelY + 8, textColor, 14, Fonts.SEMIBOLD);

		nvg.save();
		nvg.scissor(panelX, panelY + 25, PANEL_WIDTH, PANEL_HEIGHT - 25);
		nvg.translate(0, scroll.getValue());

		drawBackgroundItems(mouseX, mouseY, panelX, panelY, backgroundManager, nvg, alpha);

		nvg.restore();
		nvg.restore();
	}
	
	private void drawBackgroundItems(int mouseX, int mouseY, int panelX, int panelY, 
									 BackgroundManager backgroundManager, NanoVGManager nvg, float alpha) {
		
		int offsetX = 0;
		int offsetY = 0;
		int index = 0;
		int maxScrollY = 0;

		for (Background bg : backgroundManager.getBackgrounds()) {
			boolean isSelected = backgroundManager.getCurrentBackground().equals(bg);
			
			float itemX = panelX + 11 + offsetX;
			float itemY = panelY + 35 + offsetY;
			
			boolean hovered = MouseUtils.isInside(mouseX, mouseY, itemX, itemY + scroll.getValue(), ITEM_WIDTH, ITEM_HEIGHT);

			if (isSelected) {
				Color selectedColor = new Color(ITEM_SELECTED.getRed(), ITEM_SELECTED.getGreen(), ITEM_SELECTED.getBlue(), 
					(int)(ITEM_SELECTED.getAlpha() * alpha));
				nvg.drawRoundedRect(itemX - 1, itemY - 1, ITEM_WIDTH + 2, ITEM_HEIGHT + 2, 7, selectedColor);
			}

			if (hovered) {
				Color hoverColor = new Color(ITEM_HOVER.getRed(), ITEM_HOVER.getGreen(), ITEM_HOVER.getBlue(), 
					(int)(ITEM_HOVER.getAlpha() * alpha));
				nvg.drawRoundedRect(itemX - 1, itemY - 1, ITEM_WIDTH + 2, ITEM_HEIGHT + 2, 7, hoverColor);
			}

			if (bg instanceof DefaultBackground) {
				DefaultBackground defBg = (DefaultBackground) bg;

				if (bg.getId() == 999) {
					nvg.drawRoundedRect(itemX, itemY, ITEM_WIDTH, ITEM_HEIGHT, 6, Color.BLACK);
					Color plusColor = new Color(TEXT_WHITE.getRed(), TEXT_WHITE.getGreen(), TEXT_WHITE.getBlue(), 
						(int)(255 * alpha));
					nvg.drawCenteredText(LegacyIcon.PLUS, itemX + ITEM_WIDTH / 2, itemY + 7.5F, plusColor, 26, Fonts.LEGACYICON);
				} else {
					nvg.drawRoundedImage(defBg.getImage(), itemX, itemY, ITEM_WIDTH, ITEM_HEIGHT, 6);
				}
			}

			if (bg instanceof CustomBackground) {
				CustomBackground cusBg = (CustomBackground) bg;

				cusBg.getTrashAnimation().setAnimation(hovered ? 1.0F : 0.0F, 16);

				nvg.drawRoundedImage(cusBg.getImage(), itemX, itemY, ITEM_WIDTH, ITEM_HEIGHT, 6);
				
				int trashAlpha = (int)(cusBg.getTrashAnimation().getValue() * 255 * alpha);
				Color trashColor = new Color(TRASH_COLOR.getRed(), TRASH_COLOR.getGreen(), TRASH_COLOR.getBlue(), trashAlpha);
				nvg.drawText(LegacyIcon.TRASH, itemX + ITEM_WIDTH - 13, itemY + 3, trashColor, 10, Fonts.LEGACYICON);
			}

			Color nameColor = new Color(TEXT_WHITE.getRed(), TEXT_WHITE.getGreen(), TEXT_WHITE.getBlue(), 
				(int)(255 * alpha));
			nvg.drawRoundedRectVarying(itemX, itemY + ITEM_HEIGHT + 0.5F, ITEM_WIDTH, 16, 0, 0, 6, 6, this.getBackgroundColor());
			nvg.drawCenteredText(bg.getName(), itemX + ITEM_WIDTH / 2, itemY + ITEM_HEIGHT + 4, nameColor, 10, Fonts.REGULAR);

			offsetX += ITEM_WIDTH + ITEM_SPACING;

			if ((index + 1) % ITEMS_PER_ROW == 0) {
				maxScrollY = offsetY + ITEM_HEIGHT + 70;
				offsetY += 70;
				offsetX = 0;
			}

			index++;
		}

		scroll.setMaxScroll(Math.max(0, maxScrollY - PANEL_HEIGHT + 60));
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		
		if (mc == null) return;
		
		ScaledResolution sr = new ScaledResolution(mc);

		Glide instance = Glide.getInstance();
		if (instance == null) return;
		
		FileManager fileManager = instance.getFileManager();
		BackgroundManager backgroundManager = instance.getProfileManager().getBackgroundManager();

		int panelX = sr.getScaledWidth() / 2 - (PANEL_WIDTH / 2);
		int panelY = sr.getScaledHeight() / 2 - (PANEL_HEIGHT / 2);
		int offsetX = 0;
		int offsetY = (int)scroll.getValue();
		int index = 0;

		if (!MouseUtils.isInside(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT)) {
			if (!MouseUtils.isInside(mouseX, mouseY, sr.getScaledWidth() - 28 - 28, 6, 22, 22)) {
				introAnimation.setDirection(Direction.BACKWARDS);
			}
			return;
		}

		for (Background bg : backgroundManager.getBackgrounds()) {

			float itemX = panelX + 11 + offsetX;
			float itemY = panelY + 35 + offsetY;

			if (mouseButton == 0) {
				if (MouseUtils.isInside(mouseX, mouseY, itemX, itemY, ITEM_WIDTH, ITEM_HEIGHT)) {

					if (bg.getId() == 999) {
						Multithreading.runAsync(() -> {
							File file = FileUtils.selectImageFile();
							
							if (file == null || fileManager == null) return;
							
							File bgCacheDir = new File(fileManager.getCacheDir(), "background");

							if (file.exists() && bgCacheDir.exists() && FileUtils.getExtension(file).equals("png")) {
								File destFile = new File(bgCacheDir, file.getName());

								try {
									FileUtils.copyFile(file, destFile);
									backgroundManager.addCustomBackground(destFile);
									GlideLogger.info("[BackgroundScene] Added custom background: " + file.getName());
								} catch (IOException e) {
									GlideLogger.error("[BackgroundScene] Failed to copy background", e);
								}
							}
						});
					} else {
						backgroundManager.setCurrentBackground(bg);
					}
				}

				if (bg instanceof CustomBackground) {
					CustomBackground cusBg = (CustomBackground) bg;
					
					if (MouseUtils.isInside(mouseX, mouseY, itemX + ITEM_WIDTH - 15, itemY + 1, 14, 14)) {

						if (backgroundManager.getCurrentBackground().equals(cusBg)) {
							backgroundManager.setCurrentBackground(backgroundManager.getBackgroundById(0));
						}

						backgroundManager.removeCustomBackground(cusBg);
						GlideLogger.info("[BackgroundScene] Removed custom background");
					}
				}
			}

			offsetX += ITEM_WIDTH + ITEM_SPACING;

			if ((index + 1) % ITEMS_PER_ROW == 0) {
				offsetY += 70;
				offsetX = 0;
			}

			index++;
		}
	}

	@Override
	public void keyTyped(char typedChar, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			introAnimation.setDirection(Direction.BACKWARDS);
		}
	}
	
	@Override
	public void onSceneClosed() {
		scroll.setMaxScroll(0);
		GlideLogger.info("[BackgroundScene] Closed");
	}
}