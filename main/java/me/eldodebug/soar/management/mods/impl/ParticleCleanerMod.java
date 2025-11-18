package me.eldodebug.soar.management.mods.impl;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import net.minecraft.client.particle.EntityFX;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

public class ParticleCleanerMod extends Mod {
    
    private static ParticleCleanerMod instance;
    
    private BooleanSetting distanceCleanSetting;
    private NumberSetting distanceSetting;
    private BooleanSetting limitSetting;
    private NumberSetting maxParticlesSetting;
    
    private Field fxLayersField;
    private boolean reflectionReady = false;
    
    public ParticleCleanerMod() {
        super(TranslateText.PARTICLE_CLEANER, TranslateText.PARTICLE_CLEANER_DESCRIPTION, ModCategory.RENDER);
        instance = this;
    }
    
    @Override
    public void setup() {
        distanceCleanSetting = new BooleanSetting(TranslateText.DISTANCE_CLEAN, this, true);
        distanceSetting = new NumberSetting(TranslateText.DISTANCE, this, 1.5, 0.1, 3.0, false);
        
        limitSetting = new BooleanSetting(TranslateText.LIMIT_PARTICLES, this, true);
        maxParticlesSetting = new NumberSetting(TranslateText.MAX_PARTICLES, this, 30, 10, 60, true);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        if (!reflectionReady) {
            tryInitReflection();
        }
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
    }
    
    private void tryInitReflection() {
        if (mc == null || mc.effectRenderer == null) {
            return;
        }
        
        try {
            Class<?> clazz = mc.effectRenderer.getClass();
            
            try {
                fxLayersField = clazz.getDeclaredField("fxLayers");
            } catch (NoSuchFieldException e) {
                try {
                    fxLayersField = clazz.getDeclaredField("field_78876_b");
                } catch (NoSuchFieldException e2) {
                    for (Field f : clazz.getDeclaredFields()) {
                        if (f.getType().isArray()) {
                            Class<?> comp = f.getType().getComponentType();
                            if (comp != null && comp.isArray()) {
                                fxLayersField = f;
                                break;
                            }
                        }
                    }
                }
            }
            
            if (fxLayersField != null) {
                fxLayersField.setAccessible(true);
                reflectionReady = true;
            }
            
        } catch (Exception e) {
            reflectionReady = false;
        }
    }
    
    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!this.isToggled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.effectRenderer == null) return;
        
        if (!reflectionReady) {
            tryInitReflection();
            if (!reflectionReady) return;
        }
        
        cleanParticles();
    }
    
    @SuppressWarnings("unchecked")
    private void cleanParticles() {
        if (!distanceCleanSetting.isToggled() && !limitSetting.isToggled()) {
            return;
        }
        
        if (mc.thePlayer == null) return;
        
        final double px = mc.thePlayer.posX;
        final double py = mc.thePlayer.posY;
        final double pz = mc.thePlayer.posZ;
        
        final double maxDist = distanceSetting.getValue();
        final double maxDistSq = maxDist * maxDist;
        final int maxCount = maxParticlesSetting.getValueInt();
        
        final boolean doDistance = distanceCleanSetting.isToggled();
        final boolean doLimit = limitSetting.isToggled();
        
        try {
            Object obj = fxLayersField.get(mc.effectRenderer);
            if (obj == null) return;
            
            List<EntityFX>[][] layers = (List<EntityFX>[][]) obj;
            if (layers == null || layers.length == 0) return;
            
            if (doDistance) {
                cleanByDistance(layers, px, py, pz, maxDistSq);
            }
            
            if (doLimit) {
                enforceLimit(layers, maxCount);
            }
            
        } catch (Exception e) {
            reflectionReady = false;
        }
    }
    
    private void cleanByDistance(List<EntityFX>[][] layers, double px, double py, double pz, double maxDistSq) {
        for (int i = 0; i < layers.length; i++) {
            if (layers[i] == null) continue;
            
            for (int j = 0; j < layers[i].length; j++) {
                List<EntityFX> list = layers[i][j];
                if (list == null || list.isEmpty()) continue;
                
                Iterator<EntityFX> it = list.iterator();
                while (it.hasNext()) {
                    EntityFX fx = it.next();
                    if (fx == null) {
                        it.remove();
                        continue;
                    }
                    
                    double dx = fx.posX - px;
                    double dy = fx.posY - py;
                    double dz = fx.posZ - pz;
                    double distSq = dx * dx + dy * dy + dz * dz;
                    
                    if (distSq > maxDistSq) {
                        it.remove();
                    }
                }
            }
        }
    }
    
    private void enforceLimit(List<EntityFX>[][] layers, int max) {
        int total = 0;
        for (int i = 0; i < layers.length; i++) {
            if (layers[i] == null) continue;
            for (int j = 0; j < layers[i].length; j++) {
                List<EntityFX> list = layers[i][j];
                if (list != null) {
                    total += list.size();
                }
            }
        }
        
        if (total <= max) return;
        
        int toRemove = total - max;
        
        outer:
        for (int i = 0; i < layers.length; i++) {
            if (layers[i] == null) continue;
            
            for (int j = 0; j < layers[i].length; j++) {
                List<EntityFX> list = layers[i][j];
                if (list == null || list.isEmpty()) continue;
                
                while (toRemove > 0 && !list.isEmpty()) {
                    list.remove(0);
                    toRemove--;
                    
                    if (toRemove == 0) break outer;
                }
            }
        }
    }
    
    public static ParticleCleanerMod getInstance() {
        return instance;
    }
}