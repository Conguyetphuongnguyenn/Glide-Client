package me.eldodebug.soar.management.color.palette;

import java.awt.Color;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.color.Theme;
import me.eldodebug.soar.utils.animation.ColorAnimation;

public class ColorPalette {

	private static final int MD3_COLOR_COUNT = 10;
	
	private final ColorAnimation[] backgroundColorAnimations = new ColorAnimation[MD3_COLOR_COUNT];
	private final ColorAnimation[] fontColorAnimations = new ColorAnimation[MD3_COLOR_COUNT];
	
	public ColorPalette() {
		
		for (int i = 0; i < backgroundColorAnimations.length; i++) {
			backgroundColorAnimations[i] = new ColorAnimation();
		}
		
		for (int i = 0; i < fontColorAnimations.length; i++) {
			fontColorAnimations[i] = new ColorAnimation();
		}
	}
	
	public Color getBackgroundColor(ColorType type, int alpha) {
		if (type == null) return new Color(0, 0, 0, alpha);
		return backgroundColorAnimations[type.getIndex()].getColor(getRawBackgroundColor(type, alpha));
	}
	
	public Color getBackgroundColor(ColorType type) {
		return getBackgroundColor(type, 255);
	}
	
	private Color getRawBackgroundColor(ColorType type, int alpha) {
		
		Theme theme = getTheme();
		
		switch (type) {
			case DARK:
				return theme.getDarkBackgroundColor(alpha);
			case NORMAL:
				return theme.getNormalBackgroundColor(alpha);
			case BACKGROUND:
				return theme.getMD3Background(alpha);
			case SURFACE:
				return theme.getMD3Surface(alpha);
			case SURFACE_VARIANT:
				return theme.getMD3SurfaceVariant(alpha);
			case PRIMARY:
				return theme.getMD3Primary(alpha);
			default:
				return new Color(255, 0, 0, alpha);
		}
	}
	
	public Color getFontColor(ColorType type, int alpha) {
		if (type == null) return new Color(255, 255, 255, alpha);
		return fontColorAnimations[type.getIndex()].getColor(getRawFontColor(type, alpha));
	}
	
	public Color getFontColor(ColorType type) {
		return getFontColor(type, 255);
	}
	
	private Color getRawFontColor(ColorType type, int alpha) {
		
		Theme theme = getTheme();
		
		switch (type) {
			case DARK:
				return theme.getDarkFontColor(alpha);
			case NORMAL:
				return theme.getNormalFontColor(alpha);
			case ON_BACKGROUND:
				return theme.getMD3OnBackground(alpha);
			case ON_SURFACE:
				return theme.getMD3OnSurface(alpha);
			case OUTLINE:
				return theme.getMD3Outline(alpha);
			case ON_PRIMARY:
				return theme.getMD3OnPrimary(alpha);
			default:
				return new Color(255, 0, 0, alpha);
		}
	}
	
	private Theme getTheme() {
		
		Glide instance = Glide.getInstance();
		
		if (instance == null || instance.getColorManager() == null) {
			return Theme.LIGHT;
		}
		
		return instance.getColorManager().getTheme();
	}
	
	public Color getMD3Background(int alpha) {
		return getBackgroundColor(ColorType.BACKGROUND, alpha);
	}
	
	public Color getMD3Background() {
		return getMD3Background(255);
	}
	
	public Color getMD3OnBackground(int alpha) {
		return getFontColor(ColorType.ON_BACKGROUND, alpha);
	}
	
	public Color getMD3OnBackground() {
		return getMD3OnBackground(255);
	}
	
	public Color getMD3Surface(int alpha) {
		return getBackgroundColor(ColorType.SURFACE, alpha);
	}
	
	public Color getMD3Surface() {
		return getMD3Surface(255);
	}
	
	public Color getMD3OnSurface(int alpha) {
		return getFontColor(ColorType.ON_SURFACE, alpha);
	}
	
	public Color getMD3OnSurface() {
		return getMD3OnSurface(255);
	}
	
	public Color getMD3SurfaceVariant(int alpha) {
		return getBackgroundColor(ColorType.SURFACE_VARIANT, alpha);
	}
	
	public Color getMD3SurfaceVariant() {
		return getMD3SurfaceVariant(255);
	}
	
	public Color getMD3Outline(int alpha) {
		return getFontColor(ColorType.OUTLINE, alpha);
	}
	
	public Color getMD3Outline() {
		return getMD3Outline(255);
	}
	
	public Color getMD3Primary(int alpha) {
		return getBackgroundColor(ColorType.PRIMARY, alpha);
	}
	
	public Color getMD3Primary() {
		return getMD3Primary(255);
	}
	
	public Color getMD3OnPrimary(int alpha) {
		return getFontColor(ColorType.ON_PRIMARY, alpha);
	}
	
	public Color getMD3OnPrimary() {
		return getMD3OnPrimary(255);
	}
	
	public Color getMaterialRed(int alpha) {
		return new Color(232, 38, 52, alpha);
	}
	
	public Color getMaterialYellow(int alpha) {
		return new Color(255, 255, 0, alpha);
	}
	
	public Color getMaterialRed() {
		return getMaterialRed(255);
	}
	
	public Color getMaterialYellow() {
		return getMaterialYellow(255);
	}
}