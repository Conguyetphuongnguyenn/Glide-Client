package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayList;
import java.util.Random;
import java.util.Iterator;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.impl.projectiletrail.ProjectileTrailType;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.Vec3;

public class ProjectileTrailMod extends Mod {

    private static final Random RANDOM = new Random();
    private ProjectileTrailType type;
    private ArrayList<EntityThrowable> throwables = new ArrayList<>();
    private int ticks;
    
    private ComboSetting mode = new ComboSetting(TranslateText.TYPE, this, TranslateText.HEARTS, new ArrayList<Option>() {
        private static final long serialVersionUID = 1L;
        {
            for(ProjectileTrailType t : ProjectileTrailType.values()) {
                add(new Option(t.getNameTranslate()));
            }
        }
    });
    
    public ProjectileTrailMod() {
        super(TranslateText.PROJECTILE_TRAIL, TranslateText.PROJECTILE_TRAIL_DESCRIPTION, ModCategory.PLAYER);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;
        if (mode == null || mode.getOption() == null) return;
        
        type = ProjectileTrailType.getTypeByKey(mode.getOption().getNameKey());
        ticks = ticks >= 20 ? 0 : ticks + 2;
        
        updateThrowables();
        
        for (Entity entity : mc.theWorld.getLoadedEntityList()) {
            if (entity != null && (isValidEntity(entity) || throwables.contains(entity)) 
                && entity.getDistanceToEntity(mc.thePlayer) > 3.0F) {
                spawnParticle(type, entity.getPositionVector());
            }
        }
    }
    
    public void spawnParticle(ProjectileTrailType trail, Vec3 vector) {
        if (trail == null || vector == null) return;
        if (mc == null || mc.theWorld == null) return;
        
        if (trail != ProjectileTrailType.GREEN_STAR && trail != ProjectileTrailType.HEARTS || ticks % 4 == 0) {
            if (trail != ProjectileTrailType.MUSIC_NOTES || ticks % 2 == 0) {
                float translate = trail.translate;
                float velocity = trail.velocity;
                
                for (int i = 0; i < trail.count; ++i) {
                    float x = RANDOM.nextFloat() * translate * 2.0F - translate;
                    float y = RANDOM.nextFloat() * translate * 2.0F - translate;
                    float z = RANDOM.nextFloat() * translate * 2.0F - translate;
                    float xVel = RANDOM.nextFloat() * velocity * 2.0F - velocity;
                    float yVel = RANDOM.nextFloat() * velocity * 2.0F - velocity;
                    float zVel = RANDOM.nextFloat() * velocity * 2.0F - velocity;
                    
                    mc.theWorld.spawnParticle(trail.particle, true, x + vector.xCoord, y + vector.yCoord, z + vector.zCoord, xVel, yVel, zVel, new int[0]);
                }
            }
        }
    }
    
    public boolean isValidEntity(Entity entity) {
        if (entity.posX == entity.prevPosX && entity.posY == entity.prevPosY && entity.posZ == entity.prevPosZ) return false;
        
        if (entity instanceof EntityArrow) {
            return ((EntityArrow) entity).shootingEntity != null && ((EntityArrow) entity).shootingEntity.equals(mc.thePlayer);
        } else if (entity instanceof EntityFishHook) {
            return ((EntityFishHook) entity).angler != null && ((EntityFishHook) entity).angler.equals(mc.thePlayer);
        } else if (entity instanceof EntityThrowable && entity.ticksExisted == 1 && entity.getDistanceSqToEntity(mc.thePlayer) <= 11.0D && !throwables.contains(entity)) {
            throwables.add((EntityThrowable) entity);
            return true;
        }
        return false;
    }
    
    public void updateThrowables() {
        throwables.removeIf(throwable -> throwable == null || throwable.isDead);
    }
}