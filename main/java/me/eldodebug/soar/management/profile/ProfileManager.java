package me.eldodebug.soar.management.profile;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.color.ColorManager;
import me.eldodebug.soar.management.color.Theme;
import me.eldodebug.soar.management.file.FileManager;
import me.eldodebug.soar.management.language.Language;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModManager;
import me.eldodebug.soar.management.mods.settings.Setting;
import me.eldodebug.soar.management.mods.settings.impl.*;
import me.eldodebug.soar.management.profile.mainmenu.BackgroundManager;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.JsonUtils;
import me.eldodebug.soar.utils.file.FileUtils;

public class ProfileManager {

	// ✅ Constants for JSON keys
	private static final String KEY_PROFILE_DATA = "Profile Data";
	private static final String KEY_APPEARANCE = "Appearance";
	private static final String KEY_MODS = "Mods";
	private static final String KEY_SETTINGS = "Settings";
	private static final String KEY_TOGGLE = "Toggle";
	private static final String KEY_SERVER = "Server";
	private static final String KEY_ICON = "Icon";
	private static final String KEY_TYPE = "Type";
	private static final String KEY_ACCENT_COLOR = "Accent Color";
	private static final String KEY_THEME = "Theme";
	private static final String KEY_BACKGROUND = "Background";
	private static final String KEY_LANGUAGE = "Language";
	private static final String KEY_X = "X";
	private static final String KEY_Y = "Y";
	private static final String KEY_WIDTH = "Width";
	private static final String KEY_HEIGHT = "Height";
	private static final String KEY_SCALE = "Scale";
	
	private static final String DEFAULT_PROFILE_NAME = "Default.json";
	private static final String NULL_VALUE = "null";
	private static final String EXTENSION_JSON = "json";
	
	private static final int SPECIAL_PROFILE_ID = 999;
	
	private final CopyOnWriteArrayList<Profile> profiles = new CopyOnWriteArrayList<>();
	private final BackgroundManager backgroundManager;
	private final Gson gson;
	
	public ProfileManager() {
		this.backgroundManager = new BackgroundManager();
		this.gson = new Gson();
		this.loadProfiles(true);
	}
	
	public void loadProfiles(boolean loadDefaultProfile) {
		File profileDir = Glide.getInstance().getFileManager().getProfileDir();
		if (profileDir == null || !profileDir.exists()) {
			GlideLogger.error("Profile directory does not exist");
			return;
		}

		profiles.clear();
		
		File[] files = profileDir.listFiles();
		if (files == null) {
			profiles.add(createSpecialProfile());
			return;
		}

		int id = 0;
		for (File file : files) {
			if (!isValidProfileFile(file)) continue;
			
			if (file.getName().equals(DEFAULT_PROFILE_NAME)) {
				if (loadDefaultProfile) {
					load(file);
				}
			} else {
				Profile profile = loadProfile(file, id);
				if (profile != null) {
					profiles.add(profile);
					id++;
				}
			}
		}
		
		profiles.add(createSpecialProfile());
	}
	
	private boolean isValidProfileFile(File file) {
		return file != null && file.exists() && EXTENSION_JSON.equals(FileUtils.getExtension(file));
	}
	
	private Profile loadProfile(File file, int id) {
		try (FileReader reader = new FileReader(file)) {
			JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
			if (jsonObject == null) return null;
			
			JsonObject profileData = JsonUtils.getObjectProperty(jsonObject, KEY_PROFILE_DATA);
			if (profileData == null) return null;
			
			String serverIp = JsonUtils.getStringProperty(profileData, KEY_SERVER, "");
			ProfileIcon icon = ProfileIcon.getIconById(
				JsonUtils.getIntProperty(profileData, KEY_ICON, ProfileIcon.GRASS.getId()));
			ProfileType type = ProfileType.getTypeById(
				JsonUtils.getIntProperty(profileData, KEY_TYPE, ProfileType.ALL.getId()));
			
			Profile profile = new Profile(id, serverIp, file, icon);
			profile.setType(type);
			
			return profile;
		} catch (Exception e) {
			GlideLogger.error("Failed to load profile: " + file.getName(), e);
			return null;
		}
	}
	
