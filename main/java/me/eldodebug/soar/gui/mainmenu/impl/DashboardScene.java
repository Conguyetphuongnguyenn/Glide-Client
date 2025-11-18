// ========================================
// DashboardScene.java (ADAPTIVE LAYOUT FIX)
// ========================================
// ✅ Auto-scale để fit tất cả cards trong màn hình
// ✅ Debug logging đầy đủ
// ✅ Icon & text alignment chính xác
// ========================================

package me.eldodebug.soar.gui.mainmenu.impl;

import java.awt.Color;
import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.mainmenu.GuiGlideMainMenu;
import me.eldodebug.soar.gui.mainmenu.MainMenuScene;
import me.eldodebug.soar.gui.mainmenu.component.DashboardContextMenu;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.utils.animation.md3.MD3Animation;
import me.eldodebug.soar.utils.mouse.MouseUtils;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

public class DashboardScene extends MainMenuScene {

    // ========================================
    // CONSTANTS
    // ========================================
    private static final Color WHITE = new Color(255, 255, 255, 255);
    private static final Color GRAY_LIGHT = new Color(230, 230, 240, 220);
    private static final Color BLACK_TRANS = new Color(0, 0, 0, 0);
    private static final Color BLACK_OVERLAY = new Color(0, 0, 0, 200);
    private static final Color FALLBACK_BG = new Color(50, 50, 60, 255);
    private static final Color BG_BUTTON = new Color(230, 230, 230, 120);
    private static final Color TEXT_CLOSE_HOVER = new Color(255, 55, 55, 255);
    private static final Color TEXT_BG_HOVER = new Color(55, 255, 55, 255);
    private static final Color TEXT_DASH_HOVER = new Color(55, 55, 255, 255);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final int MAX_RECENT_CARDS = 4;
    private static final int MAIN_MENU_COLS = 3;
    private static final int RECENT_COLS = 4;
    private static final int NEWS_COLS = 4;

    // Layout constants
    private static final float TOP_PADDING = 40.0F;
    private static final float BOTTOM_PADDING = 30.0F;
    private static final float TITLE_SPACING_BASE = 40.0F;
    private static final float SECTION_SPACING_BASE = 35.0F;

    // ========================================
    // FIELDS
    // ========================================
    private final CardGrid mainMenuGrid;
    private final CardGrid recentGrid;
    private final CardGrid newsGrid;

    private final MD3Animation sceneAnim;
    private final long startTime;

    private DashboardContextMenu contextMenu;
    private long lastRightClickTime;

    private final List<ResourceLocation> loadedTextures = new ArrayList<>();
    private final ColorCache colorCache = new ColorCache();

    public DashboardScene(GuiGlideMainMenu parent) {
        super(parent);

        this.mainMenuGrid = new CardGrid(MAIN_MENU_COLS, 240, 140);
        this.recentGrid = new CardGrid(RECENT_COLS, 180, 110);
        this.newsGrid = new CardGrid(NEWS_COLS, 180, 110);

        this.sceneAnim = MD3Animation.emphasized();
        this.startTime = System.currentTimeMillis();
        
        GlideLogger.info("[Dashboard] Constructor - Created grids: Main(3x140), Recent(4x110), News(4x110)");
    }

    @Override
    public void initScene() {
        GlideLogger.info("[Dashboard] ========== INIT SCENE START ==========");

        clearTextures();

        buildMainMenuCards();
        buildRecentCards();
        buildNewsCards();

        sceneAnim.setValue(0.0F);
        sceneAnim.animateTo(1.0F);

        int totalCards = mainMenuGrid.getCards().size() + recentGrid.getCards().size() + newsGrid.getCards().size();
        GlideLogger.info("[Dashboard] Total cards created: " + totalCards + " (Main:" + mainMenuGrid.getCards().size() 
            + ", Recent:" + recentGrid.getCards().size() + ", News:" + newsGrid.getCards().size() + ")");
        GlideLogger.info("[Dashboard] ========== INIT SCENE END ==========");
    }

