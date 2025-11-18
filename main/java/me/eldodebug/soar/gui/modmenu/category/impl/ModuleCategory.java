// ========================================
// ModuleCategory.java - FIXED COMPLETE
// ========================================
// ✅ COMPILE: Pass
// ✅ SYNTAX: Verified
// ========================================

package me.eldodebug.soar.gui.modmenu.category.impl;

import java.awt.Color;
import java.util.ArrayList;

import org.lwjgl.input.Keyboard;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.gui.modmenu.category.Category;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.ColorManager;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.ModManager;
import me.eldodebug.soar.management.mods.settings.Setting;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ColorSetting;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.ImageSetting;
import me.eldodebug.soar.management.mods.settings.impl.KeybindSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.SoundSetting;
import me.eldodebug.soar.management.mods.settings.impl.TextSetting;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.ui.comp.Comp;
import me.eldodebug.soar.ui.comp.impl.CompColorPicker;
import me.eldodebug.soar.ui.comp.impl.CompComboBox;
import me.eldodebug.soar.ui.comp.impl.CompImageSelect;
import me.eldodebug.soar.ui.comp.impl.CompKeybind;
import me.eldodebug.soar.ui.comp.impl.CompSlider;
import me.eldodebug.soar.ui.comp.impl.CompSoundSelect;
import me.eldodebug.soar.ui.comp.impl.CompToggleButton;
import me.eldodebug.soar.ui.comp.impl.field.CompModTextBox;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.MathUtils;
import me.eldodebug.soar.utils.SearchUtils;
import me.eldodebug.soar.utils.animation.normal.Animation;
import me.eldodebug.soar.utils.animation.normal.Direction;
import me.eldodebug.soar.utils.animation.normal.other.SmoothStepAnimation;
import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;
import me.eldodebug.soar.utils.mouse.MouseUtils;
import me.eldodebug.soar.utils.mouse.Scroll;

public class ModuleCategory extends Category {

	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
	private static final int MOD_ITEM_HEIGHT = 50;
	private static final int SETTING_ITEM_HEIGHT = 29;
	private static final int CATEGORY_OFFSET_Y = 13;
	private static final int MOD_LIST_START_OFFSET = 23;
	private static final float ANIMATION_SPEED = 16.0F;
	private static final Color WARNING_COLOR = new Color(255, 145, 0);
	private static final String RESTRICTION_WARNING = "This mod may be restricted on some servers";
	
	private ModCategory currentCategory;
	private final Scroll settingScroll = new Scroll();
	private boolean openSetting;
	private Animation settingAnimation;
	private Mod currentMod;
	
	private final ArrayList<ModuleSetting> comps = new ArrayList<>();
	
	public ModuleCategory(GuiModMenu parent) {
		super(parent, TranslateText.MODULE, LegacyIcon.ARCHIVE, true, true);
	}
	
	@Override
	public void initGui() {
		currentCategory = ModCategory.ALL;
		openSetting = false;
		settingAnimation = new SmoothStepAnimation(260, 1.0);
		settingAnimation.setValue(1.0);
	}

	@Override
	public void initCategory() {
		scroll.resetAll();
		openSetting = false;
		settingAnimation = new SmoothStepAnimation(260, 1.0);
		settingAnimation.setValue(1.0);
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		Glide instance = Glide.getInstance();
		if (instance == null) return;
		
		NanoVGManager nvg = instance.getNanoVGManager();
		ModManager modManager = instance.getModManager();
		ColorManager colorManager = instance.getColorManager();
		
		if (nvg == null || modManager == null || colorManager == null) return;
		
		ColorPalette palette = colorManager.getPalette();
		AccentColor accentColor = colorManager.getCurrentColor();
		
		if (palette == null || accentColor == null) return;
		
		settingAnimation.setDirection(openSetting ? Direction.BACKWARDS : Direction.FORWARDS);
		
		if (settingAnimation.isDone(Direction.FORWARDS)) {
			this.setCanClose(true);
			currentMod = null;
		}
		
		nvg.save();
		nvg.translate((float) -(600 - (settingAnimation.getValue() * 600)), 0);
		
		drawModScene(nvg, palette, accentColor, modManager, mouseX, mouseY);
		
		nvg.restore();
		
		drawSettingScene(nvg, palette, mouseX, mouseY);
	}
	
