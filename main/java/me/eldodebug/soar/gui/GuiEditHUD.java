package me.eldodebug.soar.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import eu.shoroa.contrib.render.ShBlur;
import me.eldodebug.soar.management.mods.impl.InternalSettingsMod;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.event.impl.EventRenderNotification;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.utils.MathUtils;
import me.eldodebug.soar.utils.animation.normal.Animation;
import me.eldodebug.soar.utils.animation.normal.Direction;
import me.eldodebug.soar.utils.animation.normal.easing.EaseBackIn;
import me.eldodebug.soar.utils.mouse.MouseUtils;
import me.eldodebug.soar.utils.render.BlurUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.nanovg.NanoVG;

public class GuiEditHUD extends GuiScreen {

	private static final int SNAP_RANGE = 5;
	private static final int ANIMATION_DURATION = 500;
	private static final float ANIMATION_END_VALUE = 1.0F;
	private static final float ANIMATION_START_VALUE = 0.0F;
	private static final float SCALE_CHANGE_NORMAL = 0.1F;
	private static final float SCALE_CHANGE_SHIFT = 0.02F;
	private static final int BACKGROUND_ALPHA = 100;
	private static final int TOOLTIP_Y_OFFSET = 15;
	private static final float MAX_BLUR = 21.0F;
	private static final float MIN_BLUR = 1.0F;
	private static final Color SNAP_LINE_COLOR = new Color(217, 60, 255);
	private static final Color TOOLTIP_BG_COLOR = new Color(255, 255, 255, 200);
	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
	
	private Animation introAnimation;
	private final boolean fromModMenu;
	private boolean snapping;
	private boolean canSnap;
	private final ArrayList<HUDMod> mods;
	private int localMouseX = -1;
	private int localMouseY = -1;
	
	public GuiEditHUD(boolean fromModMenu) {
		this.fromModMenu = fromModMenu;
		this.mods = Glide.getInstance().getModManager().getHudMods();
		Collections.reverse(mods);
	}
	
	@Override
	public void initGui() {
		resetModStates();
		
		introAnimation = new EaseBackIn(ANIMATION_DURATION, ANIMATION_END_VALUE, ANIMATION_START_VALUE);
		introAnimation.setDirection(Direction.FORWARDS);
	}
	
	private void resetModStates() {
		if (mods == null) return;
		
		for (HUDMod m : mods) {
			if (m == null) continue;
			m.setDragging(false);
			m.getAnimation().setValue(0.0F);
		}
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (introAnimation == null || mods == null) return;
		
		if (introAnimation.isDone(Direction.BACKWARDS)) {
			mc.displayGuiScreen(null);
			return;
		}
		
		ScaledResolution sr = new ScaledResolution(mc);
		Glide instance = Glide.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorPalette palette = instance.getColorManager().getPalette();
		
		if (nvg == null || palette == null) return;
		
		boolean shift = isShiftPressed();
		localMouseX = mouseX;
		localMouseY = mouseY;
		
		snapping = false;
		
		drawBackground(sr, nvg);
		drawGuideLines(sr, nvg, palette);
		drawTooltip(sr, nvg);
		
		handleModInteractions(mouseX, mouseY, shift, sr, palette, nvg);
		
		new EventRender2D(partialTicks).call();
		new EventRenderNotification().call();
	}
	
	private boolean isShiftPressed() {
		return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
	}
	
