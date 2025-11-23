package me.eldodebug.soar.injection.mixin.mixins.gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.eldodebug.soar.hooks.GuiChatHook;
import me.eldodebug.soar.management.mods.impl.ChatTranslateMod;
import me.eldodebug.soar.utils.Multithreading;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

@Mixin(GuiChat.class)
public class MixinGuiChat extends GuiScreen {

    @Shadow
    protected GuiTextField inputField;

    @Inject(method = "drawScreen", at = @At("TAIL"))
    public void postDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ChatTranslateMod mod = ChatTranslateMod.getInstance();
        if(mod != null && mod.isToggled()) {
            GuiChatHook.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    public void preMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        ChatTranslateMod mod = ChatTranslateMod.getInstance();
        if(mod != null && mod.isToggled()) {
            GuiChatHook.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Redirect(method = "keyTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiChat;sendChatMessage(Ljava/lang/String;)V"))
    public void cancelSendMessage() {
        if(inputField == null) return;
        
        String s = this.inputField.getText();
        if(s == null) return;
        
        s = s.trim();
        
        if (s.length() > 0) {
            ChatTranslateMod mod = ChatTranslateMod.getInstance();
            
            if(mod != null && mod.isToggled() && GuiChatHook.isToggled()) {
                final String message = s;
                Multithreading.runAsync(() -> {
                    GuiChatHook.sendTranslatedMessage(message);
                });
            } else {
                this.sendChatMessage(s);
            }
        }
    }
}