	private void drawModScene(NanoVGManager nvg, ColorPalette palette, AccentColor accentColor, ModManager modManager, int mouseX, int mouseY) {
		
		nvg.save();
		nvg.translate(0, scroll.getValue());
		
		drawModCategories(nvg, palette, accentColor);
		int modCount = drawModList(nvg, palette, accentColor, modManager);
		
		nvg.restore();
		
		drawGradientOverlays(nvg, palette);
		
		scroll.setMaxScroll((modCount - (modCount > 5 ? 5.18F : modCount)) * MOD_ITEM_HEIGHT);
	}
	
	private void drawModCategories(NanoVGManager nvg, ColorPalette palette, AccentColor accentColor) {
		
		int offsetX = 0;
		float offsetY = CATEGORY_OFFSET_Y;
		
		for (ModCategory category : ModCategory.values()) {
			
			float textWidth = nvg.getTextWidth(category.getName(), 9, Fonts.MEDIUM);
			boolean isCurrentCategory = category.equals(currentCategory);
			
			category.getBackgroundAnimation().setAnimation(isCurrentCategory ? 1.0F : 0.0F, ANIMATION_SPEED);
			
			Color defaultColor = palette.getBackgroundColor(ColorType.DARK);
			Color color1 = ColorUtils.applyAlpha(accentColor.getColor1(), (int) (category.getBackgroundAnimation().getValue() * 255));
			Color color2 = ColorUtils.applyAlpha(accentColor.getColor2(), (int) (category.getBackgroundAnimation().getValue() * 255));
			Color textColor = category.getTextColorAnimation().getColor(isCurrentCategory ? Color.WHITE : palette.getFontColor(ColorType.DARK), 20);

			nvg.drawRoundedRect(this.getX() + 15 + offsetX, this.getY() + offsetY - 3, textWidth + 20, 16, 6, defaultColor);
			nvg.drawGradientRoundedRect(this.getX() + 15 + offsetX, this.getY() + offsetY - 3, textWidth + 20, 16, 6, color1, color2);
			nvg.drawText(category.getName(), this.getX() + 15 + offsetX + ((textWidth + 20) - textWidth) / 2, this.getY() + offsetY + 1.5F, textColor, 9, Fonts.MEDIUM);
			
			offsetX += textWidth + 28;
		}
	}
	
	private int drawModList(NanoVGManager nvg, ColorPalette palette, AccentColor accentColor, ModManager modManager) {
		
		float offsetY = CATEGORY_OFFSET_Y + MOD_LIST_START_OFFSET;
		int modCount = 0;
		float scrollValue = scroll.getValue();
		
		for (Mod mod : modManager.getMods()) {
			
			if (filterMod(mod)) {
				continue;
			}
			
			modCount++;
			
			if (offsetY + scrollValue + 45 > 0 && offsetY + scrollValue < this.getHeight()) {
				drawSingleMod(nvg, palette, accentColor, modManager, mod, offsetY);
			}
			
			offsetY += MOD_ITEM_HEIGHT;
		}
		
		return modCount;
	}
	
	private void drawSingleMod(NanoVGManager nvg, ColorPalette palette, AccentColor accentColor, ModManager modManager, Mod mod, float offsetY) {
		
		nvg.drawRoundedRect(this.getX() + 15, this.getY() + offsetY, this.getWidth() - 30, 40, 8, palette.getBackgroundColor(ColorType.DARK));
		nvg.drawRoundedRect(this.getX() + 21, this.getY() + offsetY + 6, 28, 28, 6, palette.getBackgroundColor(ColorType.NORMAL));
		
		if (mod.isRestricted()) {
			drawRestrictedModInfo(nvg, palette, mod, offsetY);
		} else {
			drawNormalModInfo(nvg, palette, mod, offsetY);
		}
		
		drawModToggleAnimation(nvg, accentColor, mod, offsetY);
		
		if (modManager.getSettingsByMod(mod) != null) {
			nvg.drawText(LegacyIcon.SETTINGS, this.getX() + this.getWidth() - 39, this.getY() + offsetY + 13.5F, palette.getFontColor(ColorType.NORMAL), 13, Fonts.LEGACYICON);
		}
	}
	