    private void clearTextures() {
        if (mc == null || mc.getTextureManager() == null) return;

        try {
            for (ResourceLocation loc : loadedTextures) {
                if (loc != null) {
                    mc.getTextureManager().deleteTexture(loc);
                }
            }
            loadedTextures.clear();
            GlideLogger.info("[Dashboard] Textures cleared");
        } catch (Exception e) {
            GlideLogger.error("[Dashboard] Clear textures failed", e);
        }
    }

    private void buildMainMenuCards() {
        if (mainMenuGrid == null) return;

        mainMenuGrid.clear();

        ResourceLocation spImg = loadImage("mainmenu/cards/singleplayer");
        mainMenuGrid.addCard(new Card("Singleplayer", "Start a new world", spImg, CardAction.SINGLEPLAYER));

        ResourceLocation mpImg = loadImage("mainmenu/cards/multiplayer");
        mainMenuGrid.addCard(new Card("Multiplayer", "Join a server", mpImg, CardAction.MULTIPLAYER));

        ResourceLocation setImg = loadImage("mainmenu/cards/settings");
        mainMenuGrid.addCard(new Card("Settings", "Modify the game options", setImg, CardAction.SETTINGS));

        GlideLogger.info("[Dashboard] Main menu cards built: " + mainMenuGrid.getCards().size());
    }

    private void buildRecentCards() {
        if (recentGrid == null) return;

        recentGrid.clear();

        loadRecentServers();
        loadRecentWorlds();

        while (recentGrid.getCards().size() < MAX_RECENT_CARDS) {
            recentGrid.addCard(new Card("No Recent Activity", "Play to see recent servers or worlds", (ResourceLocation) null, CardAction.NONE));
        }

        GlideLogger.info("[Dashboard] Recent cards built: " + recentGrid.getCards().size());
    }

    private void loadRecentWorlds() {
        if (mc == null || mc.mcDataDir == null || recentGrid == null) return;

        File savesDir = new File(mc.mcDataDir, "saves");
        if (!savesDir.isDirectory()) return;

        File[] dirs = savesDir.listFiles();
        if (dirs == null || dirs.length == 0) return;

        List<WorldData> worlds = new ArrayList<>();

        for (File dir : dirs) {
            if (dir == null || !dir.isDirectory()) continue;

            File levelDat = new File(dir, "level.dat");
            if (!levelDat.exists()) continue;

            String name = dir.getName();
            long time = levelDat.lastModified();

            if (name != null && !name.isEmpty() && time > 0) {
                worlds.add(new WorldData(name, time));
            }
        }

        if (worlds.isEmpty()) return;

        Collections.sort(worlds, (a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            return Long.compare(b.lastPlayed, a.lastPlayed);
        });

        int maxAdd = MAX_RECENT_CARDS - recentGrid.getCards().size();
        int count = Math.min(maxAdd, worlds.size());

        for (int i = 0; i < count; i++) {
            WorldData world = worlds.get(i);
            if (world == null) continue;

            String dateText = formatDate(world.lastPlayed);

            Glide glide = Glide.getInstance();
            File screenshot = null;

            if (glide != null && glide.getWorldScreenshotManager() != null) {
                screenshot = glide.getWorldScreenshotManager().getScreenshotFile(world.name);
            }

            if (screenshot != null && screenshot.exists()) {
                recentGrid.addCard(new Card(world.name, dateText, screenshot, CardAction.WORLD));
            } else {
                recentGrid.addCard(new Card(world.name, dateText, (ResourceLocation) null, CardAction.WORLD));
            }
        }

        GlideLogger.info("[Dashboard] Loaded " + count + " world(s)");
    }

    private void loadRecentServers() {
        if (mc == null || mc.getCurrentServerData() == null || recentGrid == null) return;

        String ip = mc.getCurrentServerData().serverIP;
        String name = mc.getCurrentServerData().serverName;

        if (ip == null || ip.isEmpty()) return;

        String subtitle = "Currently playing";
        if (name != null && !name.isEmpty() && !name.equals(ip)) {
            subtitle = name;
        }

        ResourceLocation image = loadServerImage(ip);

        if (recentGrid.getCards().size() < MAX_RECENT_CARDS) {
            recentGrid.addCard(0, new Card(ip, subtitle, image, CardAction.SERVER));
            GlideLogger.info("[Dashboard] Added server: " + ip);
        }
    }