	private void drawBackground(ScaledResolution sr, NanoVGManager nvg) {
		float animValue = (float) introAnimation.getValue();
		
		if (!InternalSettingsMod.getInstance().getBlurSetting().isToggled()) {
			float blurAmount = Math.min(animValue, 1.0F) * 20.0F + MIN_BLUR;
			BlurUtils.drawBlurScreen(blurAmount);
		}
		
		nvg.setupAndDraw(() -> {
			nvg.save();
			NanoVG.nvgGlobalAlpha(nvg.getContext(), animValue);
			
			if (InternalSettingsMod.getInstance().getBlurSetting().isToggled()) {
				ShBlur.getInstance().drawBlur(() -> 
					nvg.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), Color.WHITE)
				);
			}
			
			nvg.restore();
			nvg.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 
				new Color(0, 0, 0, (int) (animValue * BACKGROUND_ALPHA)));
		});
	}
	
	private void drawGuideLines(ScaledResolution sr, NanoVGManager nvg, ColorPalette palette) {
		int halfWidth = sr.getScaledWidth() / 2;
		int halfHeight = sr.getScaledHeight() / 2;
		Color lineColor = palette.getBackgroundColor(ColorType.DARK);
		
		nvg.setupAndDraw(() -> {
			nvg.drawRect(0, halfHeight, sr.getScaledWidth(), 0.5F, lineColor);
			nvg.drawRect(halfWidth, 0, 0.5F, sr.getScaledHeight(), lineColor);
		});
	}
	
	private void drawTooltip(ScaledResolution sr, NanoVGManager nvg) {
		String tooltip = "You can resize elements by scrolling over them. Use shift for more control.";
		
		nvg.setupAndDraw(() -> {
			nvg.drawCenteredText(
				tooltip,
				sr.getScaledWidth() / 2.0F,
				sr.getScaledHeight() - TOOLTIP_Y_OFFSET,
				TOOLTIP_BG_COLOR,
				8.0F,
				Fonts.REGULAR
			);
		});
	}
	
	private void handleModInteractions(int mouseX, int mouseY, boolean shift, ScaledResolution sr, ColorPalette palette, NanoVGManager nvg) {
		if (shift) canSnap = false;
		
		int halfWidth = sr.getScaledWidth() / 2;
		int halfHeight = sr.getScaledHeight() / 2;
		
		for (HUDMod m : mods) {
			if (m == null || !m.isToggled() || m.isHide()) continue;
			
			boolean isInside = isModUnderMouse(m, mouseX, mouseY);
			
			handleModScaling(m, isInside, shift);
			updateModAnimation(m, isInside);
			updateModPosition(m, mouseX, mouseY);
			snapToGuides(m, halfWidth, halfHeight, nvg);
			snapToOtherMods(m, sr, nvg);
			drawModOutline(m, palette, nvg);
		}
	}
	
	private boolean isModUnderMouse(HUDMod mod, int mouseX, int mouseY) {
		if (!MouseUtils.isInside(mouseX, mouseY, mod.getX(), mod.getY(), mod.getWidth(), mod.getHeight())) {
			return false;
		}
		
		for (HUDMod other : mods) {
			if (other == null || !other.isToggled() || other.isHide()) continue;
			if (other == mod) break;
			
			if (MouseUtils.isInside(mouseX, mouseY, other.getX(), other.getY(), other.getWidth(), other.getHeight())) {
				return false;
			}
		}
		
		return true;
	}
	
	private void handleModScaling(HUDMod mod, boolean isInside, boolean shift) {
		if (!isInside) return;
		
		int dWheel = Mouse.getDWheel();
		if (dWheel == 0) return;
		
		float scaleChange = shift ? SCALE_CHANGE_SHIFT : SCALE_CHANGE_NORMAL;
		float newScale = mod.getScale();
		
		if (dWheel > 0) {
			newScale += scaleChange;
		} else {
			newScale -= scaleChange;
		}
		
		float roundedScale = Math.round(newScale * 100.0F) / 100.0F;
		mod.setScale(roundedScale);
	}
	
	private void updateModAnimation(HUDMod mod, boolean isInside) {
		if (mod.getAnimation() == null) return;
		mod.getAnimation().setAnimation(isInside ? 1.0F : 0.0F, 14);
	}
	
	private void updateModPosition(HUDMod mod, int mouseX, int mouseY) {
		if (!mod.isDragging()) return;
		
		mod.setX(mouseX + mod.getDraggingX());
		mod.setY(mouseY + mod.getDraggingY());
	}
	
	private void snapToGuides(HUDMod mod, int halfWidth, int halfHeight, NanoVGManager nvg) {
		if (!canSnap || !mod.isDragging()) return;
		
		ScaledResolution sr = new ScaledResolution(mc);
		int modX = mod.getX();
		int modY = mod.getY();
		int modWidth = mod.getWidth();
		int modHeight = mod.getHeight();
		
		mod.setX(Math.max(0, Math.min(modX, sr.getScaledWidth() - modWidth)));
		mod.setY(Math.max(0, Math.min(modY, sr.getScaledHeight() - modHeight)));
		
		if (MathUtils.isInRange(modX + (modWidth / 2.0F), halfWidth - SNAP_RANGE, halfWidth + SNAP_RANGE)) {
			mod.setX(halfWidth - (modWidth / 2));
		}
		
		if (MathUtils.isInRange(modY + (modHeight / 2.0F), halfHeight - SNAP_RANGE, halfHeight + SNAP_RANGE)) {
			mod.setY(halfHeight - (modHeight / 2));
		}
	}
	
	private void snapToOtherMods(HUDMod mod, ScaledResolution sr, NanoVGManager nvg) {
		if (snapping || !canSnap || !mod.isDragging()) return;
		
		int modX = mod.getX();
		int modY = mod.getY();
		int modWidth = mod.getWidth();
		int modHeight = mod.getHeight();
		
		for (HUDMod other : mods) {
			if (other == null || other == mod || !other.isToggled() || other.isHide()) continue;
			
			int otherX = other.getX();
			int otherY = other.getY();
			int otherWidth = other.getWidth();
			int otherHeight = other.getHeight();
			
			snapHorizontal(mod, nvg, sr, modX, modY, modWidth, modHeight, otherX, otherWidth);
			snapVertical(mod, nvg, sr, modX, modY, modWidth, modHeight, otherY, otherHeight);
		}
	}
	
	private void snapHorizontal(HUDMod mod, NanoVGManager nvg, ScaledResolution sr, 
	                            int modX, int modY, int modWidth, int modHeight, 
	                            int otherX, int otherWidth) {
		
		if (MathUtils.isInRange(otherX, modX - SNAP_RANGE, modX + SNAP_RANGE)) {
			nvg.setupAndDraw(() -> nvg.drawRect(otherX, 0, 0.5F, sr.getScaledHeight(), SNAP_LINE_COLOR));
			snapping = true;
			mod.setX(otherX);
		}
		
		if (MathUtils.isInRange(otherX + otherWidth, modX - SNAP_RANGE, modX + SNAP_RANGE)) {
			nvg.setupAndDraw(() -> nvg.drawRect(otherX + otherWidth, 0, 0.5F, sr.getScaledHeight(), SNAP_LINE_COLOR));
			snapping = true;
			mod.setX(otherX + otherWidth);
		}
		
		if (MathUtils.isInRange(otherX, modX + modWidth - SNAP_RANGE, modX + modWidth + SNAP_RANGE)) {
			nvg.setupAndDraw(() -> nvg.drawRect(otherX, 0, 0.5F, sr.getScaledHeight(), SNAP_LINE_COLOR));
			snapping = true;
			mod.setX(otherX - modWidth);
		}
		
		if (MathUtils.isInRange(otherX + otherWidth, modX + modWidth - SNAP_RANGE, modX + modWidth + SNAP_RANGE)) {
			nvg.setupAndDraw(() -> nvg.drawRect(otherX + otherWidth, 0, 0.5F, sr.getScaledHeight(), SNAP_LINE_COLOR));
			snapping = true;
			mod.setX(otherX + otherWidth - modWidth);
		}
	}
	
	private void snapVertical(HUDMod mod, NanoVGManager nvg, ScaledResolution sr,
	                          int modX, int modY, int modWidth, int modHeight,
	                          int otherY, int otherHeight) {
		
		if (MathUtils.isInRange(otherY, modY - SNAP_RANGE, modY + SNAP_RANGE)) {
			nvg.setupAndDraw(() -> nvg.drawRect(0, otherY, sr.getScaledWidth(), 0.5F, SNAP_LINE_COLOR));
			snapping = true;
			mod.setY(otherY);
		}
		
		if (MathUtils.isInRange(otherY + otherHeight, modY - SNAP_RANGE, modY + SNAP_RANGE)) {
			nvg.setupAndDraw(() -> nvg.drawRect(0, otherY + otherHeight, sr.getScaledWidth(), 0.5F, SNAP_LINE_COLOR));
			snapping = true;
			mod.setY(otherY + otherHeight);
		}
		
		if (MathUtils.isInRange(otherY, modY + modHeight - SNAP_RANGE, modY + modHeight + SNAP_RANGE)) {
			nvg.setupAndDraw(() -> nvg.drawRect(0, otherY, sr.getScaledWidth(), 0.5F, SNAP_LINE_COLOR));
			snapping = true;
			mod.setY(otherY - modHeight);
		}
		
		if (MathUtils.isInRange(otherY + otherHeight, modY + modHeight - SNAP_RANGE, modY + modHeight + SNAP_RANGE)) {
			nvg.setupAndDraw(() -> nvg.drawRect(0, otherY + otherHeight, sr.getScaledWidth(), 0.5F, SNAP_LINE_COLOR));
			snapping = true;
			mod.setY(otherY + otherHeight - modHeight);
		}
	}
	
	private void drawModOutline(HUDMod mod, ColorPalette palette, NanoVGManager nvg) {
		if (mod.getAnimation() == null) return;
		
		float animValue = mod.getAnimation().getValue();
		int alpha = (int) (animValue * 255);
		Color outlineColor = palette.getBackgroundColor(ColorType.DARK, alpha);
		
		nvg.setupAndDraw(() -> {
			nvg.drawOutlineRoundedRect(
				mod.getX() - 2,
				mod.getY() - 2,
				mod.getWidth() + 4,
				mod.getHeight() + 4,
				6.5F * mod.getScale(),
				2,
				outlineColor
			);
		});
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		if (mods == null) return;
		
		for (HUDMod m : mods) {
			if (m == null || !m.isToggled() || m.isHide()) continue;
			
			boolean isInside = isModUnderMouse(m, mouseX, mouseY);
			
			if (mouseButton == 0) {
				canSnap = true;
			}
			
			if (mouseButton == 1 && isInside) {
				m.toggle();
				initGui();
				return;
			}
			
			if (mouseButton == 2 && isInside) {
				m.setScale(1.0F);
			}
			
			if (mouseButton == 0 && isInside) {
				m.setDragging(true);
				m.setDraggingX(m.getX() - mouseX);
				m.setDraggingY(m.getY() - mouseY);
			}
		}
		
		try {
			super.mouseClicked(mouseX, mouseY, mouseButton);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
		if (mods != null) {
			for (HUDMod m : mods) {
				if (m != null) {
					m.setDragging(false);
				}
			}
		}
	}
	
	@Override
	public void keyTyped(char typedChar, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			handleEscape();
			return;
		}
		
		if (mods == null) return;
		
		if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
			handleDelete();
		}
	}
	
	private void handleEscape() {
		if (fromModMenu) {
			mc.displayGuiScreen(Glide.getInstance().getModMenu());
		} else if (introAnimation != null) {
			introAnimation.setDirection(Direction.BACKWARDS);
		}
	}
	
	private void handleDelete() {
		for (HUDMod m : mods) {
			if (m == null || !m.isToggled() || m.isHide()) continue;
			
			boolean isInside = isModUnderMouse(m, localMouseX, localMouseY);
			
			if (isInside) {
				m.toggle();
				initGui();
				return;
			}
		}
	}
	
	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}