package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventLoadWorld;
import me.eldodebug.soar.management.event.impl.EventMotionUpdate;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import net.minecraft.block.Block;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;

public class KillEffectsMod extends Mod {

    // CACHED RESOURCES - Never recreate
    private static final ResourceLocation SOUND_THUNDER = new ResourceLocation("ambient.weather.thunder");
    private static final ResourceLocation SOUND_FIRE = new ResourceLocation("item.fireCharge.use");
    private static final ResourceLocation SOUND_TWINKLE = new ResourceLocation("fireworks.twinkle");
    private static final ResourceLocation SOUND_STONE = new ResourceLocation("dig.stone");
    private static final ResourceLocation SOUND_EXPLODE = new ResourceLocation("random.explode");
    private static final ResourceLocation SOUND_ORB = new ResourceLocation("random.orb");
    private static final ResourceLocation SOUND_FIREWORK = new ResourceLocation("fireworks.largeBlast");
    private static final ResourceLocation SOUND_PORTAL = new ResourceLocation("portal.portal");
    
    private static final int MAX_KILL_DISTANCE_SQ = 10000; // 100 blocks squared
    
    // PERFORMANCE: Cached Random instance
    private static final Random RANDOM = new Random();
    
    // Effect types enum for better performance
    private enum EffectType {
        LIGHTNING, FLAMES, CLOUD, BLOOD, 
        EXPLOSION, HEARTS, CRITICAL, NOTES, 
        PORTAL, SLIME, FIREWORK, ENDER, RANDOM
    }
    
    // State
    private EntityLivingBase target;
    private int entityID;
    
    // Settings
    private BooleanSetting soundSetting;
    private ComboSetting effectSetting;
    private NumberSetting multiplierSetting;
    
    // Cached
    private EffectType cachedEffectType;
    private long lastEffectCheck = 0;
    
    // RANDOM: Track all non-random effects
    private static final EffectType[] NON_RANDOM_EFFECTS = {
        EffectType.LIGHTNING, EffectType.FLAMES, EffectType.CLOUD, 
        EffectType.BLOOD, EffectType.EXPLOSION, EffectType.HEARTS, 
        EffectType.CRITICAL, EffectType.NOTES, EffectType.PORTAL, 
        EffectType.SLIME, EffectType.FIREWORK, EffectType.ENDER
    };
    
    public KillEffectsMod() {
        super(TranslateText.KILL_EFFECTS, TranslateText.KILL_EFFECTS_DESCRIPTION, ModCategory.RENDER);
    }
    
    @Override
    public void setup() {
        this.soundSetting = new BooleanSetting(TranslateText.SOUND, this, true);
        
        this.effectSetting = new ComboSetting(TranslateText.EFFECT, this, TranslateText.BLOOD, 
            new ArrayList<Option>(Arrays.asList(
                new Option(TranslateText.LIGHTING),      // Lightning
                new Option(TranslateText.FLAMES),        // Fire
                new Option(TranslateText.CLOUD),         // Clouds
                new Option(TranslateText.BLOOD),         // Blood (default)
                new Option(TranslateText.EXPLOSION),     // TNT explosion
                new Option(TranslateText.HEARTS),        // Love hearts
                new Option(TranslateText.CRITICAL),      // Crit particles
                new Option(TranslateText.NOTES),         // Music notes
                new Option(TranslateText.PORTAL),        // Portal particles
                new Option(TranslateText.SLIME),         // Slime particles
                new Option(TranslateText.FIREWORK),      // Firework
                new Option(TranslateText.ENDER),         // Ender particles
                new Option(TranslateText.RANDOM)         // Random effect
            ))
        );
        
        this.multiplierSetting = new NumberSetting(TranslateText.MULTIPLIER, this, 1, 1, 10, true);
    }
    
    @Override
    public void onEnable() {
        if (!Glide.getInstance().getRestrictedMod().checkAllowed(this)) {
            this.setToggled(false);
            return;
        }
        super.onEnable();
        Glide.getInstance().getEventManager().register(this);
        
        // Initialize
        entityID = -1000000; // Start from very negative to avoid conflicts
        target = null;
        updateCachedEffect();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        // CRITICAL: Cleanup
        Glide.getInstance().getEventManager().unregister(this);
        
        target = null;
        cachedEffectType = null;
        entityID = -1000000;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        // NULL CHECKS FIRST
        if (!this.isToggled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        // Target detection with NULL checks
        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
            if (mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
                EntityLivingBase newTarget = (EntityLivingBase) mc.objectMouseOver.entityHit;
                // Only update if different to avoid constant reassignment
                if (target != newTarget) {
                    target = newTarget;
                }
            }
        }
        
        // Cache effect type check (reduce overhead)
        long now = System.currentTimeMillis();
        if (now - lastEffectCheck > 1000) {
            updateCachedEffect();
            lastEffectCheck = now;
        }
    }
    
