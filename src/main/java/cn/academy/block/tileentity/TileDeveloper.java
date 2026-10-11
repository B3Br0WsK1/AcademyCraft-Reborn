package cn.academy.block.tileentity;

import cn.academy.ACTiles;
import cn.academy.ability.client.ui.DeveloperUI;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.IDeveloper;
import cn.lambdalib2.multiblock.IMultiTile;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkMessage.NullablePar;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.util.Side;
import com.google.common.base.Preconditions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author WeAthFolD
 *
 */
public abstract class TileDeveloper extends TileReceiverBase implements IMultiTile, IDeveloper {

    private static final String
        MSG_OPEN_GUI = "gui",
        MSG_UNUSE = "unuse",
        MSG_SYNC  = "sync";

    public static class Normal extends TileDeveloper {
        public Normal(BlockPos pos, BlockState state) {
            super(ACTiles.DEVELOPER_NORMAL, pos, state, DeveloperType.NORMAL);
        }
    }

    public static class Advanced extends TileDeveloper {
        public Advanced(BlockPos pos, BlockState state) {
            super(ACTiles.DEVELOPER_ADVANCED, pos, state, DeveloperType.ADVANCED);
        }
    }

    public final DeveloperType type;

    private int syncCD;

    private Player user;

    private TileDeveloper(BlockEntityType<?> beType, BlockPos pos, BlockState state, DeveloperType _type) {
        super(beType, pos, state, "ability_developer", 2, _type.getEnergy(), _type.getBandwidth());
        type = _type;
    }

    @Override
    public void update() {
        super.update();

        if (!getWorld().isClientSide()) {
            if (user != null && (user.isRemoved() || user.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(getPos())) > 64)) {
                unuse();
            }
            if(++syncCD == 20) {
                syncCD = 0;
                send(MSG_SYNC, user);
            }
        }
    }

    public Player getUser() {
        return user;
    }

    /**
     * SERVER only. Start let the player use the developer, if currently no user is using it.
     */
    public boolean use(Player player) {
        Preconditions.checkState(!player.level().isClientSide());

        if (user != null) {
            unuse();
        }

        user = player;
        send(MSG_OPEN_GUI, player);
        return true;
    }

    /**
     * Is effective in BOTH CLIENT AND SERVER. Let the current player(if is equal to argument) go away from the developer.
     */
    public void unuse(Player p) {
        if(getWorld().isClientSide()) {
            send(MSG_UNUSE, p);
        } else {
            if(user != null && user.equals(p))
                unuse();
        }
    }

    private void unuse() {
        user = null;
    }

    @Override
    public boolean tryPullEnergy(double amount) {
        if(energy < amount)
            return false;
        pullEnergy(amount);
        return true;
    }

    @Override
    public void onGuiClosed() {
        if (user != null) {
            send(MSG_UNUSE, user);
        }
    }

    private void send(String channel, Object ...args) {
        if (getWorld().isClientSide()) {
            NetworkMessage.sendToServer(this, channel, args);
        } else {
            NetworkMessage.sendToAllAround(TargetPoints.convert(this, 10), this, channel, args);
        }
    }

    @Override
    public final DeveloperType getDeveloperType() {
        return type;
    }

    @Environment(EnvType.CLIENT)
    @Listener(channel=MSG_OPEN_GUI, side=Side.CLIENT)
    private void hOpenGui(Player player) {
        // Sync the player right away to prevent bad lookup
        this.user = player;

        if (player.equals(Minecraft.getInstance().player)) {
            Minecraft.getInstance().gui.setScreen(DeveloperUI.apply(this));
        }
    }

    @Listener(channel=MSG_UNUSE, side=Side.SERVER)
    private void hUnuse(Player player) {
        unuse(player);
    }

    @Listener(channel=MSG_SYNC, side=Side.CLIENT)
    private void hSync(@NullablePar Player player) {
        this.user = player;
    }

}
