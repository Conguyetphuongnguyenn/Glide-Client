package me.eldodebug.soar.injection.mixin.mixins.chunk;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

@Mixin(Chunk.class)
public class MixinChunk {
    
    @Shadow
    @Final
    private ClassInheritanceMultiMap<Entity>[] entityLists;
    
    @Shadow
    @Final
    private World worldObj;
    
    @Unique
    private final List<EntityPlayer> reusablePlayerList = new ArrayList<>(10);
    
    @Inject(method = "onChunkUnload", at = @At("HEAD"))
    public void chunkUnloadFix(CallbackInfo ci) {
        reusablePlayerList.clear();
        
        if (entityLists == null) return;
        
        for (ClassInheritanceMultiMap<Entity> map : entityLists) {
            if (map == null) continue;
            
            for (EntityPlayer player : map.getByClass(EntityPlayer.class)) {
                if (player != null) {
                    reusablePlayerList.add(player);
                }
            }
        }
        
        for (EntityPlayer player : reusablePlayerList) {
            if (worldObj != null && player != null) {
                worldObj.updateEntityWithOptionalForce(player, false);
            }
        }
        
        reusablePlayerList.clear();
    }
    
    @ModifyArg(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;relightBlock(III)V", ordinal = 0), index = 1)
    private int subtractOneFromY(int y) {
        return y - 1;
    }

    @Overwrite
    public IBlockState getBlockState(BlockPos pos) {
        Chunk chunk = (Chunk)(Object)this;
        int y = pos.getY();

        if (y >= 0 && y >> 4 < chunk.getBlockStorageArray().length) {
            ExtendedBlockStorage storage = chunk.getBlockStorageArray()[y >> 4];
            if (storage != null) {
                return storage.get(pos.getX() & 15, y & 15, pos.getZ() & 15);
            }
        }

        return Blocks.air.getDefaultState();
    }
}