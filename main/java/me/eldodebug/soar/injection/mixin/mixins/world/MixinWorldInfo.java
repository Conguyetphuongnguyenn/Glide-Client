package me.eldodebug.soar.injection.mixin.mixins.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.impl.TimeChangerMod;
import me.eldodebug.soar.management.mods.impl.WeatherChangerMod;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import net.minecraft.world.storage.WorldInfo;

@Mixin(WorldInfo.class)
public class MixinWorldInfo {

    @Shadow
    private long worldTime;
    
    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    public void preIsRaining(CallbackInfoReturnable<Boolean> cir) {
        WeatherChangerMod mod = WeatherChangerMod.getInstance();
        if (mod == null || !mod.isToggled()) return;
        
        ComboSetting setting = mod.getWeatherSetting();
        if (setting == null) return;
        
        Option weather = setting.getOption();
        if (weather == null || weather.getTranslate() == null) return;
        
        cir.setReturnValue(weather.getTranslate().equals(TranslateText.CLEAR));
    }
    
    @Inject(method = "isThundering", at = @At("HEAD"), cancellable = true)
    public void preIsThundering(CallbackInfoReturnable<Boolean> cir) {
        WeatherChangerMod mod = WeatherChangerMod.getInstance();
        if (mod == null || !mod.isToggled()) return;
        
        ComboSetting setting = mod.getWeatherSetting();
        if (setting == null) return;
        
        Option weather = setting.getOption();
        if (weather == null || weather.getTranslate() == null) return;
        
        cir.setReturnValue(weather.getTranslate().equals(TranslateText.STORM));
    }
    
    @Overwrite
    public long getWorldTime() {
        TimeChangerMod mod = TimeChangerMod.getInstance();
        
        if (mod != null && mod.isToggled() && mod.getTimeSetting() != null) {
            return (long) (mod.getTimeSetting().getValueFloat() * 1_000L) + 18_000L;
        }
        
        return this.worldTime;
    }
}