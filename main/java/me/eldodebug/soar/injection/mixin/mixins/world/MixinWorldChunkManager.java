package me.eldodebug.soar.injection.mixin.mixins.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.impl.WeatherChangerMod;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import net.minecraft.world.biome.WorldChunkManager;

@Mixin(WorldChunkManager.class)
public class MixinWorldChunkManager {

    @Inject(method = "getTemperatureAtHeight", at = @At("HEAD"), cancellable = true)
    public void preGetTemperatureAtHeight(float p_76939_1_, int p_76939_2_, CallbackInfoReturnable<Float> cir) {
        WeatherChangerMod mod = WeatherChangerMod.getInstance();
        if (mod == null || !mod.isToggled()) return;
        
        ComboSetting setting = mod.getWeatherSetting();
        if (setting == null) return;
        
        Option weather = setting.getOption();
        if (weather == null || weather.getTranslate() == null) return;
        
        if (weather.getTranslate().equals(TranslateText.SNOW)) {
            cir.setReturnValue(0F);
        }
    }
}