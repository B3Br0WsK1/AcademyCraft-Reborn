package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.entity.EntityMineRayLuck;
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
public class MineRayLuck extends MineRaysBase {

    public static final MineRayLuck INSTANCE = new MineRayLuck();

    private MineRayLuck() {
        super("luck", 5);
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
            setSpeed(.5f, 1);
            setConsumption(50, 35);
            setOverload(350, 300);
            setCooldown(60, 30);
            setExpIncr(0.0003f);
        }

        @Override
        protected void onBlockBreak(Level world, BlockPos pos) {
            breakBlock(world, pos, player, 3);
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
            return new EntityMineRayLuck(player);
        }

    }

}