    private void updateCachedEffect() {
        if (effectSetting == null || effectSetting.getOption() == null) {
            cachedEffectType = EffectType.BLOOD;
            return;
        }
        
        TranslateText translate = effectSetting.getOption().getTranslate();
        
        // Map TranslateText to EffectType enum
        if (translate.equals(TranslateText.LIGHTING)) {
            cachedEffectType = EffectType.LIGHTNING;
        } else if (translate.equals(TranslateText.FLAMES)) {
            cachedEffectType = EffectType.FLAMES;
        } else if (translate.equals(TranslateText.CLOUD)) {
            cachedEffectType = EffectType.CLOUD;
        } else if (translate.equals(TranslateText.BLOOD)) {
            cachedEffectType = EffectType.BLOOD;
        } else if (translate.equals(TranslateText.EXPLOSION)) {
            cachedEffectType = EffectType.EXPLOSION;
        } else if (translate.equals(TranslateText.HEARTS)) {
            cachedEffectType = EffectType.HEARTS;
        } else if (translate.equals(TranslateText.CRITICAL)) {
            cachedEffectType = EffectType.CRITICAL;
        } else if (translate.equals(TranslateText.NOTES)) {
            cachedEffectType = EffectType.NOTES;
        } else if (translate.equals(TranslateText.PORTAL)) {
            cachedEffectType = EffectType.PORTAL;
        } else if (translate.equals(TranslateText.SLIME)) {
            cachedEffectType = EffectType.SLIME;
        } else if (translate.equals(TranslateText.FIREWORK)) {
            cachedEffectType = EffectType.FIREWORK;
        } else if (translate.equals(TranslateText.ENDER)) {
            cachedEffectType = EffectType.ENDER;
        } else if (translate.equals(TranslateText.RANDOM)) {
            cachedEffectType = EffectType.RANDOM;
        } else {
            cachedEffectType = EffectType.BLOOD; // Fallback
        }
    }
    