    private ResourceLocation loadServerImage(String ip) {
        if (ip == null || ip.isEmpty()) return null;

        String clean = ip.toLowerCase()
                .replace(".", "-")
                .replace(":", "-")
                .replaceAll("[^a-z0-9-]", "");

        ResourceLocation custom = loadImage("mainmenu/cards/servers/" + clean);
        if (custom != null) return custom;

        if (ip.contains("hypixel")) {
            return loadImage("mainmenu/cards/servers/hypixel");
        } else if (ip.contains("mineplex")) {
            return loadImage("mainmenu/cards/servers/mineplex");
        } else if (ip.contains("minemen")) {
            return loadImage("mainmenu/cards/servers/minemen");
        }

        return null;
    }

    private void buildNewsCards() {
        if (newsGrid == null) return;

        newsGrid.clear();

        // ✅ DEBUG: Test với ảnh có sẵn hoặc ảnh mới
        ResourceLocation newsUpdate = loadImage("mainmenu/cards/news/update");
        ResourceLocation newsLgbt = loadImage("mainmenu/cards/news/lgbt");
        ResourceLocation newsSearch = loadImage("mainmenu/cards/news/search");
        ResourceLocation newsDiscord = loadImage("mainmenu/cards/news/discord");

        newsGrid.addCard(new Card("GlideClient 7.3", "Download the latest version", newsUpdate, CardAction.NONE));
        newsGrid.addCard(new Card("We Support LGBT!", "Pride month celebration", newsLgbt, CardAction.NONE));
        newsGrid.addCard(new Card("Type To Search", "Press / to start searching", newsSearch, CardAction.NONE));
        newsGrid.addCard(new Card("Are you on our discord?", "Join the community", newsDiscord, CardAction.NONE));

        GlideLogger.info("[Dashboard] News cards built: " + newsGrid.getCards().size());
    }

    private ResourceLocation loadImage(String basePath) {
        if (basePath == null || basePath.isEmpty()) {
            GlideLogger.warn("[Dashboard] loadImage() - basePath is null/empty");
            return null;
        }

        if (mc == null || mc.getResourceManager() == null) {
            GlideLogger.error("[Dashboard] loadImage() - ResourceManager not available");
            return null;
        }

        String fullPath = basePath + ".png";

        try {
            ResourceLocation loc = new ResourceLocation("minecraft", "soar/" + fullPath);

            if (mc.getResourceManager().getResource(loc) != null) {
                loadedTextures.add(loc);
                GlideLogger.info("[Dashboard] ✅ Loaded: soar/" + fullPath);
                return loc;
            }
        } catch (Exception e) {
            GlideLogger.warn("[Dashboard] ❌ Not found: soar/" + fullPath);
        }

        return null;
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "Last played recently";

        try {
            LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            return "Last played on " + DATE_FORMAT.format(dt);
        } catch (Exception e) {
            return "Last played recently";
        }
    }

