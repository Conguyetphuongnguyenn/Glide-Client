package me.eldodebug.soar.management.profile.mainmenu.impl;

import me.eldodebug.soar.management.language.TranslateText;
import net.minecraft.util.ResourceLocation;

public class DefaultBackground extends Background {

	private String nameKey;
	private ResourceLocation image;
	
	public DefaultBackground(int id, TranslateText nameTranslate, ResourceLocation image) {
		super(id, nameTranslate != null ? nameTranslate.getText() : "Unknown");
		this.nameKey = nameTranslate != null ? nameTranslate.getKey() : "unknown";
		this.image = image;
	}
	
	@Override
	public String getName() {
		return super.getName();
	}

	public String getNameKey() {
		return nameKey != null ? nameKey : "unknown";
	}

	public ResourceLocation getImage() {
		return image;
	}
}