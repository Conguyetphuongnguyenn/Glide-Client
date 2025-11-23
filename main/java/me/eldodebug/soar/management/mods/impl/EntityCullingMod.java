package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;
import me.eldodebug.soar.injection.interfaces.IMixinRenderManager;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRenderTick;
import me.eldodebug.soar.management.event.impl.EventRendererLivingEntity;
import me.eldodebug.soar.management.event.impl.EventTick;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.AxisAlignedBB;

public class EntityCullingMod extends Mod {

    public static volatile boolean shouldPerformCulling = false;
    private final RenderManager renderManager = mc.getRenderManager();
    private final ConcurrentHashMap<UUID, OcclusionQuery> queries = new ConcurrentHashMap<>();
    private final boolean SUPPORT_NEW_GL = GLContext.getCapabilities().OpenGL33;
    private final AtomicInteger destroyTimer = new AtomicInteger(0);
    private final List<UUID> removeList = new ArrayList<>(64);
    private final Set<UUID> loadedSet = new HashSet<>(256);
    private NumberSetting delaySetting = new NumberSetting(TranslateText.DELAY, this, 2, 1, 3, true);
    private NumberSetting distanceSetting = new NumberSetting(TranslateText.DISTANCE, this, 45, 10, 150, true);
    
    public EntityCullingMod() {
        super(TranslateText.ENTITY_CULLING, TranslateText.ENTITY_CULLING_DESCRIPTIONN, ModCategory.OTHER);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        shouldPerformCulling = true;
        destroyTimer.set(0);
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        shouldPerformCulling = false;
        cleanupAllQueries();
        removeList.clear();
        loadedSet.clear();
    }

    @EventTarget
    public void onRendererLivingEntity(EventRendererLivingEntity event) {
        if (!this.isToggled() || !shouldPerformCulling) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        EntityLivingBase entity = (EntityLivingBase) event.getEntity();
        if (entity == null) return;
        
        boolean armorstand = entity instanceof EntityArmorStand;
        
        if (entity == mc.thePlayer || entity.worldObj != mc.thePlayer.worldObj || 
            (armorstand && ((EntityArmorStand) entity).hasMarker()) || 
            entity.isInvisibleToPlayer(mc.thePlayer)) {
            return;
        }

        if (checkEntity(entity)) {
            event.setCancelled(true);
            if (!canRenderName(entity)) return;

            double x = event.getX();
            double y = event.getY();
            double z = event.getZ();
            RendererLivingEntity<EntityLivingBase> renderer = (RendererLivingEntity<EntityLivingBase>) event.getRenderer();
            if (renderer != null) renderer.renderName(entity, x, y, z);
        }

        if ((entity instanceof EntityArmorStand) || (entity.isInvisible() && entity instanceof EntityPlayer)) {
            event.setCancelled(true);
        }

        final float entityDistance = entity.getDistanceToEntity(mc.thePlayer);
        if (entityDistance > distanceSetting.getValueFloat()) event.setCancelled(true);
    }

    @EventTarget
    public void onRenderTick(EventRenderTick event) {
        if (!this.isToggled()) return;
        mc.addScheduledTask(this::check);
    }
    
    @EventTarget
    public void onTick(EventTick event) {
        if (!this.isToggled()) return;
        if (destroyTimer.incrementAndGet() < 120) return;

        destroyTimer.set(0);
        WorldClient theWorld = mc.theWorld;
        if (theWorld == null || theWorld.loadedEntityList == null) return;

        removeList.clear();
        loadedSet.clear();
        
        for (Entity entity : theWorld.loadedEntityList) {
            if (entity != null) loadedSet.add(entity.getUniqueID());
        }

        try {
            for (OcclusionQuery value : queries.values()) {
                if (value == null) continue;
                if (loadedSet.contains(value.uuid)) continue;
                removeList.add(value.uuid);
                if (value.nextQuery != 0) {
                    try { GL15.glDeleteQueries(value.nextQuery); } catch (Exception e) {}
                }
            }
            for (UUID uuid : removeList) queries.remove(uuid);
        } catch (Exception e) {}
    }

    public boolean canRenderName(EntityLivingBase entity) {
        final EntityPlayerSP player = mc.thePlayer;
        if (player == null || entity == null) return false;
        
        if (entity instanceof EntityPlayer && entity != player) {
            final Team otherEntityTeam = entity.getTeam();
            final Team playerTeam = player.getTeam();
            if (otherEntityTeam != null) {
                final Team.EnumVisible teamVisibilityRule = otherEntityTeam.getNameTagVisibility();
                switch (teamVisibilityRule) {
                    case NEVER: return false;
                    case HIDE_FOR_OTHER_TEAMS: return playerTeam == null || otherEntityTeam.isSameTeam(playerTeam);
                    case HIDE_FOR_OWN_TEAM: return playerTeam == null || !otherEntityTeam.isSameTeam(playerTeam);
                    case ALWAYS: default: return true;
                }
            }
        }
        return Minecraft.isGuiEnabled() && renderManager != null && entity != renderManager.livingPlayer && 
               ((entity instanceof EntityArmorStand) || !entity.isInvisibleToPlayer(player)) && 
               entity.riddenByEntity == null;
    }
    