    @Override
    public void initGui() {
        if (mc == null) return;

        GlideLogger.info("[Dashboard] ========== INIT GUI START ==========");
        
        ScaledResolution sr = new ScaledResolution(mc);
        LayoutCalculator calc = new LayoutCalculator(sr);
        calc.calculate(mainMenuGrid, recentGrid, newsGrid);

        GlideLogger.info("[Dashboard] ========== INIT GUI END ==========");
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Glide glide = Glide.getInstance();
        if (glide == null) return;

        NanoVGManager nvg = glide.getNanoVGManager();
        if (nvg == null || mc == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float progress = sceneAnim.getValue();

        nvg.setupAndDraw(() -> {
            drawSection(nvg, sr, "Main Menu", mainMenuGrid, mouseX, mouseY, 0, progress, true);
            drawSection(nvg, sr, "Recent Servers & Worlds", recentGrid, mouseX, mouseY, 1, progress, false);
            drawSection(nvg, sr, "News and Tips", newsGrid, mouseX, mouseY, 2, progress, false);

            if (contextMenu != null) {
                contextMenu.draw(nvg, mouseX, mouseY);
            }

            drawTopRightIcons(nvg, sr, mouseX, mouseY);
        });
    }

    private void drawTopRightIcons(NanoVGManager nvg, ScaledResolution sr, int mouseX, int mouseY) {
        final int BUTTON_SIZE = 22;
        final int BUTTON_MARGIN = 6;

        float screenWidth = sr.getScaledWidth();

        float closeX = screenWidth - BUTTON_SIZE - BUTTON_MARGIN;
        float bgX = closeX - BUTTON_SIZE - BUTTON_MARGIN;
        float dashX = bgX - BUTTON_SIZE - BUTTON_MARGIN;

        GlideLogger.info("[Dashboard] Icons position - Close:" + closeX + ", BG:" + bgX + ", Dash:" + dashX);

        drawHeaderIcon(nvg, dashX, BUTTON_MARGIN, LegacyIcon.GRID, 15, TEXT_DASH_HOVER, mouseX, mouseY);
        drawHeaderIcon(nvg, bgX, BUTTON_MARGIN, LegacyIcon.IMAGE, 15, TEXT_BG_HOVER, mouseX, mouseY);
        drawHeaderIcon(nvg, closeX, BUTTON_MARGIN, LegacyIcon.X, 18, TEXT_CLOSE_HOVER, mouseX, mouseY);
    }

    private void drawHeaderIcon(NanoVGManager nvg, float x, float y, String icon, float size, Color color, int mouseX, int mouseY) {
        final int BUTTON_SIZE = 22;

        // Background
        nvg.drawRoundedRect(x, y, BUTTON_SIZE, BUTTON_SIZE, 4, BG_BUTTON);

        // ✅ FIX: Icon căn giữa chính xác (dùng drawCenteredText của NVG)
        float centerX = x + (BUTTON_SIZE / 2.0F);
        float centerY = y + (BUTTON_SIZE / 2.0F);

        nvg.drawCenteredText(icon, centerX, centerY, color, size, Fonts.LEGACYICON);

        // DEBUG hover
        boolean hover = MouseUtils.isInside(mouseX, mouseY, x, y, BUTTON_SIZE, BUTTON_SIZE);
        if (hover) {
            GlideLogger.info("[Dashboard] Icon hover: " + icon);
        }
    }

    private void drawSection(NanoVGManager nvg, ScaledResolution sr, String title, CardGrid grid, int mx, int my, int idx, float prog, boolean logo) {
        if (nvg == null || sr == null || grid == null || title == null) return;

        List<Card> cards = grid.getCards();
        if (cards == null || cards.isEmpty()) return;

        Card firstCard = cards.get(0);
        if (firstCard == null) return;

        float titleSpacing = TITLE_SPACING_BASE * grid.getScale();
        float titleY = firstCard.y - titleSpacing;

        float screenWidth = sr.getScaledWidth();
        float gridWidth = grid.getTotalWidth();
        float titleX = (screenWidth - gridWidth) / 2;

        Color titleColor = colorCache.getAlpha(WHITE, (int) (255 * prog));
        float titleSize = (logo ? 18 : 16) * grid.getScale();

        if (logo) {
            float logoSize = titleSize;
            nvg.drawText(LegacyIcon.SOAR, titleX - logoSize - 8, titleY, titleColor, logoSize, Fonts.LEGACYICON);
        }

        nvg.drawText(title, titleX, titleY, titleColor, titleSize, Fonts.SEMIBOLD);

        GlideLogger.info("[Dashboard] Section '" + title + "' - Title at Y:" + titleY + ", Cards start at Y:" + firstCard.y);

        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            if (card == null) continue;

            float cardProg = getCardProgress(idx, i, prog);
            drawCard(nvg, card, mx, my, cardProg, i);
        }
    }