	private Profile createSpecialProfile() {
		return new Profile(SPECIAL_PROFILE_ID, "", null, null);
	}
	
	public void save(File file, String serverIp, ProfileType type, ProfileIcon icon) {
		if (file == null) {
			GlideLogger.error("Cannot save profile: file is null");
			return;
		}
		
		Glide instance = Glide.getInstance();
		ModManager modManager = instance.getModManager();
		ColorManager colorManager = instance.getColorManager();
		
		try (FileWriter writer = new FileWriter(file)) {
			JsonObject jsonObject = new JsonObject();
			
			// ✅ Profile data
			jsonObject.add(KEY_PROFILE_DATA, createProfileData(serverIp, type, icon));
			
			// ✅ Appearance
			jsonObject.add(KEY_APPEARANCE, createAppearanceData(instance, colorManager));
			
			// ✅ Mods
			jsonObject.add(KEY_MODS, createModsData(modManager));
			
			gson.toJson(jsonObject, writer);
			
		} catch (Exception e) {
			GlideLogger.error("Failed to save profile: " + file.getName(), e);
		}
	}
	
	private JsonObject createProfileData(String serverIp, ProfileType type, ProfileIcon icon) {
		JsonObject profileData = new JsonObject();
		profileData.addProperty(KEY_ICON, icon != null ? icon.getId() : ProfileIcon.GRASS.getId());
		profileData.addProperty(KEY_TYPE, type != null ? type.getId() : ProfileType.ALL.getId());
		profileData.addProperty(KEY_SERVER, serverIp != null ? serverIp : "");
		return profileData;
	}
	
	private JsonObject createAppearanceData(Glide instance, ColorManager colorManager) {
		JsonObject appData = new JsonObject();
		appData.addProperty(KEY_ACCENT_COLOR, colorManager.getCurrentColor().getName());
		appData.addProperty(KEY_THEME, colorManager.getTheme().getId());
		appData.addProperty(KEY_BACKGROUND, backgroundManager.getCurrentBackground().getId());
		appData.addProperty(KEY_LANGUAGE, instance.getLanguageManager().getCurrentLanguage().getId());
		return appData;
	}
	
	private JsonObject createModsData(ModManager modManager) {
		JsonObject modsData = new JsonObject();
		
		for (Mod mod : modManager.getMods()) {
			if (mod == null) continue;
			
			JsonObject modData = new JsonObject();
			modData.addProperty(KEY_TOGGLE, mod.isToggled());
			
			// ✅ HUD data
			if (mod instanceof HUDMod) {
				addHUDData(modData, (HUDMod) mod);
			}
			
			// ✅ Settings
			addSettingsData(modData, modManager, mod);
			
			modsData.add(mod.getNameKey(), modData);
		}
		
		return modsData;
	}
	
	private void addHUDData(JsonObject modData, HUDMod hudMod) {
		modData.addProperty(KEY_X, hudMod.getX());
		modData.addProperty(KEY_Y, hudMod.getY());
		modData.addProperty(KEY_WIDTH, hudMod.getWidth());
		modData.addProperty(KEY_HEIGHT, hudMod.getHeight());
		modData.addProperty(KEY_SCALE, hudMod.getScale());
	}
	
	private void addSettingsData(JsonObject modData, ModManager modManager, Mod mod) {
		if (modManager.getSettingsByMod(mod) == null) return;
		
		JsonObject settingsData = new JsonObject();
		
		for (Setting setting : modManager.getSettingsByMod(mod)) {
			if (setting == null) continue;
			
			String key = setting.getNameKey();
			
			// ✅ Polymorphic saving
			if (setting instanceof ColorSetting) {
				settingsData.addProperty(key, ((ColorSetting) setting).getColor().getRGB());
			} else if (setting instanceof BooleanSetting) {
				settingsData.addProperty(key, ((BooleanSetting) setting).isToggled());
			} else if (setting instanceof ComboSetting) {
				settingsData.addProperty(key, ((ComboSetting) setting).getOption().getNameKey());
			} else if (setting instanceof NumberSetting) {
				settingsData.addProperty(key, ((NumberSetting) setting).getValue());
			} else if (setting instanceof TextSetting) {
				settingsData.addProperty(key, ((TextSetting) setting).getText());
			} else if (setting instanceof KeybindSetting) {
				settingsData.addProperty(key, ((KeybindSetting) setting).getKeyCode());
			} else if (setting instanceof ImageSetting) {
				File image = ((ImageSetting) setting).getImage();
				settingsData.addProperty(key, image == null ? NULL_VALUE : image.getName());
			} else if (setting instanceof SoundSetting) {
				File sound = ((SoundSetting) setting).getSound();
				settingsData.addProperty(key, sound == null ? NULL_VALUE : sound.getName());
			}
		}
		
		modData.add(KEY_SETTINGS, settingsData);
	}
	
