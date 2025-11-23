package me.eldodebug.soar.management.color;

import java.awt.Color;

import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;

public enum Theme {
	DARK(0, "Dark", 
		new Color(19, 19, 20), new Color(34, 35, 39), new Color(255, 255, 255), new Color(235, 235, 235),
		new Color(28, 27, 31), new Color(230, 225, 229), new Color(49, 48, 51), new Color(230, 225, 229),
		new Color(73, 69, 79), new Color(147, 143, 153), new Color(208, 188, 255), new Color(56, 30, 114)),
		
	LIGHT(1, "Light", 
		new Color(254, 254, 254), new Color(238, 238, 238), new Color(54, 54, 54), new Color(107, 117, 129),
		new Color(255, 251, 254), new Color(28, 27, 31), new Color(255, 251, 254), new Color(28, 27, 31),
		new Color(231, 224, 236), new Color(121, 116, 126), new Color(103, 80, 164), new Color(255, 255, 255)),
		
	DARK_BLUE(2, "Dark Blue", 
		new Color(22, 28, 41), new Color(27, 36, 52), new Color(157, 175, 211), new Color(116, 131, 164),
		new Color(22, 28, 41), new Color(157, 175, 211), new Color(27, 36, 52), new Color(157, 175, 211),
		new Color(38, 49, 71), new Color(116, 131, 164), new Color(100, 181, 246), new Color(13, 71, 161)),
		
	MIDNIGHT(3, "Midnight", 
		new Color(47, 54, 61), new Color(36, 41, 46), new Color(255, 255, 255), new Color(235, 235, 235),
		new Color(47, 54, 61), new Color(255, 255, 255), new Color(36, 41, 46), new Color(255, 255, 255),
		new Color(69, 79, 89), new Color(176, 190, 197), new Color(100, 221, 23), new Color(27, 94, 32)),
		
	DARK_PURPLE(4, "Dark Purple", 
		new Color(44, 14, 72), new Color(53, 24, 90), new Color(234, 226, 252), new Color(194, 186, 212),
		new Color(44, 14, 72), new Color(234, 226, 252), new Color(53, 24, 90), new Color(234, 226, 252),
		new Color(74, 44, 107), new Color(194, 186, 212), new Color(186, 104, 200), new Color(74, 20, 140)),
		
	SEA(5, "Sea", 
		new Color(203, 224, 255), new Color(190, 216, 238), new Color(32, 32, 32), new Color(106, 106, 106),
		new Color(203, 224, 255), new Color(32, 32, 32), new Color(190, 216, 238), new Color(32, 32, 32),
		new Color(224, 242, 254), new Color(84, 110, 122), new Color(33, 150, 243), new Color(255, 255, 255)),
		
	SAKURA(6, "Sakura", 
		new Color(255, 191, 178), new Color(255, 223, 226), new Color(35, 35, 35), new Color(80, 80, 80),
		new Color(255, 191, 178), new Color(35, 35, 35), new Color(255, 223, 226), new Color(35, 35, 35),
		new Color(255, 235, 238), new Color(121, 85, 72), new Color(236, 64, 122), new Color(255, 255, 255)),
		
	CATPPUCCIN_MOCHA(7, "Catppuccin Mocha", 
		new Color(49, 50, 68), new Color(30, 30, 46), new Color(205, 214, 244), new Color(245, 194, 231),
		new Color(30, 30, 46), new Color(205, 214, 244), new Color(49, 50, 68), new Color(205, 214, 244),
		new Color(88, 91, 112), new Color(166, 173, 200), new Color(245, 194, 231), new Color(17, 17, 27)),
		
	CATPPUCCIN_LATTE(8, "Catppuccin Latte", 
		new Color(230, 233, 239), new Color(239, 241, 245), new Color(76, 79, 105), new Color(140, 143, 161),
		new Color(239, 241, 245), new Color(76, 79, 105), new Color(230, 233, 239), new Color(76, 79, 105),
		new Color(204, 208, 218), new Color(140, 143, 161), new Color(136, 57, 239), new Color(255, 255, 255)),
		
	BIRD(9, "Twoot twoot", 
		new Color(25, 40, 52), new Color(20, 32, 43), new Color(255, 255, 255), new Color(136, 153, 171),
		new Color(25, 40, 52), new Color(255, 255, 255), new Color(20, 32, 43), new Color(255, 255, 255),
		new Color(38, 60, 77), new Color(136, 153, 171), new Color(3, 169, 244), new Color(1, 87, 155)),
		
	CALIFORNIA(10, "California", 
		new Color(22, 22, 25), new Color(0, 0, 0), new Color(230, 230, 230), new Color(130, 130, 130),
		new Color(22, 22, 25), new Color(230, 230, 230), new Color(0, 0, 0), new Color(230, 230, 230),
		new Color(33, 33, 38), new Color(130, 130, 130), new Color(255, 193, 7), new Color(0, 0, 0)),
		
	LAVENDER(11, "Lavender", 
		new Color(228, 229, 241), new Color(250, 250, 250), new Color(72, 75, 105), new Color(147, 148, 165),
		new Color(228, 229, 241), new Color(72, 75, 105), new Color(250, 250, 250), new Color(72, 75, 105),
		new Color(204, 206, 222), new Color(147, 148, 165), new Color(136, 57, 239), new Color(255, 255, 255)),
		
	CAMELLIA(12, "Camellia", 
		new Color(30, 31, 36), new Color(23, 24, 28), new Color(228, 229, 231), new Color(250, 56, 103),
		new Color(30, 31, 36), new Color(228, 229, 231), new Color(23, 24, 28), new Color(228, 229, 231),
		new Color(45, 46, 54), new Color(176, 180, 185), new Color(250, 56, 103), new Color(255, 255, 255)),
		