    private float getCardProgress(int section, int cardIdx, float sceneProg) {
        long elapsed = System.currentTimeMillis() - startTime;

        int prevCards = 0;
        if (section == 1) prevCards = MAIN_MENU_COLS;
        if (section == 2) prevCards = MAIN_MENU_COLS + MAX_RECENT_CARDS;

        int delay = (prevCards + cardIdx) * 40;
        long cardElapsed = Math.max(0, elapsed - delay);

        float local = Math.min(1.0F, cardElapsed / 250.0F);
        float eased = easeOut(local);

        return eased * sceneProg;
    }

    private float easeOut(float t) {
        if (t <= 0.0F) return 0.0F;
        if (t >= 1.0F) return 1.0F;

        float v = t - 1.0F;
        return v * v * v + 1.0F;
    }

    private void drawCard(NanoVGManager nvg, Card card, int mx, int my, float prog, int cardIndex) {
        if (nvg == null || card == null || prog < 0.001F) return;
        if (card.hoverAnim == null || card.scaleAnim == null) return;

        boolean hover = MouseUtils.isInside(mx, my, card.x, card.y, card.w, card.h);

        card.hoverAnim.update();
        card.scaleAnim.update();

        if (hover && card.hoverAnim.isDone() && card.hoverAnim.getValue() < 0.5F) {
            card.hoverAnim.animateTo(1.0F);
            card.scaleAnim.animateTo(1.05F);
        } else if (!hover && card.hoverAnim.isDone() && card.hoverAnim.getValue() > 0.5F) {
            card.hoverAnim.animateTo(0.0F);
            card.scaleAnim.animateTo(1.0F);
        }

        float scale = card.scaleAnim.getValue();

        nvg.save();
        nvg.scale(card.x + card.w / 2, card.y + card.h / 2, card.w, card.h, scale * prog);

        nvg.drawShadow(card.x, card.y + 6, card.w, card.h, card.radius);

        boolean drawn = false;

        if (card.imageFile != null && card.imageFile.exists()) {
            nvg.drawRoundedImage(card.imageFile, card.x, card.y, card.w, card.h, card.radius);
            drawn = true;
            GlideLogger.info("[Dashboard] Card #" + cardIndex + " drew File image: " + card.imageFile.getName());
        } else if (card.imageRes != null) {
            nvg.drawRoundedImage(card.imageRes, card.x, card.y, card.w, card.h, card.radius);
            drawn = true;
            GlideLogger.info("[Dashboard] Card #" + cardIndex + " drew ResourceLocation: " + card.imageRes.toString());
        }

        if (!drawn) {
            nvg.drawRoundedRect(card.x, card.y, card.w, card.h, card.radius, FALLBACK_BG);
            GlideLogger.info("[Dashboard] Card #" + cardIndex + " using FALLBACK (no image)");
        }

        float overlayH = card.h * 0.55F;
        float overlayY = card.y + card.h - overlayH;

        Color gs = colorCache.getAlpha(BLACK_TRANS, 0);
        Color ge = colorCache.getAlpha(BLACK_OVERLAY, (int) (200 * prog));

        nvg.drawGradientRoundedRect(card.x, overlayY, card.w, overlayH, card.radius, gs, ge);

        // ✅ FIX: Text positioning - drawText() trong NVG tự động offset y += size/2
        float pad = 14 * card.scale;
        float titleSize = 15 * card.scale;
        float subtitleSize = 11 * card.scale;
        
        // Title ở vị trí cao hơn một chút
        float titleY = card.y + card.h - (40 * card.scale);
        // Subtitle cách title một khoảng
        float subtitleY = titleY + (20 * card.scale);

        Color tc = colorCache.getAlpha(WHITE, (int) (255 * prog));
        Color sc = colorCache.getAlpha(GRAY_LIGHT, (int) (220 * prog));

        if (card.title != null && !card.title.isEmpty()) {
            nvg.drawText(card.title, card.x + pad, titleY, tc, titleSize, Fonts.SEMIBOLD);
        }

        if (card.subtitle != null && !card.subtitle.isEmpty()) {
            nvg.drawText(card.subtitle, card.x + pad, subtitleY, sc, subtitleSize, Fonts.REGULAR);
        }

        nvg.restore();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        if (contextMenu != null) {
            if (contextMenu.handleClick(mouseX, mouseY)) {
                contextMenu = null;
                return;
            }

            if (!contextMenu.isInside(mouseX, mouseY)) {
                contextMenu = null;
                return;
            }
        }

        if (handleCardClick(mainMenuGrid, mouseX, mouseY)) return;
        if (handleCardClick(recentGrid, mouseX, mouseY)) return;
        if (handleCardClick(newsGrid, mouseX, mouseY)) return;
    }

