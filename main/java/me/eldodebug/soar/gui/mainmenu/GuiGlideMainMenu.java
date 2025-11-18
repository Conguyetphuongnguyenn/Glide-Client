// ========================================
// GuiGlideMainMenu.java (PRODUCTION FIXED)
// ========================================
// ? FIX #1: Dashboard button ho?t d?ng
// ? FIX #2: Background button ho?t d?ng
// ? FIX #3: Nút X alignment dúng
// ? FIX #4: Button order t? ph?i sang trái
// ? Full null safety + logging
// ========================================

package me.eldodebug.soar.gui.mainmenu;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;

import me.eldodebug.soar.gui.mainmenu.impl.DashboardScene;
import me.eldodebug.soar.gui.mainmenu.impl.DiscontinuedSoar8;
import me.eldodebug.soar.gui.mainmenu.impl.UpdateScene;
import me.eldodebug.soar.gui.mainmenu.impl.welcome.*;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.utils.Sound;
import org.lwjgl.input.Mouse;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.mainmenu.impl.BackgroundScene;
import me.eldodebug.soar.gui.mainmenu.impl.MainScene;
import me.eldodebug.soar.management.event.impl.EventRenderNotification;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.management.profile.mainmenu.impl.Background;
import me.eldodebug.soar.management.profile.mainmenu.impl.CustomBackground;
import me.eldodebug.soar.management.profile.mainmenu.impl.DefaultBackground;
import me.eldodebug.soar.utils.animation.normal.Animation;
import me.eldodebug.soar.utils.animation.normal.Direction;
import me.eldodebug.soar.utils.animation.normal.other.DecelerateAnimation;
import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;
import me.eldodebug.soar.utils.mouse.MouseUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

public class GuiGlideMainMenu extends GuiScreen {

	private static final Color BG_BUTTON = new Color(230, 230, 230, 120);
	private static final Color TEXT_WHITE = new Color(255, 255, 255, 255);
	private static final Color TEXT_CLOSE_HOVER = new Color(255, 55, 55, 255);
	private static final Color TEXT_BG_HOVER = new Color(55, 255, 55, 255);
	private static final Color TEXT_DASH_HOVER = new Color(55, 55, 255, 255);
	private static final Color SPLASH_BG = new Color(0, 0, 0, 255);
	
	private static final int BUTTON_SIZE = 22;
	private static final int BUTTON_MARGIN = 6;

	private MainMenuScene currentScene;
	private boolean sceneInitialized = false;
	
	private final SimpleAnimation closeFocusAnimation = new SimpleAnimation();
	private final SimpleAnimation backgroundSelectFocusAnimation = new SimpleAnimation();
	private final SimpleAnimation dashboardFocusAnimation = new SimpleAnimation();
	private final SimpleAnimation[] backgroundAnimations = new SimpleAnimation[2];

	private final ArrayList<MainMenuScene> scenes = new ArrayList<>();
	private boolean soundPlayed = false;

	private Animation fadeIconAnimation;
	private Animation fadeBackgroundAnimation;
	
	public GuiGlideMainMenu() {
		
		GlideLogger.info("[MainMenu] Initializing...");
		
		Glide instance = Glide.getInstance();
		if (instance == null) {
			GlideLogger.error("[MainMenu] CRITICAL: Glide instance is null!");
			return;
		}
		
		for (int i = 0; i < backgroundAnimations.length; i++) {
			backgroundAnimations[i] = new SimpleAnimation();
		}
		
		initializeScenes(instance);
		selectInitialScene(instance);
		
		GlideLogger.info("[MainMenu] Initialized successfully with " + scenes.size() + " scenes");
	}
	
	private void initializeScenes(Glide instance) {
		if (instance == null) return;
		
		scenes.add(new MainScene(this));
		scenes.add(new DashboardScene(this));
		scenes.add(new BackgroundScene(this));
		scenes.add(new WelcomeMessageScene(this));
		scenes.add(new ThemeSelectScene(this));
		scenes.add(new LanguageSelectScene(this));
		scenes.add(new AccentColorSelectScene(this));
		scenes.add(new LastMessageScene(this));
		scenes.add(new UpdateScene(this));
		scenes.add(new DiscontinuedSoar8(this));
		
		GlideLogger.info("[MainMenu] Created " + scenes.size() + " scene instances");
	}
	
