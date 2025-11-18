package me.eldodebug.soar.management.mods.impl;

import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;

public class ItemPhysicsMod extends Mod {

	private static ItemPhysicsMod instance;
	
	private NumberSetting speedSetting = new NumberSetting(TranslateText.SPEED, this, 1, 0.5, 4, false);
	
	public ItemPhysicsMod() {
		super(TranslateText.ITEM_PHYSICS, TranslateText.ITEM_PHYSICS_DESCRIPTION, ModCategory.RENDER);
		
		instance = this;
	}

	@Override
	public void onEnable() {
		super.onEnable();
		
		// NULL CHECK: Items2DMod có thể chưa init
		Items2DMod items2D = Items2DMod.getInstance();
		if (items2D != null && items2D.isToggled()) {
			items2D.setToggled(false);
		}
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
		// Config-only mod - Settings auto-managed bởi ModManager
	}
	
	public static ItemPhysicsMod getInstance() {
		return instance;
	}

	public NumberSetting getSpeedSetting() {
		return speedSetting;
	}
}