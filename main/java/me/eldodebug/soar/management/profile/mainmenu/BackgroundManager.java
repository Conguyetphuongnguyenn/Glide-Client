package me.eldodebug.soar.management.profile.mainmenu;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.file.FileManager;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.profile.mainmenu.impl.Background;
import me.eldodebug.soar.management.profile.mainmenu.impl.CustomBackground;
import me.eldodebug.soar.management.profile.mainmenu.impl.DefaultBackground;
import me.eldodebug.soar.utils.JsonUtils;
import me.eldodebug.soar.utils.file.FileUtils;
import net.minecraft.util.ResourceLocation;

public class BackgroundManager {

	private CopyOnWriteArrayList<Background> backgrounds = new CopyOnWriteArrayList<Background>();
	private CopyOnWriteArrayList<CustomBackground> removeBackgrounds = new CopyOnWriteArrayList<CustomBackground>();
	private Background currentBackground;
	private Background fallbackBackground;
	
	public BackgroundManager() {
		
		Glide instance = Glide.getInstance();
		if (instance == null) {
			GlideLogger.error("[BackgroundManager] Glide instance is null!");
			return;
		}
		
		FileManager fileManager = instance.getFileManager();
		if (fileManager == null) {
			GlideLogger.error("[BackgroundManager] FileManager is null!");
			return;
		}
		
		File bgCacheDir = new File(fileManager.getCacheDir(), "background");
		File dataJson = new File(bgCacheDir, "Data.json");
		
		if (!bgCacheDir.exists()) {
			fileManager.createDir(bgCacheDir);
		}
		
		if (!dataJson.exists()) {
			fileManager.createFile(dataJson);
		}
		
		backgrounds.add(new DefaultBackground(0, TranslateText.GRADIENT, new ResourceLocation("soar/mainmenu/background.png")));
		backgrounds.add(new DefaultBackground(1, TranslateText.NIGHT, new ResourceLocation("soar/mainmenu/background-night.png")));
		backgrounds.add(new DefaultBackground(2, TranslateText.DOLPHIN, new ResourceLocation("soar/mainmenu/background-dolphin.png")));
		backgrounds.add(new DefaultBackground(3, TranslateText.UNITY, new ResourceLocation("soar/mainmenu/background-unity.png")));
		backgrounds.add(new DefaultBackground(999, TranslateText.ADD, null));

		fallbackBackground = backgrounds.get(0);
		
		ArrayList<String> removeImages = load();
		
		if (bgCacheDir.listFiles() != null) {
			for (File f : bgCacheDir.listFiles()) {
				if (f == null) continue;
				
				if (FileUtils.getExtension(f).equals("png")) {
					if (!removeImages.isEmpty() && removeImages.contains(f.getName())) {
						f.delete();
					} else {
						addCustomBackground(f);
					}
				}
			}
		}
		
		currentBackground = getBackgroundByIdSafe(0);
		
		GlideLogger.info("[BackgroundManager] Initialized with " + backgrounds.size() + " backgrounds");
	}

	public ArrayList<String> load() {
		
		Glide instance = Glide.getInstance();
		if (instance == null) return new ArrayList<>();
		
		FileManager fileManager = instance.getFileManager();
		if (fileManager == null) return new ArrayList<>();
		
		File bgCacheDir = new File(fileManager.getCacheDir(), "background");
		File dataJson = new File(bgCacheDir, "Data.json");
		ArrayList<String> output = new ArrayList<String>();
		
		if (!dataJson.exists()) return output;
		
		try (FileReader reader = new FileReader(dataJson)) {
			
			Gson gson = new Gson();
			JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
			
			if (jsonObject != null) {
				
				JsonArray jsonArray = JsonUtils.getArrayProperty(jsonObject, "Remove Images");
				
				if (jsonArray != null) {
					
					Iterator<JsonElement> iterator = jsonArray.iterator();
					
					while (iterator.hasNext()) {
						
						JsonElement jsonElement = iterator.next();
						if (jsonElement == null) continue;
						
						JsonObject rJsonObject = gson.fromJson(jsonElement, JsonObject.class);
						if (rJsonObject == null) continue;
						
						output.add(JsonUtils.getStringProperty(rJsonObject, "Image", "null"));
					}
				}
			}
		} catch (Exception e) {
			GlideLogger.error("[BackgroundManager] Failed to load data", e);
		}
		
		return output;
	}
	