	private void drawRestrictedModInfo(NanoVGManager nvg, ColorPalette palette, Mod mod, float offsetY) {
		
		nvg.drawText(mod.getName(), this.getX() + 56, this.getY() + offsetY + 9F, palette.getFontColor(ColorType.DARK), 13, Fonts.MEDIUM);
		
		float nameWidth = nvg.getTextWidth(mod.getName(), 13, Fonts.MEDIUM);
		nvg.drawText(mod.getDescription(), this.getX() + 56 + nameWidth + 5, this.getY() + offsetY + 12, palette.getFontColor(ColorType.NORMAL), 9, Fonts.REGULAR);
		
		nvg.drawText(LegacyIcon.INFO, this.getX() + 56, this.getY() + offsetY + 23, WARNING_COLOR, 9, Fonts.LEGACYICON);
		
		float iconWidth = nvg.getTextWidth(LegacyIcon.INFO, 9, Fonts.LEGACYICON);
		nvg.drawText(RESTRICTION_WARNING, this.getX() + 57 + iconWidth, this.getY() + offsetY + 24, WARNING_COLOR, 9, Fonts.REGULAR);
	}
	
	private void drawNormalModInfo(NanoVGManager nvg, ColorPalette palette, Mod mod, float offsetY) {
		
		nvg.drawText(mod.getName(), this.getX() + 56, this.getY() + offsetY + 15F, palette.getFontColor(ColorType.DARK), 13, Fonts.MEDIUM);
		
		float nameWidth = nvg.getTextWidth(mod.getName(), 13, Fonts.MEDIUM);
		nvg.drawText(mod.getDescription(), this.getX() + 56 + nameWidth + 5, this.getY() + offsetY + 17, palette.getFontColor(ColorType.NORMAL), 9, Fonts.REGULAR);
	}
	
	private void drawModToggleAnimation(NanoVGManager nvg, AccentColor accentColor, Mod mod, float offsetY) {
		
		mod.getAnimation().setAnimation(mod.isToggled() ? 1.0F : 0.0F, ANIMATION_SPEED);
		
		nvg.save();
		nvg.scale(this.getX() + 21, this.getY() + offsetY + 6, 28, 28, mod.getAnimation().getValue());
		nvg.drawGradientRoundedRect(this.getX() + 21, this.getY() + offsetY + 6, 28, 28, 6, 
			ColorUtils.applyAlpha(accentColor.getColor1(), (int) (mod.getAnimation().getValue() * 255)), 
			ColorUtils.applyAlpha(accentColor.getColor2(), (int) (mod.getAnimation().getValue() * 255)));
		nvg.restore();
	}
	
	private void drawGradientOverlays(NanoVGManager nvg, ColorPalette palette) {
		
		nvg.drawVerticalGradientRect(getX() + 15, this.getY(), getWidth() - 30, 12, palette.getBackgroundColor(ColorType.NORMAL), TRANSPARENT);
		nvg.drawVerticalGradientRect(getX() + 15, this.getY() + this.getHeight() - 12, getWidth() - 30, 12, TRANSPARENT, palette.getBackgroundColor(ColorType.NORMAL));
	}
	
	private void drawSettingScene(NanoVGManager nvg, ColorPalette palette, int mouseX, int mouseY) {
		
		nvg.save();
		nvg.translate((float) (settingAnimation.getValue() * 600), 0);
		
		if (currentMod != null) {
			
			if (MouseUtils.isInside(mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight())) {
				settingScroll.onScroll();
				settingScroll.onAnimation();
			}
			
			drawSettingHeader(nvg, palette);
			drawSettingList(nvg, palette, mouseX, mouseY);
			
			settingScroll.setMaxScroll(getModuleSettingHeight());
		}
		
		nvg.restore();
	}
	
