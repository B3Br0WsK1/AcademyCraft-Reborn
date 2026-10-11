package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.entity.EntityMineRayExpert;
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
public class MineRayExpert extends MineRaysBase {

    public static final MineRayExpert INSTANCE = new MineRayExpert();

    private MineRayExpert() {
        super("expert", 4);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, Ctx::new);
    }

    public static class Ctx extends MRContext {

        public Ctx(Player p) {
            super(p, INSTANCE);
            setRange(20);
            setHarvestLevel(5);
            setSpeed(0.5f, 1f);
            setConsumption(25f, 15f);
            setOverload(300f, 200f);
            setCooldown(60f, 30f);
            setExpIncr(0.0003f);
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
            return new EntityMineRayExpert(player);
        }

    }

}
