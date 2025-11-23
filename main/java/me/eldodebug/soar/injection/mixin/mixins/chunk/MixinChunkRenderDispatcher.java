package me.eldodebug.soar.injection.mixin.mixins.chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.eldodebug.soar.management.mods.impl.FPSBoostMod;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

@Mixin(ChunkRenderDispatcher.class)
public class MixinChunkRenderDispatcher {
    
    @Inject(method = "getNextChunkUpdate", at = @At("HEAD"))
    private void limitChunkUpdates(CallbackInfoReturnable<ChunkCompileTaskGenerator> cir) {
        FPSBoostMod mod = FPSBoostMod.getInstance();
        if (mod == null || !mod.isToggled()) return;
        
        BooleanSetting delaySetting = mod.getChunkDelaySetting();
        if (delaySetting == null || !delaySetting.isToggled()) return;
        
        NumberSetting delayValue = mod.getDelaySetting();
        if (delayValue == null) return;
        
        try {
            long delay = delayValue.getValueLong();
            if (delay > 0 && delay < 1000) {
                Thread.sleep(delay * 15);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
        }
    }
}