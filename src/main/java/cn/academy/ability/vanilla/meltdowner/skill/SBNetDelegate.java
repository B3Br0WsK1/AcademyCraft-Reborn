package cn.academy.ability.vanilla.meltdowner.skill;
import cn.academy.entity.EntityMdRaySmall;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


public enum SBNetDelegate {
    INSTANCE;
    public static final String MSG_EFFECT = "eff";

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        NetworkS11n.addDirectInstance(SBNetDelegate.INSTANCE);
    }

    @Environment(EnvType.CLIENT)
    @Listener(channel = MSG_EFFECT, side = Side.CLIENT)
    private void hSpawnEffect(Vec3 start, Vec3 end) {
        Player player = Minecraft.getInstance().player;
        EntityMdRaySmall raySmall  = new EntityMdRaySmall(player.level());
        raySmall.setFromTo(start, end);
        raySmall.viewOptimize = false;
        player.level().addFreshEntity(raySmall);
    }

}