	private void drawSettingHeader(NanoVGManager nvg, ColorPalette palette) {
		
		float offsetY = 15;
		
		nvg.save();
		nvg.drawRoundedRect(this.getX() + 15, this.getY() + offsetY, this.getWidth() - 30, this.getHeight() - 30, 10, palette.getBackgroundColor(ColorType.DARK));
		nvg.drawText(LegacyIcon.CHEVRON_LEFT, this.getX() + 25, this.getY() + offsetY + 8, palette.getFontColor(ColorType.DARK), 13, Fonts.LEGACYICON);
		nvg.drawText(currentMod.getName(), this.getX() + 42, this.getY() + offsetY + 9, palette.getFontColor(ColorType.DARK), 13, Fonts.MEDIUM);
		nvg.drawText(LegacyIcon.REFRESH, this.getX() + this.getWidth() - 39, this.getY() + offsetY + 7.5F, palette.getFontColor(ColorType.DARK), 13, Fonts.LEGACYICON);
	}
	
	private void drawSettingList(NanoVGManager nvg, ColorPalette palette, int mouseX, int mouseY) {
		
		int offsetX = 0;
		float offsetY = 44;
		int setIndex = 0;
		
		nvg.scissor(this.getX() + 15, this.getY() + offsetY, this.getWidth() - 30, this.getHeight() - 59);
		nvg.translate(0, settingScroll.getValue());
		
		for (ModuleSetting setting : comps) {
			
			setting.openAnimation.setAnimation(setting.openY, ANIMATION_SPEED);
			
			nvg.drawText(setting.setting.getName(), this.getX() + offsetX + 26, this.getY() + offsetY + 15F + setting.openAnimation.getValue(), palette.getFontColor(ColorType.DARK), 10, Fonts.MEDIUM);
			
			positionSettingComponent(setting, offsetX, offsetY);
			setting.comp.draw(mouseX, (int) (mouseY - settingScroll.getValue()), 0);
			
			offsetX += 194;
			setIndex++;
			
			if (setIndex % 2 == 0) {
				offsetY += SETTING_ITEM_HEIGHT;
				offsetX = 0;
			}
		}
		
		nvg.restore();
	}
	
	private void positionSettingComponent(ModuleSetting setting, int offsetX, float offsetY) {
		
		Comp comp = setting.comp;
		float animValue = setting.openAnimation.getValue();
		
		if (comp instanceof CompToggleButton) {
			CompToggleButton toggleButton = (CompToggleButton) comp;
			toggleButton.setX(this.getX() + offsetX + 168);
			toggleButton.setY(this.getY() + offsetY + 12 + animValue);
			toggleButton.setScale(0.85F);
		} else if (comp instanceof CompSlider) {
			CompSlider slider = (CompSlider) comp;
			slider.setX(this.getX() + offsetX + 122);
			slider.setY(this.getY() + offsetY + 17 + animValue);
			slider.setWidth(75);
		} else if (comp instanceof CompComboBox) {
			CompComboBox comboBox = (CompComboBox) comp;
			comboBox.setX(this.getX() + offsetX + 122);
			comboBox.setY(this.getY() + offsetY + 11 + animValue);
		} else if (comp instanceof CompKeybind) {
			CompKeybind keybind = (CompKeybind) comp;
			keybind.setX(this.getX() + offsetX + 122);
			keybind.setY(this.getY() + offsetY + 11 + animValue);
		} else if (comp instanceof CompModTextBox) {
			CompModTextBox textBox = (CompModTextBox) comp;
			textBox.setX(this.getX() + offsetX + 122);
			textBox.setY(this.getY() + offsetY + 11 + animValue);
			textBox.setWidth(75);
			textBox.setHeight(16);
		} else if (comp instanceof CompColorPicker) {
			CompColorPicker picker = (CompColorPicker) comp;
			picker.setX(this.getX() + offsetX + 98);
			picker.setY(this.getY() + offsetY + 12.5F + animValue);
			picker.setScale(0.8F);
		} else if (comp instanceof CompImageSelect) {
			CompImageSelect imageSelect = (CompImageSelect) comp;
			imageSelect.setX(this.getX() + offsetX + 181);
			imageSelect.setY(this.getY() + offsetY + 11 + animValue);
		} else if (comp instanceof CompSoundSelect) {
			CompSoundSelect soundSelect = (CompSoundSelect) comp;
			soundSelect.setX(this.getX() + offsetX + 181);
			soundSelect.setY(this.getY() + offsetY + 11 + animValue);
		}
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		
		if (mouseButton != 0 && mouseButton != 3) return;
		
		if (!openSetting) {
			handleModSceneClick(mouseX, mouseY, mouseButton);
		}
		
		if (openSetting && settingAnimation.isDone(Direction.BACKWARDS)) {
			handleSettingSceneClick(mouseX, mouseY, mouseButton);
		}
		
		if (openSetting && mouseButton == 3) {
			openSetting = false;
		}
	}
	
