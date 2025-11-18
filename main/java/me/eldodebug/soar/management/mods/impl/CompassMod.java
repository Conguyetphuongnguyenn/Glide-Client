package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.SimpleHUDMod;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.utils.buffer.ScreenStencil;
import net.minecraft.util.MathHelper;

public class CompassMod extends SimpleHUDMod {

	private static final String[] CARDINAL_DIRECTIONS = {"S", "W", "N", "E"};
	private static final String[] ORDINAL_DIRECTIONS = {"SW", "NW", "NE", "SE"};
	private static final String[] ANGLE_LABELS_1 = {"15", "105", "195", "285"};
	private static final String[] ANGLE_LABELS_2 = {"30", "120", "210", "300"};
	private static final String[] ANGLE_LABELS_3 = {"60", "150", "240", "300"};
	private static final String[] ANGLE_LABELS_4 = {"70", "165", "255", "345"};
	
	private static final float MAIN_LINE_OFFSET = -2.0F;
	private static final float SECONDARY_LINE_OFFSET_1 = 12.0F;
	private static final float SECONDARY_LINE_OFFSET_2 = 26.0F;
	private static final float SECONDARY_LINE_OFFSET_3 = -16.0F;
	private static final float SECONDARY_LINE_OFFSET_4 = -30.0F;
	
	private final ScreenStencil stencil = new ScreenStencil();
	
	private final ComboSetting designSetting = new ComboSetting(TranslateText.DESIGN, this, TranslateText.SIMPLE, new ArrayList<>(Arrays.asList(
			new Option(TranslateText.SIMPLE), new Option(TranslateText.FANCY))));
	private final BooleanSetting iconSetting = new BooleanSetting(TranslateText.ICON, this, true);
	private final NumberSetting widthSetting = new NumberSetting(TranslateText.WIDTH, this, 180, 50, 450, true);
	
	public CompassMod() {
		super(TranslateText.COMPASS, TranslateText.COMPASS_DESCRIPTION);
	}
	
	@EventTarget
	public void onRender2D(EventRender2D event) {
		
		if (mc.thePlayer == null) return;
		
		Glide instance = Glide.getInstance();
		if (instance == null) return;
		
		NanoVGManager nvg = instance.getNanoVGManager();
		if (nvg == null) return;
		
		Option design = designSetting.getOption();
		if (design == null) return;
		
		if (design.getTranslate().equals(TranslateText.SIMPLE)) {
			this.draw();
		} else {
			nvg.setupAndDraw(() -> {
				this.drawBackground(widthSetting.getValueInt(), 29);
			});
			stencil.wrap(() -> drawNanoVG(), this.getX(), this.getY(), this.getWidth(), this.getHeight(), 6 * this.getScale());
		}
	}
	
	private void drawNanoVG() {
		
		if (mc.thePlayer == null) return;
		
		int baseAngle = (int) MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) * -1 - 360;
		int width = widthSetting.getValueInt();
		
		this.renderMarker(this.getX() + ((width / 2) * this.getScale()), this.getY() + (2.5F * this.getScale()), this.getFontColor());
		
		for (int i = 0; i <= 2; i++) {
			drawCardinalDirections(baseAngle, width);
			drawOrdinalDirections(baseAngle, width);
			drawAngleLabels1(baseAngle, width);
			drawAngleLabels2(baseAngle, width);
			drawAngleLabels3(baseAngle, width);
			drawAngleLabels4(baseAngle, width);
		}
		