    private boolean handleCardClick(CardGrid grid, int mx, int my) {
        if (grid == null || mc == null) return false;

        List<Card> cards = grid.getCards();
        if (cards == null) return false;

        for (Card card : cards) {
            if (card == null) continue;

            if (MouseUtils.isInside(mx, my, card.x, card.y, card.w, card.h)) {
                GlideLogger.info("[Dashboard] Card clicked: " + card.title);
                executeAction(card.action);
                return true;
            }
        }

        return false;
    }

    private void executeAction(CardAction action) {
        if (action == null || mc == null) return;

        switch (action) {
            case SINGLEPLAYER:
                mc.displayGuiScreen(new GuiSelectWorld(getParent()));
                break;
            case MULTIPLAYER:
                mc.displayGuiScreen(new GuiMultiplayer(getParent()));
                break;
            case SETTINGS:
                mc.displayGuiScreen(new GuiOptions(getParent(), mc.gameSettings));
                break;
            case WORLD:
                mc.displayGuiScreen(new GuiSelectWorld(getParent()));
                break;
            case SERVER:
                mc.displayGuiScreen(new GuiMultiplayer(getParent()));
                break;
            case NONE:
            default:
                break;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 1) return;

        if (contextMenu != null) {
            contextMenu = null;
            return;
        }

        if (recentGrid == null) return;

        List<Card> cards = recentGrid.getCards();
        if (cards == null) return;

        for (Card card : cards) {
            if (card == null || card.action != CardAction.WORLD) continue;

            if (MouseUtils.isInside(mouseX, mouseY, card.x, card.y, card.w, card.h)) {
                openContextMenu(mouseX, mouseY, card);
                return;
            }
        }
    }

    private void openContextMenu(int mx, int my, Card card) {
        if (card == null || card.title == null) return;

        long now = System.currentTimeMillis();
        if (now - lastRightClickTime < 300) return;
        lastRightClickTime = now;

        Glide glide = Glide.getInstance();
        if (glide == null || glide.getWorldScreenshotManager() == null) return;

        boolean hasCustom = glide.getWorldScreenshotManager().hasCustomScreenshot(card.title);

        contextMenu = new DashboardContextMenu(mx, my, card.title, hasCustom, () -> {
            contextMenu = null;
            buildRecentCards();
            initGui();
        });
    }

    @Override
    public void onSceneClosed() {
        if (mainMenuGrid != null) mainMenuGrid.cleanup();
        if (recentGrid != null) recentGrid.cleanup();
        if (newsGrid != null) newsGrid.cleanup();

        contextMenu = null;

        clearTextures();
        colorCache.clear();

        GlideLogger.info("[Dashboard] Closed and cleaned up");
    }

    // ========================================
    // INNER CLASSES
    // ========================================

    private static class ColorCache {
        private final java.util.HashMap<String, Color> cache = new java.util.HashMap<>();

        Color getAlpha(Color base, int alpha) {
            if (base == null) return new Color(255, 255, 255, Math.max(0, Math.min(255, alpha)));

            String key = base.getRGB() + "_" + alpha;

            if (cache.containsKey(key)) {
                return cache.get(key);
            }

            alpha = Math.max(0, Math.min(255, alpha));
            Color result = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);

            if (cache.size() < 100) {
                cache.put(key, result);
            }

            return result;
        }

