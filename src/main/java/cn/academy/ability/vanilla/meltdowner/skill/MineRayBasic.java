package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.entity.EntityMineRayBasic;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Ported from Scala.
 * @author WeAthFolD, KSkun
 */
public class MineRayBasic extends MineRaysBase {

    public static final MineRayBasic INSTANCE = new MineRayBasic();

    private MineRayBasic() {
        super("basic", 3);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, Ctx::new);
    }

    public static class Ctx extends MRContext {

        public Ctx(Player p) {
            super(p, INSTANCE);
            setRange(10);
            setHarvestLevel(2);
            setSpeed(0.2f, 0.4f);
            setConsumption(12.0f, 7.0f);
            setOverload(200f, 150f);
            setCooldown(40f, 20f);
            setExpIncr(0.0005f);
        }

        @Override
        protected void onBlockBreak(Level world, BlockPos pos) {
            breakBlock(world, pos, player, 0);
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(Ctx.class)
    public static class CtxC extends MRContextC {

        public CtxC(Context par) {
            super(par);
        }

        @Override
        protected Entity createRay() {
            return new EntityMineRayBasic(player);
        }

    }

}