    public boolean renderItem(Entity stack) {
        if (stack == null || mc.thePlayer == null || mc.thePlayer.worldObj == null) return false;
        return shouldPerformCulling && stack.worldObj == mc.thePlayer.worldObj && checkEntity(stack);
    }
    
    private void check() {
        long delay = 0;
        switch (delaySetting.getValueInt() - 1) {
            case 0: delay = 10; break;
            case 1: delay = 25; break;
            case 2: delay = 50; break;
        }
        long nanoTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        try {
            for (OcclusionQuery query : queries.values()) {
                if (query == null) continue;
                if (query.nextQuery != 0) {
                    try {
                        final long queryObject = GL15.glGetQueryObjecti(query.nextQuery, GL15.GL_QUERY_RESULT_AVAILABLE);
                        if (queryObject != 0) {
                            query.occluded = GL15.glGetQueryObjecti(query.nextQuery, GL15.GL_QUERY_RESULT) == 0;
                            GL15.glDeleteQueries(query.nextQuery);
                            query.nextQuery = 0;
                        }
                    } catch (Exception e) { query.nextQuery = 0; }
                }
                if (query.nextQuery == 0 && nanoTime - query.executionTime > delay) {
                    query.executionTime = nanoTime;
                    query.refresh = true;
                }
            }
        } catch (Exception e) {}
    }

    private boolean checkEntity(Entity entity) {
        if (entity == null) return false;
        OcclusionQuery query = queries.computeIfAbsent(entity.getUniqueID(), OcclusionQuery::new);
        if (query == null) return false;
        
        if (query.refresh) {
            query.nextQuery = getQuery();
            query.refresh = false;
            if (renderManager == null) return false;
            try {
                int mode = SUPPORT_NEW_GL ? GL33.GL_ANY_SAMPLES_PASSED : GL15.GL_SAMPLES_PASSED;
                GL15.glBeginQuery(mode, query.nextQuery);
                IMixinRenderManager rm = (IMixinRenderManager) renderManager;
                drawSelectionBoundingBox(entity.getEntityBoundingBox().expand(.2, .2, .2).offset(-rm.getRenderPosX(), -rm.getRenderPosY(), -rm.getRenderPosZ()));
                GL15.glEndQuery(mode);
            } catch (Exception e) {
                if (query.nextQuery != 0) {
                    try { GL15.glDeleteQueries(query.nextQuery); } catch (Exception ignored) {}
                    query.nextQuery = 0;
                }
            }
        }
        return query.occluded;
    }

    public static void drawSelectionBoundingBox(AxisAlignedBB b) {
        if (b == null) return;
        try {
            GlStateManager.disableAlpha();
            GlStateManager.disableCull();
            GlStateManager.depthMask(false);
            GlStateManager.colorMask(false, false, false, false);
            final Tessellator tessellator = Tessellator.getInstance();
            final WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION);
            worldrenderer.pos(b.maxX, b.maxY, b.maxZ).endVertex();
            worldrenderer.pos(b.maxX, b.maxY, b.minZ).endVertex();
            worldrenderer.pos(b.minX, b.maxY, b.maxZ).endVertex();
            worldrenderer.pos(b.minX, b.maxY, b.minZ).endVertex();
            worldrenderer.pos(b.minX, b.minY, b.maxZ).endVertex();
            worldrenderer.pos(b.minX, b.minY, b.minZ).endVertex();
            worldrenderer.pos(b.minX, b.maxY, b.minZ).endVertex();
            worldrenderer.pos(b.minX, b.minY, b.minZ).endVertex();
            worldrenderer.pos(b.maxX, b.maxY, b.minZ).endVertex();
            worldrenderer.pos(b.maxX, b.minY, b.minZ).endVertex();
            worldrenderer.pos(b.maxX, b.maxY, b.maxZ).endVertex();
            worldrenderer.pos(b.maxX, b.minY, b.maxZ).endVertex();
            worldrenderer.pos(b.minX, b.maxY, b.maxZ).endVertex();
            worldrenderer.pos(b.minX, b.minY, b.maxZ).endVertex();
            worldrenderer.pos(b.minX, b.minY, b.maxZ).endVertex();
            worldrenderer.pos(b.maxX, b.minY, b.maxZ).endVertex();
            worldrenderer.pos(b.minX, b.minY, b.minZ).endVertex();
            worldrenderer.pos(b.maxX, b.minY, b.minZ).endVertex();
            tessellator.draw();
            GlStateManager.depthMask(true);
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.enableAlpha();
        } catch (Exception e) {
            try {
                GlStateManager.depthMask(true);
                GlStateManager.colorMask(true, true, true, true);
                GlStateManager.enableAlpha();
            } catch (Exception ignored) {}
        }
    }

    private static int getQuery() {
        try { return GL15.glGenQueries(); } catch (Throwable throwable) { return 0; }
    }
    
    private void cleanupAllQueries() {
        try {
            for (OcclusionQuery query : queries.values()) {
                if (query != null && query.nextQuery != 0) {
                    try { GL15.glDeleteQueries(query.nextQuery); } catch (Exception ignored) {}
                }
            }
            queries.clear();
        } catch (Exception e) { queries.clear(); }
    }
    
    private class OcclusionQuery {
        public final UUID uuid;
        public int nextQuery;
        public boolean refresh = true;
        public boolean occluded;
        public long executionTime = 0;
        private OcclusionQuery(UUID uuid) { this.uuid = uuid; }
    }
}