		this.setWidth(width);
		this.setHeight(29);
	}
	
	private void drawCardinalDirections(int startAngle, int width) {
		
		int angle = startAngle;
		
		for (int i = 0; i < 4; i++) {
			
			this.drawRect((width / 2) + angle + MAIN_LINE_OFFSET, 8, 0.8F, 9);
			this.drawRect((width / 2) + angle + SECONDARY_LINE_OFFSET_1, 8, 0.8F, 6);
			this.drawRect((width / 2) + angle + SECONDARY_LINE_OFFSET_2, 8, 0.8F, 6);
			this.drawRect((width / 2) + angle + SECONDARY_LINE_OFFSET_3, 8, 0.8F, 6);
			this.drawRect((width / 2) + angle + SECONDARY_LINE_OFFSET_4, 8, 0.8F, 6);
			this.drawCenteredText(CARDINAL_DIRECTIONS[i], (width / 2) + angle - 1.5F, 19, 8.5F, getHudFont(2));
			
			angle += 90;
		}
	}
	
	private void drawOrdinalDirections(int startAngle, int width) {
		
		int angle = startAngle;
		
		for (int i = 0; i < 4; i++) {
			this.drawCenteredText(ORDINAL_DIRECTIONS[i], (width / 2) + angle + 43F, 8.5F, 6.8F, getHudFont(1));
			angle += 90;
		}
	}
	
	private void drawAngleLabels1(int startAngle, int width) {
		
		int angle = startAngle;
		
		for (int i = 0; i < 4; i++) {
			this.drawCenteredText(ANGLE_LABELS_1[i], (width / 2) + angle + 13F, 17, 5.4F, getHudFont(1));
			angle += 90;
		}
	}
	
	private void drawAngleLabels2(int startAngle, int width) {
		
		int angle = startAngle;
		
		for (int i = 0; i < 4; i++) {
			this.drawCenteredText(ANGLE_LABELS_2[i], (width / 2) + angle + 27F, 17, 5.4F, getHudFont(1));
			angle += 90;
		}
	}
	
	private void drawAngleLabels3(int startAngle, int width) {
		
		int angle = startAngle;
		
		for (int i = 0; i < 4; i++) {
			this.drawCenteredText(ANGLE_LABELS_3[i], (width / 2) + angle + 60.5F, 17, 5.4F, getHudFont(1));
			angle += 90;
		}
	}
	
	private void drawAngleLabels4(int startAngle, int width) {
		
		int angle = startAngle;
		
		for (int i = 0; i < 4; i++) {
			this.drawCenteredText(ANGLE_LABELS_4[i], (width / 2) + angle + 74.5F, 17, 5.4F, getHudFont(1));
			angle += 90;
		}
	}
	
	private void renderMarker(float x, float y, Color color) {
		
		Glide instance = Glide.getInstance();
		if (instance == null || color == null) return;
		
		NanoVGManager nvg = instance.getNanoVGManager();
		if (nvg == null) return;
		
		long vg = nvg.getContext();
		NVGColor nvgColor = nvg.getColor(color);
		float scale = this.getScale();
		
		NanoVG.nvgBeginPath(vg);
		NanoVG.nvgMoveTo(vg, x, y + (4 * scale));
		NanoVG.nvgLineTo(vg, x + (4 * scale), y);
		NanoVG.nvgLineTo(vg, x - (4 * scale), y);
		NanoVG.nvgClosePath(vg);
		
		NanoVG.nvgFillColor(vg, nvgColor);
		NanoVG.nvgFill(vg);
	}
	
	@Override
	public String getText() {
		
		if (mc.thePlayer == null) {
			return "Direction: Error";
		}
		
		double rotation = (mc.thePlayer.rotationYawHead - 90) % 360;
		
		if (rotation < 0) {
			rotation += 360.0;
		}
		
		return "Direction: " + getDirectionFromRotation(rotation);
	}
	
	private String getDirectionFromRotation(double rotation) {
		
		if (rotation < 22.5) {
			return "W";
		} else if (rotation < 67.5) {
			return "NW";
		} else if (rotation < 112.5) {
			return "N";
		} else if (rotation < 157.5) {
			return "NE";
		} else if (rotation < 202.5) {
			return "E";
		} else if (rotation < 247.5) {
			return "SE";
		} else if (rotation < 292.5) {
			return "S";
		} else if (rotation < 337.5) {
			return "SW";
		} else {
			return "W";
		}
	}
	
	@Override
	public String getIcon() {
		return iconSetting.isToggled() ? LegacyIcon.COMPASS : null;
	}
}