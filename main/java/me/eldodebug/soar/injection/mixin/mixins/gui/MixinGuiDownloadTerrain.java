package me.eldodebug.soar.injection.mixin.mixins.gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import me.eldodebug.soar.management.event.impl.EventLoadWorld;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiScreen;

@Mixin(GuiDownloadTerrain.class)
public class MixinGuiDownloadTerrain extends GuiScreen {

    private static final EventLoadWorld CACHED_EVENT_LOAD_WORLD = new EventLoadWorld();

    @Overwrite
    public void initGui() {
        this.buttonList.clear();
        CACHED_EVENT_LOAD_WORLD.call();
    }
}