	private void handleModSceneClick(int mouseX, int mouseY, int mouseButton) {
		
		Glide instance = Glide.getInstance();
		if (instance == null) return;
		
		NanoVGManager nvg = instance.getNanoVGManager();
		ModManager modManager = instance.getModManager();
		
		if (nvg == null || modManager == null) return;
		
		handleCategoryClick(nvg, mouseX, mouseY);
		handleModListClick(nvg, modManager, mouseX, mouseY, mouseButton);
	}
	
	private void handleCategoryClick(NanoVGManager nvg, int mouseX, int mouseY) {
		
		int offsetX = 0;
		float offsetY = CATEGORY_OFFSET_Y + scroll.getValue();
		
		for (ModCategory category : ModCategory.values()) {
			
			float textWidth = nvg.getTextWidth(category.getName(), 9, Fonts.MEDIUM);
			
			if (MouseUtils.isInside(mouseX, mouseY, this.getX() + 15 + offsetX, this.getY() + offsetY - 3, textWidth + 20, 16)) {
				currentCategory = category;
				scroll.reset();
				break;
			}
			
			offsetX += textWidth + 28;
		}
	}
	
	private void handleModListClick(NanoVGManager nvg, ModManager modManager, int mouseX, int mouseY, int mouseButton) {
		
		float offsetY = CATEGORY_OFFSET_Y + MOD_LIST_START_OFFSET + scroll.getValue();
		
		for (Mod mod : modManager.getMods()) {
			
			if (filterMod(mod)) {
				continue;
			}
			
			if (!MouseUtils.isInside(mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight())) {
				offsetY += MOD_ITEM_HEIGHT;
				continue;
			}
			
			if (MouseUtils.isInside(mouseX, mouseY, this.getX() + 15, this.getY() + offsetY, this.getWidth() - 60, 40)) {
				mod.toggle();
			}
			
			if (MouseUtils.isInside(mouseX, mouseY, this.getX() + this.getWidth() - 44, this.getY() + offsetY + 9, 22, 22) && !openSetting) {
				openModSettings(modManager, mod);
			}
			
			offsetY += MOD_ITEM_HEIGHT;
		}
	}
	
	private void openModSettings(ModManager modManager, Mod mod) {
		
		ArrayList<Setting> settings = modManager.getSettingsByMod(mod);
		
		if (settings == null) return;
		
		comps.clear();
		
		int offsetX = 0;
		float offsetY = 44;
		int setIndex = 0;
		
		for (Setting setting : settings) {
			
			Comp comp = createSettingComponent(setting, offsetX, offsetY);
			
			if (comp != null) {
				comps.add(new ModuleSetting(setting, comp));
			}
			
			offsetX += 194;
			setIndex++;
			
			if (setIndex % 2 == 0) {
				offsetY += SETTING_ITEM_HEIGHT;
				offsetX = 0;
			}
		}
		
		settingScroll.resetAll();
		currentMod = mod;
		openSetting = true;
		this.setCanClose(false);
	}
	
	private Comp createSettingComponent(Setting setting, int offsetX, float offsetY) {
		
		if (setting instanceof BooleanSetting) {
			return createBooleanComponent((BooleanSetting) setting, offsetX, offsetY);
		}
		
		if (setting instanceof NumberSetting) {
			return createNumberComponent((NumberSetting) setting, offsetX, offsetY);
		}
		
		if (setting instanceof ComboSetting) {
			return createComboComponent((ComboSetting) setting, offsetX, offsetY);
		}
		
		if (setting instanceof ImageSetting) {
			return createImageComponent((ImageSetting) setting, offsetX, offsetY);
		}
		
		if (setting instanceof SoundSetting) {
			return createSoundComponent((SoundSetting) setting, offsetX, offsetY);
		}
		
		if (setting instanceof KeybindSetting) {
			return createKeybindComponent((KeybindSetting) setting, offsetX, offsetY);
		}
		
		if (setting instanceof TextSetting) {
			return createTextComponent((TextSetting) setting, offsetX, offsetY);
		}
		
		if (setting instanceof ColorSetting) {
			return createColorComponent((ColorSetting) setting, offsetX, offsetY);
		}
		
		return null;
	}
	
