package me.eldodebug.soar.management.mods.impl;

import java.io.File;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventMotionUpdate;
import me.eldodebug.soar.management.event.impl.EventTick;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.SoundSetting;
import me.eldodebug.soar.utils.Sound;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;

public class KillSoundsMod extends Mod {

    private static final ResourceLocation OOF_SOUND = new ResourceLocation("soar/audio/oof.wav");
    private static final int MAX_KILL_DISTANCE_SQ = 10000;
    private static final int MIN_TICKS_ALIVE = 3;
    private static final int VOLUME_UPDATE_INTERVAL = 4;
    
    private EntityLivingBase target;
    private File prevCustomSound;
    private Sound oofSound;
    private Sound customSound;
    private volatile boolean oofSoundReady = false;
    private volatile boolean customSoundReady = false;
    private float cachedVolume = 0.5F;
    private boolean volumeNeedsUpdate = true;
    private int tickCounter = 0;
    
    private NumberSetting volumeSetting;
    private BooleanSetting customSoundSetting;
    private SoundSetting soundSetting;
    
    public KillSoundsMod() {
        super(TranslateText.KILL_SOUNDS, TranslateText.KILL_SOUNDS_DESCRIPTION, ModCategory.OTHER);
    }
    
    @Override
    public void setup() {
        this.volumeSetting = new NumberSetting(TranslateText.VOLUME, this, 0.5, 0.0, 1.0, false);
        this.customSoundSetting = new BooleanSetting(TranslateText.CUSTOM_SOUND, this, false);
        this.soundSetting = new SoundSetting(TranslateText.SOUND, this);
    }
    
    @Override
    public void onEnable() {
        if (!Glide.getInstance().getRestrictedMod().checkAllowed(this)) {
            this.setToggled(false);
            return;
        }
        
        super.onEnable();
        Glide.getInstance().getEventManager().register(this);
        
        target = null;
        prevCustomSound = null;
        tickCounter = 0;
        volumeNeedsUpdate = true;
        
        initializeSounds();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        Glide.getInstance().getEventManager().unregister(this);
        
        target = null;
        prevCustomSound = null;
        tickCounter = 0;
        oofSoundReady = false;
        customSoundReady = false;
        oofSound = null;
        customSound = null;
    }
    
    private void initializeSounds() {
        if (oofSound == null) {
            oofSound = new Sound();
        }
        
        try {
            oofSound.loadClip(OOF_SOUND);
            oofSoundReady = true;
        } catch (Exception e) {
            oofSoundReady = false;
        }
        
        if (customSound == null) {
            customSound = new Sound();
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (!this.isToggled()) return;
        
        if (++tickCounter < VOLUME_UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;
        
        updateVolume();
        
        if (customSoundSetting.isToggled()) {
            loadCustomSoundIfNeeded();
        }
    }
    
    private void updateVolume() {
        float newVolume = volumeSetting.getValueFloat();
        
        if (Math.abs(newVolume - cachedVolume) > 0.001F || volumeNeedsUpdate) {
            cachedVolume = newVolume;
            volumeNeedsUpdate = false;
            
            try {
                if (customSoundSetting.isToggled() && customSoundReady) {
                    customSound.setVolume(cachedVolume);
                } else if (oofSoundReady) {
                    oofSound.setVolume(cachedVolume);
                }
            } catch (Exception e) {}
        }
    }
    
    private void loadCustomSoundIfNeeded() {
        File currentSoundFile = soundSetting.getSound();
        
        if (currentSoundFile != null && !currentSoundFile.equals(prevCustomSound)) {
            prevCustomSound = currentSoundFile;
            customSoundReady = false;
            
            try {
                customSound.loadClip(currentSoundFile);
                customSound.setVolume(cachedVolume);
                customSoundReady = true;
            } catch (Exception e) {
                customSoundReady = false;
            }
        }
    }
    
    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!this.isToggled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        MovingObjectPosition mouseOver = mc.objectMouseOver;
        
        if (mouseOver != null && mouseOver.entityHit != null) {
            if (mouseOver.entityHit instanceof EntityLivingBase) {
                target = (EntityLivingBase) mouseOver.entityHit;
            }
        }
    }
    
    @EventTarget
    public void onPreMotionUpdate(EventMotionUpdate event) {
        if (!this.isToggled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (target == null) return;
        
        if (!mc.theWorld.loadedEntityList.contains(target)) {
            double distanceSq = mc.thePlayer.getDistanceSq(target.posX, mc.thePlayer.posY, target.posZ);
            
            if (distanceSq < MAX_KILL_DISTANCE_SQ && mc.thePlayer.ticksExisted > MIN_TICKS_ALIVE) {
                playKillSound();
            }
            
            target = null;
        }
    }
    
    private void playKillSound() {
        try {
            if (customSoundSetting.isToggled() && customSoundReady && customSound != null) {
                customSound.play();
            } else if (oofSoundReady && oofSound != null) {
                oofSound.play();
            }
        } catch (Exception e) {}
    }
}