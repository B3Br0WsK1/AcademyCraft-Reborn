package cn.academy.client.render.entity;

import cn.academy.ACEntities;
import cn.academy.entity.*;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.render.legacy.LegacyRender;
import cn.lambdalib2.render.legacy.LegacyRenderAdapter;
import cn.lambdalib2.render.legacy.RenderManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

/**
 * Client side entity renderer registration of {@link ACEntities}.
 */
@Environment(EnvType.CLIENT)
public final class ACEntityRenderers {

    private ACEntityRenderers() {}

    @StateEventCallback
    private static void initClient(StateEvents.Init ev) {
        RenderManager rm = RenderManager.INSTANCE;
        EntityRendererRegistry.register(ACEntities.ENTITY_BLOCK, cn.academy.client.render.entity.RenderEntityBlock::new);
        EntityRendererRegistry.register(ACEntities.MAG_MANIP_BLOCK, cn.academy.client.render.entity.RenderEntityBlock::new);
        reg(ACEntities.COIN_THROWING, () -> new cn.academy.client.render.entity.RendererCoinThrowing(rm));
        reg(ACEntities.MAG_HOOK, () -> new cn.academy.client.render.entity.RendererMagHook(rm));
        reg(ACEntities.MD_BALL, () -> new EntityMdBall.R(rm));
        reg(ACEntities.SILBARN, () -> new EntitySilbarn.RenderSibarn(rm));

        reg(ACEntities.ARC, () -> new EntityArc.Renderer(rm));
        reg(ACEntities.BARRAGE_RAY_PRE, () -> new EntityBarrageRayPre.BRPRender(rm));
        reg(ACEntities.BLOOD_SPLASH, () -> new EntityBloodSplash.SplashRenderer(rm));
        reg(ACEntities.DIAMOND_SHIELD, () -> new cn.academy.client.render.entity.RenderDiamondShield(rm));
        reg(ACEntities.INTENSIFY_EFFECT, () -> new EntitySurroundArc.Renderer(rm));
        reg(ACEntities.MARKER, () -> new cn.academy.client.render.entity.RenderMarker(rm));
        reg(ACEntities.MD_RAY, () -> new EntityMDRay.MDRayRender(rm));
        reg(ACEntities.MD_RAY_BARRAGE, () -> new EntityMdRayBarrage.BarrageRenderer(rm));
        reg(ACEntities.MD_RAY_SMALL, () -> new EntityMdRaySmall.SmallMdRayRender(rm));
        reg(ACEntities.MD_SHIELD, () -> new cn.academy.client.render.entity.RenderMdShield(rm));
        reg(ACEntities.MINE_RAY_BASIC, () -> new EntityMineRayBasic.BasicMineRayRender(rm));
        reg(ACEntities.MINE_RAY_EXPERT, () -> new EntityMineRayExpert.ExpertRayRenderer(rm));
        reg(ACEntities.MINE_RAY_LUCK, () -> new EntityMineRayLuck.LuckRayRender(rm));
        reg(ACEntities.RAILGUN_FX, () -> new EntityRailgunFX.RailgunRender(rm));
        reg(ACEntities.RIPPLE_MARK, () -> new cn.academy.client.render.entity.RippleMarkRender(rm));
        reg(ACEntities.SURROUND_ARC, () -> new EntitySurroundArc.Renderer(rm));
        reg(ACEntities.TP_MARKING, () -> new cn.academy.client.render.entity.MarkRender(rm));
        reg(ACEntities.LOCAL_EFFECT, () -> new cn.academy.client.render.entity.LocalEffectRender());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Entity> void reg(EntityType<T> type, Supplier<? extends LegacyRender> render) {
        EntityRendererRegistry.register(type, ctx -> new LegacyRenderAdapter<T>(ctx, render.get()));
    }

}