	private CompToggleButton createBooleanComponent(BooleanSetting setting, int offsetX, float offsetY) {
		
		CompToggleButton toggleButton = new CompToggleButton(setting);
		toggleButton.setX(this.getX() + offsetX + 168);
		toggleButton.setY(this.getY() + offsetY + 8);
		toggleButton.setScale(0.85F);
		return toggleButton;
	}
	
	private CompSlider createNumberComponent(NumberSetting setting, int offsetX, float offsetY) {
		
		CompSlider slider = new CompSlider(setting);
		slider.setX(this.getX() + offsetX + 122);
		slider.setY(this.getY() + offsetY + 13);
		slider.setWidth(75);
		return slider;
	}
	
	private CompComboBox createComboComponent(ComboSetting setting, int offsetX, float offsetY) {
		
		CompComboBox comboBox = new CompComboBox(75, setting);
		comboBox.setX(this.getX() + offsetX + 122);
		comboBox.setY(this.getY() + offsetY + 11);
		return comboBox;
	}
	
	private CompImageSelect createImageComponent(ImageSetting setting, int offsetX, float offsetY) {
		
		CompImageSelect imageSelect = new CompImageSelect(setting);
		imageSelect.setX(this.getX() + offsetX + 181);
		imageSelect.setY(this.getY() + offsetY + 11);
		return imageSelect;
	}
	
	private CompSoundSelect createSoundComponent(SoundSetting setting, int offsetX, float offsetY) {
		
		CompSoundSelect soundSelect = new CompSoundSelect(setting);
		soundSelect.setX(this.getX() + offsetX + 181);
		soundSelect.setY(this.getY() + offsetY + 11);
		return soundSelect;
	}
	
	private CompKeybind createKeybindComponent(KeybindSetting setting, int offsetX, float offsetY) {
		
		CompKeybind keybind = new CompKeybind(75, setting);
		keybind.setX(this.getX() + offsetX + 122);
		keybind.setY(this.getY() + offsetY + 7);
		return keybind;
	}
	
	private CompModTextBox createTextComponent(TextSetting setting, int offsetX, float offsetY) {
		
		CompModTextBox textBox = new CompModTextBox(setting);
		textBox.setX(this.getX() + offsetX + 122);
		textBox.setY(this.getY() + offsetY + 7);
		textBox.setWidth(75);
		textBox.setHeight(16);
		return textBox;
	}
	
	private CompColorPicker createColorComponent(ColorSetting setting, int offsetX, float offsetY) {
		
		CompColorPicker picker = new CompColorPicker(setting);
		picker.setX(this.getX() + offsetX + 98);
		picker.setY(this.getY() + offsetY + 8.5F);
		picker.setScale(0.8F);
		return picker;
	}
	
	private void handleSettingSceneClick(int mouseX, int mouseY, int mouseButton) {
		
		if (MouseUtils.isInside(mouseX, mouseY, this.getX() + 22, this.getY() + 20, 18, 18)) {
			openSetting = false;
			return;
		}
		
		int x = getX() - 32;
		int y = getY() - 31;
		int width = getWidth() + 32;
		int height = getHeight() + 31;
		
		if (!MouseUtils.isInside(mouseX, mouseY, x - 5, y - 5, width + 10, height + 10)) {
			openSetting = false;
			return;
		}
		
		if (!MouseUtils.isInside(mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight())) {
			return;
		}
		
		handleSettingComponentClick(mouseX, mouseY, mouseButton);
		handleResetButtonClick(mouseX, mouseY);
	}
	
	private void handleSettingComponentClick(int mouseX, int mouseY, int mouseButton) {
		
		for (ModuleSetting setting : comps) {
			
			setting.comp.mouseClicked(mouseX, (int) (mouseY - settingScroll.getValue()), mouseButton);
			
			if (setting.comp instanceof CompColorPicker) {
				handleColorPickerExpansion((CompColorPicker) setting.comp, setting, mouseX, (int) (mouseY - settingScroll.getValue()));
			}
		}
	}
	
