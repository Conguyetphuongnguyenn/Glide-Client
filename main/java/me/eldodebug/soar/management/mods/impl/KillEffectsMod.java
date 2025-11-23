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

    private static final ResourceLocation SOUND_THUNDER = new ResourceLocation("ambient.weather.thunder");
    private static final ResourceLocation SOUND_FIRE = new ResourceLocation("item.fireCharge.use");
    private static final ResourceLocation SOUND_TWINKLE = new ResourceLocation("fireworks.twinkle");
    private static final ResourceLocation SOUND_STONE = new ResourceLocation("dig.stone");
    private static final ResourceLocation SOUND_EXPLODE = new ResourceLocation("random.explode");
    private static final ResourceLocation SOUND_ORB = new ResourceLocation("random.orb");
    private static final ResourceLocation SOUND_FIREWORK = new ResourceLocation("fireworks.largeBlast");
    private static final ResourceLocation SOUND_PORTAL = new ResourceLocation("portal.portal");
    private static final int MAX_KILL_DISTANCE_SQ = 10000;
    private static final Random RANDOM = new Random();
    
    private enum EffectType {
        LIGHTNING, FLAMES, CLOUD, BLOOD, EXPLOSION, HEARTS, CRITICAL, NOTES, PORTAL, SLIME, FIREWORK, ENDER, RANDOM
    }
    
    private EntityLivingBase target;
    private int entityID;
    private BooleanSetting soundSetting;
    private ComboSetting effectSetting;
    private NumberSetting multiplierSetting;
    private EffectType cachedEffectType;
    private long lastEffectCheck = 0;
    
    private static final EffectType[] NON_RANDOM_EFFECTS = {
        EffectType.LIGHTNING, EffectType.FLAMES, EffectType.CLOUD, EffectType.BLOOD, EffectType.EXPLOSION, 
        EffectType.HEARTS, EffectType.CRITICAL, EffectType.NOTES, EffectType.PORTAL, EffectType.SLIME, 
        EffectType.FIREWORK, EffectType.ENDER
    };
    
    public KillEffectsMod() {
        super(TranslateText.KILL_EFFECTS, TranslateText.KILL_EFFECTS_DESCRIPTION, ModCategory.RENDER);
    }
    
    @Override
    public void setup() {
        this.soundSetting = new BooleanSetting(TranslateText.SOUND, this, true);
        this.effectSetting = new ComboSetting(TranslateText.EFFECT, this, TranslateText.BLOOD, 
            new ArrayList<Option>(Arrays.asList(
                new Option(TranslateText.LIGHTING), new Option(TranslateText.FLAMES), new Option(TranslateText.CLOUD),
                new Option(TranslateText.BLOOD), new Option(TranslateText.EXPLOSION), new Option(TranslateText.HEARTS),
                new Option(TranslateText.CRITICAL), new Option(TranslateText.NOTES), new Option(TranslateText.PORTAL),
                new Option(TranslateText.SLIME), new Option(TranslateText.FIREWORK), new Option(TranslateText.ENDER),
                new Option(TranslateText.RANDOM)
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
        entityID = -1000000;
        target = null;
        updateCachedEffect();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        Glide.getInstance().getEventManager().unregister(this);
        target = null;
        cachedEffectType = null;
        entityID = -1000000;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!this.isToggled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
            if (mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
                EntityLivingBase newTarget = (EntityLivingBase) mc.objectMouseOver.entityHit;
                if (target != newTarget) target = newTarget;
            }
        }
        
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
        if (translate.equals(TranslateText.LIGHTING)) cachedEffectType = EffectType.LIGHTNING;
        else if (translate.equals(TranslateText.FLAMES)) cachedEffectType = EffectType.FLAMES;
        else if (translate.equals(TranslateText.CLOUD)) cachedEffectType = EffectType.CLOUD;
        else if (translate.equals(TranslateText.BLOOD)) cachedEffectType = EffectType.BLOOD;
        else if (translate.equals(TranslateText.EXPLOSION)) cachedEffectType = EffectType.EXPLOSION;
        else if (translate.equals(TranslateText.HEARTS)) cachedEffectType = EffectType.HEARTS;
        else if (translate.equals(TranslateText.CRITICAL)) cachedEffectType = EffectType.CRITICAL;
        else if (translate.equals(TranslateText.NOTES)) cachedEffectType = EffectType.NOTES;
        else if (translate.equals(TranslateText.PORTAL)) cachedEffectType = EffectType.PORTAL;
        else if (translate.equals(TranslateText.SLIME)) cachedEffectType = EffectType.SLIME;
        else if (translate.equals(TranslateText.FIREWORK)) cachedEffectType = EffectType.FIREWORK;
        else if (translate.equals(TranslateText.ENDER)) cachedEffectType = EffectType.ENDER;
        else if (translate.equals(TranslateText.RANDOM)) cachedEffectType = EffectType.RANDOM;
        else cachedEffectType = EffectType.BLOOD;
    }
    
    @EventTarget
    public void onPreMotionUpdate(EventMotionUpdate event) {
        if (!this.isToggled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (target == null) return;
        
        double distanceSq = mc.thePlayer.getDistanceToEntity(target);
        distanceSq = distanceSq * distanceSq;
        
        if (!mc.theWorld.loadedEntityList.contains(target) && distanceSq < MAX_KILL_DISTANCE_SQ) {
            if (mc.thePlayer.ticksExisted > 3) spawnEffect();
            target = null;
        }
    }
    
    private void spawnEffect() {
        if (cachedEffectType == null) return;
        if (target == null) return;
        
        EffectType effectToSpawn = cachedEffectType;
        if (cachedEffectType == EffectType.RANDOM) {
            effectToSpawn = NON_RANDOM_EFFECTS[RANDOM.nextInt(NON_RANDOM_EFFECTS.length)];
        }
        
        switch (effectToSpawn) {
            case LIGHTNING: spawnLightning(); break;
            case FLAMES: spawnFlames(); break;
            case CLOUD: spawnCloud(); break;
            case BLOOD: spawnBlood(); break;
            case EXPLOSION: spawnExplosion(); break;
            case HEARTS: spawnHearts(); break;
            case CRITICAL: spawnCritical(); break;
            case NOTES: spawnNotes(); break;
            case PORTAL: spawnPortal(); break;
            case SLIME: spawnSlime(); break;
            case FIREWORK: spawnFirework(); break;
            case ENDER: spawnEnder(); break;
            default: spawnBlood(); break;
        }
    }
    
    private void spawnLightning() {
        try {
            if (mc.theWorld == null || target == null) return;
            EntityLightningBolt lightning = new EntityLightningBolt(mc.theWorld, target.posX, target.posY, target.posZ);
            if (entityID < -2000000) entityID = -1000000;
            mc.theWorld.addEntityToWorld(entityID--, lightning);
            if (soundSetting.isToggled()) playSound(SOUND_THUNDER);
        } catch (Exception e) {}
    }
    
    private void spawnFlames() { spawnParticles(EnumParticleTypes.FLAME, SOUND_FIRE); }
    private void spawnCloud() { spawnParticles(EnumParticleTypes.CLOUD, SOUND_TWINKLE); }
    
    private void spawnBlood() {
        try {
            if (mc.theWorld == null || target == null) return;
            int count = multiplierSetting.getValueInt() * 50;
            for (int i = 0; i < count; i++) {
                mc.theWorld.spawnParticle(EnumParticleTypes.BLOCK_CRACK, target.posX, target.posY + target.height - 0.75, target.posZ, 0, 0, 0, Block.getStateId(Blocks.redstone_block.getDefaultState()));
            }
            if (soundSetting.isToggled()) playPositionedSound(SOUND_STONE, 4.0F, 1.2F);
        } catch (Exception e) {}
    }
    
    private void spawnExplosion() { spawnParticles(EnumParticleTypes.EXPLOSION_LARGE, SOUND_EXPLODE); }
    private void spawnHearts() { spawnParticles(EnumParticleTypes.HEART, SOUND_ORB); }
    private void spawnCritical() { spawnParticles(EnumParticleTypes.CRIT, SOUND_ORB); }
    private void spawnNotes() { spawnParticles(EnumParticleTypes.NOTE, SOUND_ORB); }
    private void spawnPortal() { spawnParticles(EnumParticleTypes.PORTAL, SOUND_PORTAL); }
    private void spawnSlime() { spawnParticles(EnumParticleTypes.SLIME, SOUND_STONE); }
    private void spawnFirework() { spawnParticles(EnumParticleTypes.FIREWORKS_SPARK, SOUND_FIREWORK); }
    private void spawnEnder() { spawnParticles(EnumParticleTypes.PORTAL, SOUND_PORTAL); }
    
    private void spawnParticles(EnumParticleTypes particleType, ResourceLocation sound) {
        try {
            if (mc.effectRenderer == null || target == null) return;
            int count = multiplierSetting.getValueInt();
            for (int i = 0; i < count; i++) mc.effectRenderer.emitParticleAtEntity(target, particleType);
            if (soundSetting.isToggled()) playSound(sound);
        } catch (Exception e) {}
    }
    
    private void playSound(ResourceLocation sound) {
        if (target == null || mc.getSoundHandler() == null) return;
        try {
            mc.getSoundHandler().playSound(PositionedSoundRecord.create(sound, (float) target.posX, (float) target.posY, (float) target.posZ));
        } catch (Exception e) {}
    }
    
    private void playPositionedSound(ResourceLocation sound, float volume, float pitch) {
        if (target == null || mc.getSoundHandler() == null) return;
        try {
            mc.getSoundHandler().playSound(new PositionedSoundRecord(sound, volume, pitch, (float) target.posX, (float) target.posY, (float) target.posZ));
        } catch (Exception e) {}
    }
    
    @EventTarget
    public void onLoadWorld(EventLoadWorld event) {
        entityID = -1000000;
        target = null;
    }
}