	public void save() {
		
		Glide instance = Glide.getInstance();
		if (instance == null) return;
		
		FileManager fileManager = instance.getFileManager();
		if (fileManager == null) return;
		
		File bgCacheDir = new File(fileManager.getCacheDir(), "background");
		File dataJson = new File(bgCacheDir, "Data.json");
		
		try (FileWriter writer = new FileWriter(dataJson)) {
			
			JsonObject jsonObject = new JsonObject();
			JsonArray jsonArray = new JsonArray();
			Gson gson = new Gson();
			
			for (CustomBackground bg : removeBackgrounds) {
				if (bg == null || bg.getImage() == null) continue;
				
				JsonObject innerJsonObject = new JsonObject();
				innerJsonObject.addProperty("Image", bg.getImage().getName());
				jsonArray.add(innerJsonObject);
			}
			
			jsonObject.add("Remove Images", jsonArray);
			
			gson.toJson(jsonObject, writer);
			
		} catch (Exception e) {
			GlideLogger.error("[BackgroundManager] Failed to save data", e);
		}
	}
	
	public CopyOnWriteArrayList<Background> getBackgrounds() {
		return backgrounds;
	}
	
	public Background getBackgroundById(int id) {
		
		if (backgrounds == null || backgrounds.isEmpty()) {
			return fallbackBackground;
		}
		
		for (Background bg : backgrounds) {
			if (bg == null) continue;
			if (bg.getId() == id) {
				return bg;
			}
		}
		
		GlideLogger.warn("[BackgroundManager] Background ID " + id + " not found, using fallback");
		return fallbackBackground != null ? fallbackBackground : backgrounds.get(0);
	}
	
	public Background getBackgroundByIdSafe(int id) {
		Background result = getBackgroundById(id);
		return result != null ? result : fallbackBackground;
	}
	
	public int getMaxId() {
		
		if (backgrounds == null || backgrounds.isEmpty()) {
			return 0;
		}
		
		int maxId = 0;
		
		for (Background bg : backgrounds) {
			if (bg == null) continue;
			if (bg.getId() != 999 && bg.getId() > maxId) {
				maxId = bg.getId();
			}
		}
		
		return maxId;
	}
	
	public void addCustomBackground(File image) {
		
		if (image == null || !image.exists()) {
			GlideLogger.warn("[BackgroundManager] Cannot add null or non-existent image");
			return;
		}
		
		if (backgrounds == null) {
			GlideLogger.error("[BackgroundManager] Backgrounds list is null!");
			return;
		}
		
		int maxId = getMaxId();
		int index = -1;
		
		for (int i = 0; i < backgrounds.size(); i++) {
			Background bg = backgrounds.get(i);
			if (bg != null && bg.getId() == 999) {
				index = i;
				break;
			}
		}
		
		if (index == -1) {
			index = backgrounds.size();
		}
		
		String name = image.getName().replace(".png", "");
		backgrounds.add(index, new CustomBackground(maxId + 1, name, image));
		
		GlideLogger.info("[BackgroundManager] Added custom background: " + name);
	}
	
	public void removeCustomBackground(CustomBackground cusBackground) {
		
		if (cusBackground == null) {
			GlideLogger.warn("[BackgroundManager] Cannot remove null background");
			return;
		}
		
		Glide instance = Glide.getInstance();
		if (instance == null) return;
		
		NanoVGManager nvg = instance.getNanoVGManager();
		if (nvg == null) return;
		
		if (cusBackground.getImage() != null) {
			nvg.getAssetManager().removeImage(nvg.getContext(), cusBackground.getImage());
		}
		
		backgrounds.remove(cusBackground);
		removeBackgrounds.add(cusBackground);
		
		save();
		
		GlideLogger.info("[BackgroundManager] Removed custom background");
	}
	
	public Background getCurrentBackground() {
		return currentBackground != null ? currentBackground : fallbackBackground;
	}

	public void setCurrentBackground(Background newBackground) {
		if (newBackground != null) {
			this.currentBackground = newBackground;
		} else {
			GlideLogger.warn("[BackgroundManager] Attempted to set null background, using fallback");
			this.currentBackground = fallbackBackground;
		}
	}
	
	public void cleanup() {
		if (backgrounds != null) {
			backgrounds.clear();
		}
		if (removeBackgrounds != null) {
			removeBackgrounds.clear();
		}
		currentBackground = null;
		fallbackBackground = null;
		
		GlideLogger.info("[BackgroundManager] Cleaned up");
	}
}