	private void handleColorPickerExpansion(CompColorPicker picker, ModuleSetting currentSetting, int mouseX, int mouseY) {
		
		if (!picker.isInsideOpen(mouseX, mouseY)) {
			return;
		}
		
		int openIndex = 1;
		int currentIndex = comps.indexOf(currentSetting);
		int add = picker.isShowAlpha() ? 100 : 85;
		
		while ((openIndex * 2) + currentIndex < comps.size()) {
			ModuleSetting targetSetting = comps.get((openIndex * 2) + currentIndex);
			targetSetting.openY += picker.isOpen() ? add : -add;
			openIndex++;
		}
	}
	
	private void handleResetButtonClick(int mouseX, int mouseY) {
		
		if (MouseUtils.isInside(mouseX, mouseY, this.getX() + this.getWidth() - 41, this.getY() + 15 + 6F, 16, 16)) {
			for (ModuleSetting setting : comps) {
				setting.setting.reset();
			}
		}
	}
	
	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
		
		if (!MouseUtils.isInside(mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight())) {
			return;
		}
		
		if (mouseButton != 0) {
			return;
		}
		
		for (ModuleSetting setting : comps) {
			setting.comp.mouseReleased(mouseX, mouseY, mouseButton);
		}
	}
	
	@Override
	public void keyTyped(char typedChar, int keyCode) {
		
		boolean binding = isAnyKeybindActive();
		
		for (ModuleSetting setting : comps) {
			setting.comp.keyTyped(typedChar, keyCode);
		}
		
		if (binding) {
			return;
		}
		
		if (openSetting && keyCode == Keyboard.KEY_ESCAPE) {
			openSetting = false;
			return;
		}
		
		if (!openSetting) {
			scroll.onKey(keyCode);
			if (keyCode != 0xD0 && keyCode != 0xC8 && keyCode != Keyboard.KEY_ESCAPE) {
				this.getSearchBox().setFocused(true);
			}
		}
	}
	
	private boolean isAnyKeybindActive() {
		
		for (ModuleSetting setting : comps) {
			if (setting.comp instanceof CompKeybind) {
				CompKeybind keybind = (CompKeybind) setting.comp;
				if (keybind.isBinding()) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean filterMod(Mod mod) {
		
		if (mod == null) return true;
		if (mod.isHide()) return true;
		if (!mod.getAllowed()) return true;
		
		if (!currentCategory.equals(ModCategory.ALL) && !mod.getCategory().equals(currentCategory)) {
			return true;
		}
		
		if (this.getSearchBox() != null && !this.getSearchBox().getText().isEmpty()) {
			Glide instance = Glide.getInstance();
			if (instance == null) return true;
			
			ModManager modManager = instance.getModManager();
			if (modManager == null) return true;
			
			if (!SearchUtils.isSimillar(modManager.getWords(mod), this.getSearchBox().getText())) {
				return true;
			}
		}
		
		return false;
	}
	
	private int getModuleSettingHeight() {
		
		int oddOutput = 0;
		int evenOutput = 0;
		int oddTotal = 0;
		int evenTotal = 0;
		
		for (int i = 0; i < comps.size(); i++) {
			
			if (MathUtils.isOdd(i + 1)) {
				oddOutput += SETTING_ITEM_HEIGHT;
			} else {
				evenOutput += SETTING_ITEM_HEIGHT;
			}
			
			ModuleSetting setting = comps.get(i);
			
			if (setting.comp instanceof CompColorPicker) {
				CompColorPicker picker = (CompColorPicker) setting.comp;
				if (picker.isOpen()) {
					int add = picker.isShowAlpha() ? 100 : 85;
					if (MathUtils.isOdd(i + 1)) {
						oddTotal += add;
					} else {
						evenTotal += add;
					}
				}
			}
		}
		
		int output = Math.max(oddOutput, evenOutput) + Math.max(oddTotal, evenTotal);
		
		return Math.max(0, output - (this.getHeight() - 72));
	}
	
	private static class ModuleSetting {
		
		private final SimpleAnimation openAnimation = new SimpleAnimation();
		private final Setting setting;
		private final Comp comp;
		private float openY;
		
		public ModuleSetting(Setting setting, Comp comp) {
			this.setting = setting;
			this.comp = comp;
			this.openY = 0;
		}
	}
}