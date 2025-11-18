package me.eldodebug.soar.injection.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.eldodebug.soar.management.mods.impl.DamageTiltMod;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

@Mixin(Entity.class)
public class MixinEntity {

    @Shadow
    public boolean onGround;

    @Inject(method = "spawnRunningParticles", at = @At("HEAD"), cancellable = true)
    private void checkGroundState(CallbackInfo ci) {
        if (!this.onGround) {
            ci.cancel();
        }
    }

    @Redirect(method = "getBrightnessForRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isBlockLoaded(Lnet/minecraft/util/BlockPos;)Z"))
    public boolean alwaysReturnTrue(World world, BlockPos pos) {
        return true;
    }

    @Inject(method = "setVelocity", at = @At("HEAD"))
    public void preSetVelocity(double x, double y, double z, CallbackInfo ci) {
        DamageTiltMod mod = DamageTiltMod.getInstance();
        if (mod == null || !mod.isToggled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        Entity self = (Entity)(Object)this;
        if (self != mc.thePlayer) return;

        EntityPlayer player = mc.thePlayer;
        double dx = player.motionX - x;
        double dz = player.motionZ - z;

        if (dx == 0.0 && dz == 0.0) return;

        double angleRadians = Math.atan2(dz, dx);
        float result = (float)(Math.toDegrees(angleRadians) - player.rotationYaw);

        if (Float.isFinite(result)) {
            player.attackedAtYaw = result;
        }
    }
}