    @EventTarget
    public void onPreMotionUpdate(EventMotionUpdate event) {
        // NULL CHECKS
        if (!this.isToggled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (target == null) return;
        
        // FIX: Use target.posY instead of mc.thePlayer.posY
        double distanceSq = mc.thePlayer.getDistanceToEntity(target);
        distanceSq = distanceSq * distanceSq; // Square it
        
        // Check if target died (not in world but was close)
        if (!mc.theWorld.loadedEntityList.contains(target) && distanceSq < MAX_KILL_DISTANCE_SQ) {
            
            // Prevent spawn on first few ticks
            if (mc.thePlayer.ticksExisted > 3) {
                spawnEffect();
            }
            
            target = null;
        }
    }
    
    private void spawnEffect() {
        if (cachedEffectType == null) return;
        if (target == null) return;
        
        // RANDOM: Pick random effect
        EffectType effectToSpawn = cachedEffectType;
        if (cachedEffectType == EffectType.RANDOM) {
            effectToSpawn = NON_RANDOM_EFFECTS[RANDOM.nextInt(NON_RANDOM_EFFECTS.length)];
        }
        
        // Spawn based on type
        switch (effectToSpawn) {
            case LIGHTNING:
                spawnLightning();
                break;
            case FLAMES:
                spawnFlames();
                break;
            case CLOUD:
                spawnCloud();
                break;
            case BLOOD:
                spawnBlood();
                break;
            case EXPLOSION:
                spawnExplosion();
                break;
            case HEARTS:
                spawnHearts();
                break;
            case CRITICAL:
                spawnCritical();
                break;
            case NOTES:
                spawnNotes();
                break;
            case PORTAL:
                spawnPortal();
                break;
            case SLIME:
                spawnSlime();
                break;
            case FIREWORK:
                spawnFirework();
                break;
            case ENDER:
                spawnEnder();
                break;
        }
    }
    
    // ===== ORIGINAL EFFECTS =====
    
    private void spawnLightning() {
        try {
            if (mc.theWorld == null || target == null) return;
            
            EntityLightningBolt lightning = new EntityLightningBolt(
                mc.theWorld, 
                target.posX, 
                target.posY, 
                target.posZ
            );
            
            // Safe entity ID decrement
            if (entityID < -2000000) entityID = -1000000;
            mc.theWorld.addEntityToWorld(entityID--, lightning);
            
            if (soundSetting.isToggled()) {
                playSound(SOUND_THUNDER);
            }
        } catch (Exception e) {
            // Entity spawn failed - ignore
        }
    }
    
    private void spawnFlames() {
        spawnParticles(EnumParticleTypes.FLAME, SOUND_FIRE);
    }
    
    private void spawnCloud() {
        spawnParticles(EnumParticleTypes.CLOUD, SOUND_TWINKLE);
    }
    
    private void spawnBlood() {
        try {
            if (mc.theWorld == null || target == null) return;
            
            int count = multiplierSetting.getValueInt() * 50; // Multiplier support
            
            for (int i = 0; i < count; i++) {
                mc.theWorld.spawnParticle(
                    EnumParticleTypes.BLOCK_CRACK, 
                    target.posX, 
                    target.posY + target.height - 0.75, 
                    target.posZ, 
                    0, 0, 0, 
                    Block.getStateId(Blocks.redstone_block.getDefaultState())
                );
            }
            
            if (soundSetting.isToggled()) {
                playPositionedSound(SOUND_STONE, 4.0F, 1.2F);
            }
        } catch (Exception e) {
            // Particle spawn failed
        }
    }
    
    // ===== NEW EFFECTS =====
    
    private void spawnExplosion() {
        spawnParticles(EnumParticleTypes.EXPLOSION_LARGE, SOUND_EXPLODE);
    }
    
    private void spawnHearts() {
        spawnParticles(EnumParticleTypes.HEART, SOUND_ORB);
    }
    
    private void spawnCritical() {
        spawnParticles(EnumParticleTypes.CRIT, SOUND_ORB);
    }
    
    private void spawnNotes() {
        spawnParticles(EnumParticleTypes.NOTE, SOUND_ORB);
    }
    
    private void spawnPortal() {
        spawnParticles(EnumParticleTypes.PORTAL, SOUND_PORTAL);
    }
    
    private void spawnSlime() {
        spawnParticles(EnumParticleTypes.SLIME, SOUND_STONE);
    }
    
    private void spawnFirework() {
        spawnParticles(EnumParticleTypes.FIREWORKS_SPARK, SOUND_FIREWORK);
    }
    
    private void spawnEnder() {
        spawnParticles(EnumParticleTypes.PORTAL, SOUND_PORTAL);
    }
    
    // ===== HELPER METHODS =====
    
    private void spawnParticles(EnumParticleTypes particleType, ResourceLocation sound) {
        try {
            if (mc.effectRenderer == null || target == null) return;
            
            int count = multiplierSetting.getValueInt();
            for (int i = 0; i < count; i++) {
                mc.effectRenderer.emitParticleAtEntity(target, particleType);
            }
            
            if (soundSetting.isToggled()) {
                playSound(sound);
            }
        } catch (Exception e) {
            // Particle spawn failed
        }
    }
    
    private void playSound(ResourceLocation sound) {
        if (target == null || mc.getSoundHandler() == null) return;
        
        try {
            mc.getSoundHandler().playSound(
                PositionedSoundRecord.create(
                    sound, 
                    (float) target.posX, 
                    (float) target.posY, 
                    (float) target.posZ
                )
            );
        } catch (Exception e) {
            // Sound play failed
        }
    }
    
    private void playPositionedSound(ResourceLocation sound, float volume, float pitch) {
        if (target == null || mc.getSoundHandler() == null) return;
        
        try {
            mc.getSoundHandler().playSound(
                new PositionedSoundRecord(
                    sound, 
                    volume, 
                    pitch, 
                    (float) target.posX, 
                    (float) target.posY, 
                    (float) target.posZ
                )
            );
        } catch (Exception e) {
            // Sound play failed
        }
    }
    
    @EventTarget
    public void onLoadWorld(EventLoadWorld event) {
        entityID = -1000000;
        target = null;
    }
}