	public void save() {
		File defaultProfile = new File(
			Glide.getInstance().getFileManager().getProfileDir(), 
			DEFAULT_PROFILE_NAME
		);
		save(defaultProfile, "", ProfileType.ALL, ProfileIcon.GRASS);
	}
	
	public void load(File file) {
		if (file == null || !file.exists()) {
			GlideLogger.error("Cannot load profile: file is null or does not exist");
			return;
		}
		
		Glide instance = Glide.getInstance();
		ModManager modManager = instance.getModManager();
		ColorManager colorManager = instance.getColorManager();
		FileManager fileManager = instance.getFileManager();
		
		try (FileReader reader = new FileReader(file)) {
			JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
			if (jsonObject == null) {
				GlideLogger.error("Invalid JSON in profile file");
				return;
			}
			
			// ✅ Load appearance
			loadAppearance(jsonObject, instance, colorManager);
			
			// ✅ Load mods
			loadMods(jsonObject, modManager, fileManager);
			
		} catch (Exception e) {
			GlideLogger.error("Failed to load profile: " + file.getName(), e);
		}
	}
	
	private void loadAppearance(JsonObject jsonObject, Glide instance, ColorManager colorManager) {
		JsonObject appData = JsonUtils.getObjectProperty(jsonObject, KEY_APPEARANCE);
		if (appData == null) return;
		
		String accentColorName = JsonUtils.getStringProperty(appData, KEY_ACCENT_COLOR, "Teal Love");
		int themeId = JsonUtils.getIntProperty(appData, KEY_THEME, Theme.LIGHT.getId());
		int backgroundId = JsonUtils.getIntProperty(appData, KEY_BACKGROUND, 0);
		String languageId = JsonUtils.getStringProperty(appData, KEY_LANGUAGE, Language.ENGLISHGB.getId());
		
		colorManager.setCurrentColor(colorManager.getColorByName(accentColorName));
		colorManager.setTheme(Theme.getThemeById(themeId));
		backgroundManager.setCurrentBackground(backgroundManager.getBackgroundById(backgroundId));
		instance.getLanguageManager().setCurrentLanguage(Language.getLanguageById(languageId));
	}
	
	private void loadMods(JsonObject jsonObject, ModManager modManager, FileManager fileManager) {
		JsonObject modsData = JsonUtils.getObjectProperty(jsonObject, KEY_MODS);
		if (modsData == null) return;
		
		for (Mod mod : modManager.getMods()) {
			if (mod == null) continue;
			
			JsonObject modData = JsonUtils.getObjectProperty(modsData, mod.getNameKey());
			if (modData == null) continue;
			
			// ✅ Toggle state
			mod.setToggled(JsonUtils.getBooleanProperty(modData, KEY_TOGGLE, false));
			
			// ✅ HUD position
			if (mod instanceof HUDMod) {
				loadHUDData(modData, (HUDMod) mod);
			}
			
			// ✅ Settings
			loadModSettings(modData, mod, modManager, fileManager);
		}
	}
	
	private void loadHUDData(JsonObject modData, HUDMod hudMod) {
		hudMod.setX(JsonUtils.getIntProperty(modData, KEY_X, 100));
		hudMod.setY(JsonUtils.getIntProperty(modData, KEY_Y, 100));
		hudMod.setWidth(JsonUtils.getIntProperty(modData, KEY_WIDTH, 100));
		hudMod.setHeight(JsonUtils.getIntProperty(modData, KEY_HEIGHT, 100));
		hudMod.setScale(JsonUtils.getFloatProperty(modData, KEY_SCALE, 1.0F));
	}
	
