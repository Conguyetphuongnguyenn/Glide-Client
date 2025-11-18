package me.eldodebug.soar.injection.mixin.mixins.block;

import org.spongepowered.asm.mixin.Mixin;

import me.eldodebug.soar.management.mods.impl.ClearGlassMod;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;

@Mixin(BlockGlass.class)
public class MixinBlockGlass extends Block {

    protected MixinBlockGlass(Material materialIn) {
        super(materialIn);
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        ClearGlassMod mod = ClearGlassMod.getInstance();
        
        if (mod == null || !mod.isToggled()) {
            return super.shouldSideBeRendered(worldIn, pos, side);
        }
        
        BooleanSetting normalSetting = mod.getNormalSetting();
        if (normalSetting != null && normalSetting.isToggled()) {
            return false;
        }
        
        return super.shouldSideBeRendered(worldIn, pos, side);
    }
}