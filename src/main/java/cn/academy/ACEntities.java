package cn.academy;

import cn.academy.entity.*;
import cn.lambdalib2.LLEntities;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;


/**
 * Entity types of AcademyCraft. Most entities are client-local effects, which are never spawned on the server; their
 *  classes are client only, so their types are created with a null factory.
 */
public class ACEntities {

    // Server spawned entities
    public static final EntityType<EntityBlock> ENTITY_BLOCK = synced("entity_block", EntityBlock::new, 0.98f, 0.98f, 4, 3);
    public static final EntityType<MagManipEntityBlock> MAG_MANIP_BLOCK = synced("mag_manip_block", MagManipEntityBlock::new, 0.98f, 0.98f, 4, 10);
    public static final EntityType<EntityCoinThrowing> COIN_THROWING = synced("coin_throwing", EntityCoinThrowing::new, 0.2f, 0.2f, 4, 3);
    public static final EntityType<EntityMagHook> MAG_HOOK = synced("mag_hook", EntityMagHook::new, 0.5f, 0.5f, 4, 3);
    public static final EntityType<EntityMdBall> MD_BALL = synced("md_ball", EntityMdBall::new, 0.25f, 0.25f, 4, 3);
    public static final EntityType<EntitySilbarn> SILBARN = synced("silbarn", EntitySilbarn::new, 0.4f, 0.4f, 4, 3);

    // Client local entities
    public static final EntityType<EntityArc> ARC = local("arc");
    public static final EntityType<EntityBarrageRayPre> BARRAGE_RAY_PRE = local("barrage_ray_pre");
    public static final EntityType<EntityBloodSplash> BLOOD_SPLASH = local("blood_splash");
    public static final EntityType<EntityDiamondShield> DIAMOND_SHIELD = local("diamond_shield");
    public static final EntityType<EntityIntensifyEffect> INTENSIFY_EFFECT = local("intensify_effect");
    public static final EntityType<EntityMarker> MARKER = local("marker");
    public static final EntityType<EntityMDRay> MD_RAY = local("md_ray");
    public static final EntityType<EntityMdRayBarrage> MD_RAY_BARRAGE = local("md_ray_barrage");
    public static final EntityType<EntityMdRaySmall> MD_RAY_SMALL = local("md_ray_small");
    public static final EntityType<EntityMdShield> MD_SHIELD = local("md_shield");
    public static final EntityType<EntityMineRayBasic> MINE_RAY_BASIC = local("mine_ray_basic");
    public static final EntityType<EntityMineRayExpert> MINE_RAY_EXPERT = local("mine_ray_expert");
    public static final EntityType<EntityMineRayLuck> MINE_RAY_LUCK = local("mine_ray_luck");
    public static final EntityType<EntityRailgunFX> RAILGUN_FX = local("railgun_fx");
    public static final EntityType<EntityRippleMark> RIPPLE_MARK = local("ripple_mark");
    public static final EntityType<EntitySurroundArc> SURROUND_ARC = local("surround_arc");
    public static final EntityType<EntityTPMarking> TP_MARKING = local("tp_marking");
    /** Generic type for the various local effect entities of the skills. */
    public static final EntityType<Entity> LOCAL_EFFECT = local("local_effect");

    private static <T extends Entity> EntityType<T> synced(String name, EntityType.EntityFactory<T> factory,
                                                           float w, float h, int range, int interval) {
        return LLEntities.register(Identifier.fromNamespaceAndPath(AcademyCraft.MODID, name),
            EntityType.Builder.of(factory, MobCategory.MISC).sized(w, h).clientTrackingRange(range)
                .updateInterval(interval).noLootTable());
    }

    private static <T extends Entity> EntityType<T> local(String name) {
        return LLEntities.register(Identifier.fromNamespaceAndPath(AcademyCraft.MODID, name),
            EntityType.Builder.<T>of((type, level) -> null, MobCategory.MISC).sized(0.25f, 0.25f).noSave().noSummon()
                .clientTrackingRange(0).noLootTable());
    }

    public static void init() {}

}