	private void loadModSettings(JsonObject modData, Mod mod, ModManager modManager, FileManager fileManager) {
		if (modManager.getSettingsByMod(mod) == null) return;
		
		JsonObject settingsData = JsonUtils.getObjectProperty(modData, KEY_SETTINGS);
		if (settingsData == null) return;
		
		for (Setting setting : modManager.getSettingsByMod(mod)) {
			if (setting == null) continue;
			
			String key = setting.getNameKey();
			
			// ✅ Polymorphic loading
			if (setting instanceof ColorSetting) {
				int colorInt = JsonUtils.getIntProperty(settingsData, key, Color.RED.getRGB());
				((ColorSetting) setting).setColor(ColorUtils.getColorByInt(colorInt));
				
			} else if (setting instanceof BooleanSetting) {
				boolean value = JsonUtils.getBooleanProperty(settingsData, key, false);
				((BooleanSetting) setting).setToggled(value);
				
			} else if (setting instanceof ComboSetting) {
				ComboSetting comboSetting = (ComboSetting) setting;
				String optionKey = JsonUtils.getStringProperty(settingsData, key, comboSetting.getDefaultOption().getNameKey());
				comboSetting.setOption(comboSetting.getOptionByNameKey(optionKey));
				
			} else if (setting instanceof NumberSetting) {
				NumberSetting numberSetting = (NumberSetting) setting;
				double value = JsonUtils.getDoubleProperty(settingsData, key, numberSetting.getDefaultValue());
				numberSetting.setValue(value);
				
			} else if (setting instanceof TextSetting) {
				TextSetting textSetting = (TextSetting) setting;
				String text = JsonUtils.getStringProperty(settingsData, key, textSetting.getDefaultText());
				textSetting.setText(text);
				
			} else if (setting instanceof KeybindSetting) {
				KeybindSetting keybindSetting = (KeybindSetting) setting;
				int keyCode = JsonUtils.getIntProperty(settingsData, key, keybindSetting.getDefaultKeyCode());
				keybindSetting.setKeyCode(keyCode);
				
			} else if (setting instanceof ImageSetting) {
				loadImageSetting((ImageSetting) setting, settingsData, key, fileManager);
				
			} else if (setting instanceof SoundSetting) {
				loadSoundSetting((SoundSetting) setting, settingsData, key, fileManager);
			}
		}
	}
	
	private void loadImageSetting(ImageSetting setting, JsonObject settingsData, String key, FileManager fileManager) {
		String fileName = JsonUtils.getStringProperty(settingsData, key, null);
		if (fileName == null || fileName.equals(NULL_VALUE)) return;
		
		File cacheDir = new File(fileManager.getCacheDir(), "custom-image");
		if (!cacheDir.exists()) return;
		
		File image = new File(cacheDir, fileName);
		if (image.exists()) {
			setting.setImage(image);
		}
	}
	
	private void loadSoundSetting(SoundSetting setting, JsonObject settingsData, String key, FileManager fileManager) {
		String fileName = JsonUtils.getStringProperty(settingsData, key, null);
		if (fileName == null || fileName.equals(NULL_VALUE)) return;
		
		File cacheDir = new File(fileManager.getCacheDir(), "custom-sound");
		if (!cacheDir.exists()) return;
		
		File sound = new File(cacheDir, fileName);
		if (sound.exists()) {
			setting.setSound(sound);
		}
	}
	
	public void delete(Profile profile) {
		if (profile == null) return;
		
		profiles.remove(profile);
		
		File jsonFile = profile.getJsonFile();
		if (jsonFile != null && jsonFile.exists()) {
			if (!jsonFile.delete()) {
				GlideLogger.error("Failed to delete profile file: " + jsonFile.getName());
			}
		}
	}
	
	public BackgroundManager getBackgroundManager() {
		return backgroundManager;
	}

	public CopyOnWriteArrayList<Profile> getProfiles() {
		return profiles;
	}
}