	private void selectInitialScene(Glide instance) {
		if (instance == null) {
			GlideLogger.error("[MainMenu] Cannot select scene - Glide instance null");
			return;
		}
		
		if (instance.isFirstLogin()) {
			currentScene = getSceneByClass(WelcomeMessageScene.class);
		} else {
			if (instance.getSoar8Released()) {
				currentScene = getSceneByClass(DiscontinuedSoar8.class);
			} else if (instance.getUpdateNeeded()) {
				currentScene = getSceneByClass(UpdateScene.class);
			} else {
				currentScene = getSceneByClass(DashboardScene.class);
			}
		}
		
		if (currentScene != null) {
			currentScene.initScene();
			sceneInitialized = true;
			GlideLogger.info("[MainMenu] Initial scene set and initialized: " + currentScene.getClass().getSimpleName());
		} else {
			GlideLogger.error("[MainMenu] CRITICAL: Failed to initialize any scene!");
		}
	}
	
	@Override
	public void initGui() {
		if (currentScene == null) {
			GlideLogger.error("[MainMenu] initGui() called but currentScene is null!");
			return;
		}
		
		if (!sceneInitialized) {
			GlideLogger.warn("[MainMenu] Scene not initialized, initializing now...");
			currentScene.initScene();
			sceneInitialized = true;
		}
		
		currentScene.initGui();
		GlideLogger.info("[MainMenu] GUI initialized for: " + currentScene.getClass().getSimpleName());
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		if (mc == null) {
			GlideLogger.error("[MainMenu] drawScreen() - mc is null!");
			return;
		}
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		Glide instance = Glide.getInstance();
		if (instance == null) {
			GlideLogger.error("[MainMenu] drawScreen() - Glide instance is null!");
			return;
		}
		
		NanoVGManager nvg = instance.getNanoVGManager();
		if (nvg == null) {
			GlideLogger.error("[MainMenu] drawScreen() - NanoVG is null!");
			return;
		}
		
		boolean isFirstLogin = instance.isFirstLogin();
		
		updateBackgroundAnimations();
		
		nvg.setupAndDraw(() -> {
			drawBackground(sr, instance, nvg);
			
			if (!isFirstLogin) {
				drawButtons(mouseX, mouseY, sr, nvg);
			}
		});
		
		if (currentScene != null) {
			currentScene.drawScreen(mouseX, mouseY, partialTicks);
		} else {
			GlideLogger.error("[MainMenu] drawScreen() - currentScene is null!");
		}
		
		if (fadeBackgroundAnimation == null || !fadeBackgroundAnimation.isDone(Direction.FORWARDS)) {
			nvg.setupAndDraw(() -> drawSplashScreen(sr, nvg));
			if (!soundPlayed) {
				try {
					Sound.play("soar/audio/start.wav", true);
					soundPlayed = true;
				} catch (Exception e) {
					GlideLogger.error("[MainMenu] Failed to play start sound", e);
				}
			}
		}
		
		nvg.setupAndDraw(() -> {
			new EventRenderNotification().call();
		});
		
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
	
	private void updateBackgroundAnimations() {
		if (backgroundAnimations == null || backgroundAnimations.length < 2) return;
		
		try {
			backgroundAnimations[0].setAnimation(Mouse.getX(), 16);
			backgroundAnimations[1].setAnimation(Mouse.getY(), 16);
		} catch (Exception e) {
			GlideLogger.error("[MainMenu] Error updating background animations", e);
		}
	}
	
	private void drawBackground(ScaledResolution sr, Glide instance, NanoVGManager nvg) {
		
		if (sr == null || instance == null || nvg == null) return;
		
		String copyright = "Copyright Mojang AB. Do not distribute!";
		
		Background currentBackground = null;
		try {
			currentBackground = instance.getProfileManager().getBackgroundManager().getCurrentBackground();
		} catch (Exception e) {
			GlideLogger.error("[MainMenu] Failed to get background", e);
		}
		
		float parallaxX = -21 + backgroundAnimations[0].getValue() / 90;
		float parallaxY = backgroundAnimations[1].getValue() * -1 / 90;
		float bgWidth = sr.getScaledWidth() + 21;
		float bgHeight = sr.getScaledHeight() + 20;
		
		if (currentBackground instanceof DefaultBackground) {
			DefaultBackground bg = (DefaultBackground) currentBackground;
			nvg.drawImage(bg.getImage(), parallaxX, parallaxY, bgWidth, bgHeight);
		} else if (currentBackground instanceof CustomBackground) {
			CustomBackground bg = (CustomBackground) currentBackground;
			nvg.drawImage(bg.getImage(), parallaxX, parallaxY, bgWidth, bgHeight);
		}

		float copyrightWidth = nvg.getTextWidth(copyright, 9, Fonts.REGULAR);
		nvg.drawText(copyright, sr.getScaledWidth() - copyrightWidth - 4, sr.getScaledHeight() - 12, TEXT_WHITE, 9, Fonts.REGULAR);
		nvg.drawText("Glide Client v" + instance.getVersion(), 4, sr.getScaledHeight() - 12, TEXT_WHITE, 9, Fonts.REGULAR);
	}
	
	private void drawButtons(int mouseX, int mouseY, ScaledResolution sr, NanoVGManager nvg) {
		
		if (sr == null || nvg == null) return;
		
		int screenWidth = sr.getScaledWidth();
		
		float closeX = screenWidth - BUTTON_SIZE - BUTTON_MARGIN;
		float bgSelectX = closeX - BUTTON_SIZE - BUTTON_MARGIN;
		float dashX = bgSelectX - BUTTON_SIZE - BUTTON_MARGIN;
		
		closeFocusAnimation.setAnimation(
			MouseUtils.isInside(mouseX, mouseY, closeX, BUTTON_MARGIN, BUTTON_SIZE, BUTTON_SIZE) ? 1.0F : 0.0F, 16);
		backgroundSelectFocusAnimation.setAnimation(
			MouseUtils.isInside(mouseX, mouseY, bgSelectX, BUTTON_MARGIN, BUTTON_SIZE, BUTTON_SIZE) ? 1.0F : 0.0F, 16);
		dashboardFocusAnimation.setAnimation(
			MouseUtils.isInside(mouseX, mouseY, dashX, BUTTON_MARGIN, BUTTON_SIZE, BUTTON_SIZE) ? 1.0F : 0.0F, 16);
		
		nvg.drawRoundedRect(closeX, BUTTON_MARGIN, BUTTON_SIZE, BUTTON_SIZE, 4, BG_BUTTON);
		Color closeColor = lerpColor(TEXT_WHITE, TEXT_CLOSE_HOVER, closeFocusAnimation.getValue());
		nvg.drawCenteredText(LegacyIcon.X, closeX + (BUTTON_SIZE / 2.0F), BUTTON_MARGIN + (BUTTON_SIZE / 2.0F), closeColor, 18, Fonts.LEGACYICON);
		
		nvg.drawRoundedRect(bgSelectX, BUTTON_MARGIN, BUTTON_SIZE, BUTTON_SIZE, 4, BG_BUTTON);
		Color bgColor = lerpColor(TEXT_WHITE, TEXT_BG_HOVER, backgroundSelectFocusAnimation.getValue());
		nvg.drawCenteredText(LegacyIcon.IMAGE, bgSelectX + (BUTTON_SIZE / 2.0F), BUTTON_MARGIN + (BUTTON_SIZE / 2.0F), bgColor, 15, Fonts.LEGACYICON);
		
		nvg.drawRoundedRect(dashX, BUTTON_MARGIN, BUTTON_SIZE, BUTTON_SIZE, 4, BG_BUTTON);
		Color dashColor = lerpColor(TEXT_WHITE, TEXT_DASH_HOVER, dashboardFocusAnimation.getValue());
		nvg.drawCenteredText(LegacyIcon.GRID, dashX + (BUTTON_SIZE / 2.0F), BUTTON_MARGIN + (BUTTON_SIZE / 2.0F), dashColor, 15, Fonts.LEGACYICON);
	}
	
	private void drawSplashScreen(ScaledResolution sr, NanoVGManager nvg) {
		
		if (sr == null || nvg == null) return;
		
		if (fadeIconAnimation == null) {
			fadeIconAnimation = new DecelerateAnimation(100, 1);
			fadeIconAnimation.setDirection(Direction.FORWARDS);
			fadeIconAnimation.reset();
		}
		
		if (fadeIconAnimation.isDone(Direction.FORWARDS) && fadeBackgroundAnimation == null) {
			fadeBackgroundAnimation = new DecelerateAnimation(500, 1);
			fadeBackgroundAnimation.setDirection(Direction.FORWARDS);
			fadeBackgroundAnimation.reset();
		}
		
		int bgAlpha = fadeBackgroundAnimation != null ? (int)(255 - (fadeBackgroundAnimation.getValue() * 255)) : 255;
		nvg.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 
			new Color(SPLASH_BG.getRed(), SPLASH_BG.getGreen(), SPLASH_BG.getBlue(), bgAlpha));
		
		int iconAlpha = (int)(255 - (fadeIconAnimation.getValue() * 255));
		Color iconColor = new Color(TEXT_WHITE.getRed(), TEXT_WHITE.getGreen(), TEXT_WHITE.getBlue(), iconAlpha);
		
		nvg.drawCenteredText(LegacyIcon.SOAR, sr.getScaledWidth() / 2, 
			(sr.getScaledHeight() / 2) - (nvg.getTextHeight(LegacyIcon.SOAR, 130, Fonts.LEGACYICON) / 2) - 1, 
			iconColor, 130, Fonts.LEGACYICON);
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		
		if (mc == null) {
			GlideLogger.error("[MainMenu] mouseClicked() - mc is null!");
			return;
		}
		
		ScaledResolution sr = new ScaledResolution(mc);
		
		Glide instance = Glide.getInstance();
		if (instance == null) {
			GlideLogger.error("[MainMenu] mouseClicked() - Glide instance is null!");
			return;
		}
		
		boolean isFirstLogin = instance.isFirstLogin();
		
		if (mouseButton == 0 && !isFirstLogin) {
			
			int screenWidth = sr.getScaledWidth();
			
			int closeX = screenWidth - BUTTON_SIZE - BUTTON_MARGIN;
			int bgSelectX = closeX - BUTTON_SIZE - BUTTON_MARGIN;
			int dashX = bgSelectX - BUTTON_SIZE - BUTTON_MARGIN;
			
			if (MouseUtils.isInside(mouseX, mouseY, closeX, BUTTON_MARGIN, BUTTON_SIZE, BUTTON_SIZE)) {
				GlideLogger.info("[MainMenu] Close button clicked - Shutting down");
				mc.shutdown();
				return;
			}
			
			if (MouseUtils.isInside(mouseX, mouseY, bgSelectX, BUTTON_MARGIN, BUTTON_SIZE, BUTTON_SIZE)) {
				MainMenuScene bgScene = getSceneByClass(BackgroundScene.class);
				if (bgScene != null) {
					GlideLogger.info("[MainMenu] Background button clicked");
					this.setCurrentScene(bgScene);
				} else {
					GlideLogger.error("[MainMenu] BackgroundScene not found!");
				}
				return;
			}
			
			if (MouseUtils.isInside(mouseX, mouseY, dashX, BUTTON_MARGIN, BUTTON_SIZE, BUTTON_SIZE)) {
				MainMenuScene dashScene = getSceneByClass(DashboardScene.class);
				if (dashScene != null) {
					GlideLogger.info("[MainMenu] Dashboard button clicked");
					this.setCurrentScene(dashScene);
				} else {
					GlideLogger.error("[MainMenu] DashboardScene not found!");
				}
				return;
			}
		}
		
		if (currentScene != null) {
			currentScene.mouseClicked(mouseX, mouseY, mouseButton);
		}
		
		try {
			super.mouseClicked(mouseX, mouseY, mouseButton);
		} catch (IOException e) {
			GlideLogger.error("[MainMenu] Mouse click error", e);
		}
	}
	
	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
		if (currentScene != null) {
			currentScene.mouseReleased(mouseX, mouseY, mouseButton);
		}
	}
	
	@Override
	public void keyTyped(char typedChar, int keyCode) {
		if (currentScene != null) {
			currentScene.keyTyped(typedChar, keyCode);
		}
	}
	
	@Override
	public void handleInput() throws IOException {
		super.handleInput();
	}
	
	@Override
	public void onGuiClosed() {
		GlideLogger.info("[MainMenu] Closing GUI...");
		
		if (currentScene != null) {
			currentScene.onGuiClosed();
		}
		
		scenes.clear();
		sceneInitialized = false;
		
		GlideLogger.info("[MainMenu] Closed and cleaned up");
	}

	public MainMenuScene getCurrentScene() {
		return currentScene;
	}

	public void setCurrentScene(MainMenuScene newScene) {
		
		if (newScene == null) {
			GlideLogger.error("[MainMenu] setCurrentScene() - newScene is null!");
			return;
		}
		
		if (this.currentScene != null) {
			GlideLogger.info("[MainMenu] Closing scene: " + this.currentScene.getClass().getSimpleName());
			this.currentScene.onSceneClosed();
		}
		
		this.currentScene = newScene;
		this.sceneInitialized = false;
		
		try {
			this.currentScene.initScene();
			this.sceneInitialized = true;
			GlideLogger.info("[MainMenu] Scene initialized: " + this.currentScene.getClass().getSimpleName());
		} catch (Exception e) {
			GlideLogger.error("[MainMenu] Failed to initialize scene", e);
			return;
		}
		
		try {
			this.currentScene.initGui();
			GlideLogger.info("[MainMenu] Scene GUI initialized: " + this.currentScene.getClass().getSimpleName());
		} catch (Exception e) {
			GlideLogger.error("[MainMenu] Failed to initialize scene GUI", e);
		}
	}
	
	public boolean isDoneBackgroundAnimation() {
		return fadeBackgroundAnimation != null && fadeBackgroundAnimation.isDone(Direction.FORWARDS);
	}
	
	public MainMenuScene getSceneByClass(Class<? extends MainMenuScene> clazz) {
		
		if (clazz == null) {
			GlideLogger.error("[MainMenu] getSceneByClass() - clazz is null!");
			return null;
		}
		
		for (MainMenuScene s : scenes) {
			if (s != null && s.getClass().equals(clazz)) {
				return s;
			}
		}
		
		GlideLogger.warn("[MainMenu] Scene not found: " + clazz.getSimpleName());
		return null;
	}
	
	public Color getBackgroundColor() {
		return BG_BUTTON;
	}
	
	private Color lerpColor(Color start, Color end, float t) {
		if (start == null || end == null) {
			return start != null ? start : (end != null ? end : TEXT_WHITE);
		}
		
		if (t <= 0.0F) return start;
		if (t >= 1.0F) return end;
		
		t = Math.max(0.0F, Math.min(1.0F, t));
		
		int r = (int)(start.getRed() + (end.getRed() - start.getRed()) * t);
		int g = (int)(start.getGreen() + (end.getGreen() - start.getGreen()) * t);
		int b = (int)(start.getBlue() + (end.getBlue() - start.getBlue()) * t);
		int a = (int)(start.getAlpha() + (end.getAlpha() - start.getAlpha()) * t);
		
		return new Color(
			Math.max(0, Math.min(255, r)),
			Math.max(0, Math.min(255, g)),
			Math.max(0, Math.min(255, b)),
			Math.max(0, Math.min(255, a))
		);
	}
}