        void clear() {
            cache.clear();
        }
    }

    private static class CardGrid {
        private final List<Card> cards;
        private final int cols;
        private final float baseW;
        private final float baseH;
        private float scale = 1.0F;

        CardGrid(int cols, float baseW, float baseH) {
            this.cards = new ArrayList<>();
            this.cols = cols;
            this.baseW = baseW;
            this.baseH = baseH;
        }

        void addCard(Card card) {
            if (card != null) cards.add(card);
        }

        void addCard(int idx, Card card) {
            if (card != null && idx >= 0 && idx <= cards.size()) {
                cards.add(idx, card);
            }
        }

        void clear() {
            cards.clear();
        }

        void cleanup() {
            for (Card card : cards) {
                if (card != null) card.cleanup();
            }
            cards.clear();
        }

        List<Card> getCards() {
            return cards;
        }

        float getTotalWidth() {
            float cardWidth = baseW * scale;
            float gap = 10 * scale;
            return (cardWidth * cols) + (gap * (cols - 1));
        }

        float getScale() {
            return scale;
        }

        void setScale(float s) {
            this.scale = s;
        }

        int getCols() {
            return cols;
        }

        float getBaseW() {
            return baseW;
        }

        float getBaseH() {
            return baseH;
        }
    }

    private static class Card {
        String title;
        String subtitle;
        ResourceLocation imageRes;
        File imageFile;
        CardAction action;

        float x, y, w, h, radius, scale;

        final MD3Animation hoverAnim = MD3Animation.standard();
        final MD3Animation scaleAnim = MD3Animation.standard();

        Card(String title, String subtitle, ResourceLocation image, CardAction action) {
            this.title = title;
            this.subtitle = subtitle;
            this.imageRes = image;
            this.imageFile = null;
            this.action = action;
            this.scaleAnim.setValue(1.0F);
        }

        Card(String title, String subtitle, File image, CardAction action) {
            this.title = title;
            this.subtitle = subtitle;
            this.imageRes = null;
            this.imageFile = image;
            this.action = action;
            this.scaleAnim.setValue(1.0F);
        }

        void cleanup() {
            title = null;
            subtitle = null;
            imageRes = null;
            imageFile = null;
        }
    }

    private enum CardAction {
        SINGLEPLAYER, MULTIPLAYER, SETTINGS, WORLD, SERVER, NONE
    }

    private static class WorldData {
        final String name;
        final long lastPlayed;

        WorldData(String name, long lastPlayed) {
            this.name = name;
            this.lastPlayed = lastPlayed;
        }
    }

    // ========================================
    // ADAPTIVE LAYOUT CALCULATOR
    // ========================================
    private static class LayoutCalculator {
        private final ScaledResolution sr;
        private float scale;

        LayoutCalculator(ScaledResolution sr) {
            this.sr = sr;
        }

        void calculate(CardGrid main, CardGrid recent, CardGrid news) {
            if (sr == null || main == null || recent == null || news == null) return;

            GlideLogger.info("[Layout] ========== LAYOUT CALCULATION START ==========");
            GlideLogger.info("[Layout] Screen: " + sr.getScaledWidth() + "x" + sr.getScaledHeight());

            float screenWidth = sr.getScaledWidth();
            float screenHeight = sr.getScaledHeight();

            // ✅ Calculate scale to fit width
            float horizontalPadding = screenWidth * 0.08F;
            float availableWidth = screenWidth - (horizontalPadding * 2);
            float mainGridBaseWidth = (main.getBaseW() * main.getCols()) + (10 * (main.getCols() - 1));
            
            float scaleByWidth = availableWidth / mainGridBaseWidth;
            GlideLogger.info("[Layout] Scale by width: " + scaleByWidth);

            // ✅ Calculate total height with this scale
            float testScale = scaleByWidth;
            float titleSpacing = TITLE_SPACING_BASE * testScale;
            float sectionSpacing = SECTION_SPACING_BASE * testScale;
            
            float mainH = main.getBaseH() * testScale;
            float recentH = recent.getBaseH() * testScale;
            float newsH = news.getBaseH() * testScale;

            float totalHeight = TOP_PADDING 
                + titleSpacing + mainH
                + sectionSpacing + titleSpacing + recentH
                + sectionSpacing + titleSpacing + newsH
                + BOTTOM_PADDING;

            GlideLogger.info("[Layout] Total height at scale " + testScale + ": " + totalHeight + " (available: " + screenHeight + ")");

            // ✅ If too tall, reduce scale
            if (totalHeight > screenHeight) {
                float scaleByHeight = (screenHeight - TOP_PADDING - BOTTOM_PADDING) / 
                    (titleSpacing * 3 + mainH + recentH + newsH + sectionSpacing * 2);
                
                this.scale = Math.min(scaleByWidth, scaleByHeight) * 0.95F; // 95% để có margin
                GlideLogger.info("[Layout] Content too tall! Adjusted scale to: " + this.scale);
            } else {
                this.scale = scaleByWidth;
                GlideLogger.info("[Layout] Content fits! Using scale: " + this.scale);
            }

            // Apply scale
            main.setScale(this.scale);
            recent.setScale(this.scale);
            news.setScale(this.scale);

            // Recalculate dimensions
            titleSpacing = TITLE_SPACING_BASE * this.scale;
            sectionSpacing = SECTION_SPACING_BASE * this.scale;

            float mainGridHeight = main.getBaseH() * this.scale;
            float recentGridHeight = recent.getBaseH() * this.scale;
            float newsGridHeight = news.getBaseH() * this.scale;

            float mainSectionHeight = titleSpacing + mainGridHeight;
            float recentSectionHeight = titleSpacing + recentGridHeight;
            float newsSectionHeight = titleSpacing + newsGridHeight;

            totalHeight = TOP_PADDING 
                + mainSectionHeight
                + sectionSpacing + recentSectionHeight
                + sectionSpacing + newsSectionHeight
                + BOTTOM_PADDING;

            GlideLogger.info("[Layout] Final total height: " + totalHeight);

            // ✅ Center vertically
            float startY = (screenHeight - totalHeight) / 2 + TOP_PADDING;
            startY = Math.max(startY, TOP_PADDING);

            GlideLogger.info("[Layout] Start Y: " + startY);

            float currentY = startY;

            // Main Menu section
            currentY += titleSpacing;
            positionGrid(main, screenWidth, currentY, "Main");
            currentY += mainGridHeight;

            // Recent section
            currentY += sectionSpacing;
            currentY += titleSpacing;
            positionGrid(recent, screenWidth, currentY, "Recent");
            currentY += recentGridHeight;

            // News section
            currentY += sectionSpacing;
            currentY += titleSpacing;
            positionGrid(news, screenWidth, currentY, "News");

            GlideLogger.info("[Layout] ========== LAYOUT CALCULATION END ==========");
        }

        private void positionGrid(CardGrid grid, float screenWidth, float y, String name) {
            if (grid == null) return;

            float scale = grid.getScale();
            float cardWidth = grid.getBaseW() * scale;
            float cardHeight = grid.getBaseH() * scale;
            float gap = 10 * scale;
            int cols = grid.getCols();

            float totalWidth = (cardWidth * cols) + (gap * (cols - 1));
            float startX = (screenWidth - totalWidth) / 2;

            GlideLogger.info("[Layout] " + name + " Grid: startX=" + startX + ", Y=" + y + ", cardW=" + cardWidth + ", cardH=" + cardHeight);

            List<Card> cards = grid.getCards();
            if (cards == null) return;

            for (int i = 0; i < cards.size(); i++) {
                Card card = cards.get(i);
                if (card == null) continue;

                int col = i % cols;
                int row = i / cols;

                card.x = startX + (col * (cardWidth + gap));
                card.y = y + (row * (cardHeight + gap));
                card.w = cardWidth;
                card.h = cardHeight;
                card.radius = 10 * scale;
                card.scale = scale;

                GlideLogger.info("[Layout]   Card[" + i + "]: X=" + card.x + ", Y=" + card.y + ", W=" + card.w + ", H=" + card.h);
            }
        }

        float getScale() {
            return scale;
        }
    }
}