package me.eldodebug.soar.management.mods;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;

public class SimpleHUDMod extends HUDMod {

	private static NanoVGManager cachedNvg;
	
	public SimpleHUDMod(TranslateText nameTranslate, TranslateText descriptionText) {
		super(nameTranslate, descriptionText);
	}

	public SimpleHUDMod(TranslateText nameTranslate, TranslateText descriptionText, String alias) {
		super(nameTranslate, descriptionText, alias);
	}

	public void draw() {
		if(cachedNvg == null) {
			Glide instance = Glide.getInstance();
			if(instance != null) {
				cachedNvg = instance.getNanoVGManager();
			}
		}
		
		if(cachedNvg == null) return;
		
		String text = getText();
		if(text == null) return;
		
		String icon = getIcon();
		boolean hasIcon = icon != null;
		float addX = hasIcon ? this.getTextWidth(icon, 9.5F, Fonts.LEGACYICON) + 4 : 0;
		
		cachedNvg.setupAndDraw(() -> {
			float bgWidth = (this.getTextWidth(text, 9, getHudFont(1)) + 10) + addX;
			
			this.drawBackground(bgWidth, 18);
			this.drawText(text, 5.5F + addX, 5.5F, 9, getHudFont(1));
			
			if(hasIcon) {
				this.drawText(icon, 5.5F, 4F, 10.4F, Fonts.LEGACYICON);
			}
			
			this.setWidth((int) bgWidth);
			this.setHeight(18);
		});
	}
	
	public String getText() {
		return null;
	}
	
	public String getIcon() {
		return null;
	}
}