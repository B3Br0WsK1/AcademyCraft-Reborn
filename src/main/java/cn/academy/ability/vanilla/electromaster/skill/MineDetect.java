package cn.academy.ability.vanilla.electromaster.skill;

import cn.academy.Resources;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.electromaster.CatElectromaster;
import cn.academy.client.sound.ACSounds;
import cn.academy.entity.LocalEntity;
import cn.lambdalib2.render.legacy.GL11;
import cn.lambdalib2.render.legacy.LegacyGL;
import cn.lambdalib2.render.legacy.LegacyMesh;
import cn.lambdalib2.render.legacy.LegacyMeshUtils;
import cn.lambdalib2.render.legacy.RenderStage;
import cn.lambdalib2.render.legacy.SimpleMaterial;
import cn.lambdalib2.render.legacy.Tessellator;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.IBlockSelector;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.WorldUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * Mine detection. Ported from Scala.
 * @author WeAthFolD, KSkun
 */
public class MineDetect extends Skill {

    public static final MineDetect INSTANCE = new MineDetect();

    public static final int TIME = 100;

    private MineDetect() {
        super("mine_detect", 3);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyid) {
        activateSingleKey(rt, keyid, MDContext::new);
    }

    public static final String MSG_EFFECT = "effect";
    public static final String MSG_EXECUTE = "execute";

    public static class MDContext extends Context<MineDetect> {

        private final float range = lerpf(15, 30, ctx.getSkillExp());
        private final boolean isAdvanced = ctx.getSkillExp() > 0.5f && ctx.aData.getLevel() >= 4;

        public MDContext(Player p) {
            super(p, INSTANCE);
        }

        private boolean consume() {
            float exp = ctx.getSkillExp();

            float cp = lerpf(1500, 1000, exp);
            float overload = lerpf(200, 180, exp);

            return ctx.consume(overload, cp);
        }

        @Listener(channel=MSG_KEYDOWN, side=Side.CLIENT)
        private void l_onKeyDown() {
            sendToServer(MSG_EXECUTE);
        }

        @Listener(channel=MSG_EXECUTE, side=Side.SERVER)
        private void s_execute() {
            if (consume()) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TIME));
                ctx.addSkillExp(0.008f);
                sendToClient(MSG_EFFECT, range, isAdvanced);

                float exp = ctx.getSkillExp();
                int cooldown = (int) lerpf(900, 400, exp);
                ctx.setCooldown(cooldown);
            }
            terminate();
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(MDContext.class)
    public static class MDContextC extends ClientContext {

        public MDContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_EFFECT, side=Side.CLIENT)
        private void c_spawnEffects(float range, boolean advanced) {
            if (isLocal()) {
                player.level().addFreshEntity(new HandlerEntity(player, TIME, range, advanced));
                ACSounds.playClient(player, "em.minedetect", SoundSource.AMBIENT, 0.5f);
            }
        }

    }

    public record MineElem(int x, int y, int z, int level) {}

    /**
     * Client-local entity which tracks the ores around the player and renders them.
     */
    public static class HandlerEntity extends LocalEntity {

        static final IBlockSelector blockFilter = (world, x, y, z, block) -> CatElectromaster.isOreBlock(block);

        final List<MineElem> aliveSims = new ArrayList<>();

        final int lifeTime;
        final double range;
        final Player target;
        final boolean isAdvanced;

        private final List<BlockPos> blockPosBuffer = new ArrayList<>();

        public HandlerEntity(Player _target, int _time, double _range, boolean _advanced) {
            super(_target.level());
            target = _target;
            lifeTime = _time;
            range = Math.min(_range, 28);
            isAdvanced = _advanced;
            setPos(target.getX(), target.getY(), target.getZ());
        }

        @Override
        protected void onFirstUpdate() {
            updateBlocks();
        }

        @Override
        public void onUpdate() {
            super.onUpdate();

            setPos(target.getX(), target.getY(), target.getZ());
            if (tickCount % 5 == 0)
                updateBlocks();

            if (tickCount > lifeTime) {
                setDead();
            }
        }

        private int harvestLevel(BlockState state) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 1;
            return 0;
        }

        private void updateBlocks() {
            final int LIMIT = 8400;
            WorldUtils.getBlocksWithin(blockPosBuffer, this, range, LIMIT, blockFilter);

            aliveSims.clear();
            for (BlockPos bp : blockPosBuffer) {
                aliveSims.add(new MineElem(bp.getX(), bp.getY(), bp.getZ(),
                    isAdvanced ? Math.min(3, harvestLevel(world.getBlockState(bp)) + 1) : 0));
            }
        }

        // ---- Rendering

        @Environment(EnvType.CLIENT)
        private static class RenderData {
            static final Identifier texture = Resources.getTexture("effects/mineview");
            static final LegacyMesh mesh = LegacyMeshUtils.createBoxWithUV(null, .05, .05, .05, .9, .9, .9);
            static final SimpleMaterial material = new SimpleMaterial(texture).setIgnoreLight();

            static final Color[] colors = { // alpha will be reset each time rendering
                new Color(115, 200, 227, 0), // default color
                new Color(161, 181, 188, 0), // harvest level 0-3
                new Color(87, 231, 248, 0),
                new Color(97, 204, 94, 0),
                new Color(235, 109, 84, 0)
            };
        }

        @Override
        @Environment(EnvType.CLIENT)
        public void renderEffect(double x, double y, double z, float partialTicks) {
            Tessellator t = Tessellator.instance;
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);

            RenderUtils.loadTexture(RenderData.texture);

            RenderData.material.onRenderStage(RenderStage.BEFORE_TESSELLATE);

            Vec3 cam = LegacyGL.cameraPos();
            t.startDrawing(GL11.GL_TRIANGLES);
            for (MineElem me : aliveSims) {
                t.setTranslation(me.x() - cam.x, me.y() - cam.y, me.z() - cam.z);
                Color color = RenderData.colors[Math.min(RenderData.colors.length - 1, me.level())];
                color.setAlpha(calcAlpha(getX() - me.x(), getY() - me.y(), getZ() - me.z(), range));
                Colors.bindToGL(color);
                RenderData.mesh.redrawWithinBatch(RenderData.material);
            }
            t.draw();

            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        private static int calcAlpha(double x, double y, double z, double range) {
            double jdg = 1 - MathUtils.length(x, y, z) / range * 2.2;
            return Colors.f2i(0.3f + (float) (jdg * 0.7));
        }

    }

}