	TERMINAL(13, "Terminal", 
		new Color(7, 7, 7), new Color(12, 12, 12), new Color(33, 96, 7), new Color(54, 73, 0),
		new Color(7, 7, 7), new Color(33, 96, 7), new Color(12, 12, 12), new Color(33, 96, 7),
		new Color(18, 18, 18), new Color(54, 73, 0), new Color(76, 175, 80), new Color(0, 0, 0)),
		
	NORD(14, "Nord", 
		new Color(59, 66, 82), new Color(46, 52, 64), new Color(236, 239, 244), new Color(216, 222, 233),
		new Color(59, 66, 82), new Color(236, 239, 244), new Color(46, 52, 64), new Color(236, 239, 244),
		new Color(76, 86, 106), new Color(216, 222, 233), new Color(136, 192, 208), new Color(46, 52, 64)),
		
	GRUVBOX(15, "Gruvbox Dark Med", 
		new Color(0x3C3836), new Color(0x282828), new Color(0xEBDBB2), new Color(0xA89984),
		new Color(0x282828), new Color(0xEBDBB2), new Color(0x3C3836), new Color(0xEBDBB2),
		new Color(0x504945), new Color(0xA89984), new Color(0xFE8019), new Color(0x282828));

	private final SimpleAnimation animation = new SimpleAnimation();
	
	private final String name;
	private final int id;
	
	private final Color darkBackgroundColor;
	private final Color normalBackgroundColor;
	private final Color darkFontColor;
	private final Color normalFontColor;
	
	private final Color md3Background;
	private final Color md3OnBackground;
	private final Color md3Surface;
	private final Color md3OnSurface;
	private final Color md3SurfaceVariant;
	private final Color md3Outline;
	private final Color md3Primary;
	private final Color md3OnPrimary;
	
	private Theme(int id, String name, 
			Color darkBackgroundColor, Color normalBackgroundColor, 
			Color darkFontColor, Color normalFontColor,
			Color md3Background, Color md3OnBackground,
			Color md3Surface, Color md3OnSurface,
			Color md3SurfaceVariant, Color md3Outline,
			Color md3Primary, Color md3OnPrimary) {
		
		this.name = name;
		this.id = id;
		this.darkBackgroundColor = darkBackgroundColor;
		this.darkFontColor = darkFontColor;
		this.normalBackgroundColor = normalBackgroundColor;
		this.normalFontColor = normalFontColor;
		
		this.md3Background = md3Background;
		this.md3OnBackground = md3OnBackground;
		this.md3Surface = md3Surface;
		this.md3OnSurface = md3OnSurface;
		this.md3SurfaceVariant = md3SurfaceVariant;
		this.md3Outline = md3Outline;
		this.md3Primary = md3Primary;
		this.md3OnPrimary = md3OnPrimary;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}
	
	public Color getDarkBackgroundColor() {
		return darkBackgroundColor;
	}

	public Color getNormalBackgroundColor() {
		return normalBackgroundColor;
	}

	public Color getDarkFontColor() {
		return darkFontColor;
	}

	public Color getNormalFontColor() {
		return normalFontColor;
	}

	public Color getDarkBackgroundColor(int alpha) {
		return ColorUtils.applyAlpha(darkBackgroundColor, alpha);
	}

	public Color getNormalBackgroundColor(int alpha) {
		return ColorUtils.applyAlpha(normalBackgroundColor, alpha);
	}

	public Color getDarkFontColor(int alpha) {
		return ColorUtils.applyAlpha(darkFontColor, alpha);
	}

	public Color getNormalFontColor(int alpha) {
		return ColorUtils.applyAlpha(normalFontColor, alpha);
	}
	
	public Color getMD3Background() {
		return md3Background;
	}
	
	public Color getMD3OnBackground() {
		return md3OnBackground;
	}
	
	public Color getMD3Surface() {
		return md3Surface;
	}
	
	public Color getMD3OnSurface() {
		return md3OnSurface;
	}
	
	public Color getMD3SurfaceVariant() {
		return md3SurfaceVariant;
	}
	
	public Color getMD3Outline() {
		return md3Outline;
	}
	
	public Color getMD3Primary() {
		return md3Primary;
	}
	
	public Color getMD3OnPrimary() {
		return md3OnPrimary;
	}
	
	public Color getMD3Background(int alpha) {
		return ColorUtils.applyAlpha(md3Background, alpha);
	}
	
	public Color getMD3OnBackground(int alpha) {
		return ColorUtils.applyAlpha(md3OnBackground, alpha);
	}
	
	public Color getMD3Surface(int alpha) {
		return ColorUtils.applyAlpha(md3Surface, alpha);
	}
	
	public Color getMD3OnSurface(int alpha) {
		return ColorUtils.applyAlpha(md3OnSurface, alpha);
	}
	
	public Color getMD3SurfaceVariant(int alpha) {
		return ColorUtils.applyAlpha(md3SurfaceVariant, alpha);
	}
	
	public Color getMD3Outline(int alpha) {
		return ColorUtils.applyAlpha(md3Outline, alpha);
	}
	
	public Color getMD3Primary(int alpha) {
		return ColorUtils.applyAlpha(md3Primary, alpha);
	}
	
	public Color getMD3OnPrimary(int alpha) {
		return ColorUtils.applyAlpha(md3OnPrimary, alpha);
	}

	public SimpleAnimation getAnimation() {
		return animation;
	}

	public static Theme getThemeById(int id) {
		
		for (Theme t : Theme.values()) {
			if (t.getId() == id) {
				return t;
			}
		}
		
		return LIGHT;
	}
}