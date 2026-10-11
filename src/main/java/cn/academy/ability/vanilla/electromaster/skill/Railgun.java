package cn.academy.ability.vanilla.electromaster.skill;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.client.render.misc.RailgunHandEffect;
import cn.academy.client.sound.ACSounds;
import cn.academy.datapart.CPData;
import cn.academy.datapart.PresetData;
import cn.academy.entity.EntityCoinThrowing;
import cn.academy.entity.EntityRailgunFX;
import cn.academy.event.CoinThrowEvent;
import cn.academy.util.RangedRayDamage;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.renderhook.DummyRenderData;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.SideUtils;
import cn.lambdalib2.util.VecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * Railgun. Ported from Scala.
 */
public class Railgun extends Skill {

    public static final Railgun INSTANCE = new Railgun();

    private static final String MSG_CHARGE_EFFECT = "charge_eff";
    private static final String MSG_PERFORM = "perform";
    private static final String MSG_REFLECT = "reflect";
    private static final String MSG_COIN_PERFORM = "coin_perform";
    private static final String MSG_ITEM_PERFORM = "item_perform";

    private static final int REFLECT_DISTANCE = 15;

    private boolean hitEntity = false;

    private static final Set<Item> acceptedItems = Set.of(Items.IRON_INGOT, Items.IRON_BLOCK);

    private Railgun() {
        super("railgun", 4);
        EventHub.BUS.register(this);
    }

    public static boolean isAccepted(ItemStack stack) {
        return stack != null && !stack.isEmpty() && acceptedItems.contains(stack.getItem());
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new Delegate());
    }

    @SubscribeEvent
    public void onThrowCoin(CoinThrowEvent evt) {
        CPData cpData = CPData.get(evt.getEntityPlayer());
        PresetData pData = PresetData.get(evt.getEntityPlayer());

        boolean spawn = cpData.canUseAbility() && pData.getCurrentPreset().hasControllable(this);

        if (spawn) {
            if (evt.getEntityPlayer().level().isClientSide()) {
                informDelegate(evt.coin);
            } else {
                NetworkMessage.sendToAllAround(
                    TargetPoints.convert(evt.getEntityPlayer(), 30),
                    this,
                    MSG_CHARGE_EFFECT,
                    evt.getEntityPlayer()
                );
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private void informDelegate(EntityCoinThrowing coin) {
        ClientRuntime rt = ClientRuntime.instance();
        for (KeyDelegate dele : rt.getDelegates(ClientRuntime.DEFAULT_GROUP)) {
            if (dele instanceof Delegate rgdele) {
                rgdele.informThrowCoin(coin);
                return;
            }
        }
    }

    @Environment(EnvType.CLIENT)
    @Listener(channel=MSG_CHARGE_EFFECT, side=Side.CLIENT)
    private void hSpawnClientEffect(Player target) {
        spawnClientEffect(target);
    }

    @Environment(EnvType.CLIENT)
    @Listener(channel=MSG_REFLECT, side=Side.CLIENT)
    private void hReflectClient(Player player, Entity reflector) {
        EntityRailgunFX eff = new EntityRailgunFX(player, REFLECT_DISTANCE);

        double dist = player.distanceTo(reflector);
        Vec3 mo = VecUtils.lookingPos(player, dist);

        eff.setPos(mo.x, mo.y, mo.z);
        eff.setYRot(reflector.getYHeadRot());
        eff.setXRot(reflector.getXRot());

        player.level().addFreshEntity(eff);
    }

    private void reflectServer(Player player, Entity reflector) {
        AbilityContext ctx = AbilityContext.of(player, this);

        RayTraceResult result = Raytrace.traceLiving(reflector, REFLECT_DISTANCE);
        if (result != null && result.typeOfHit == RayTraceResult.Type.ENTITY) {
            ctx.attack(result.entityHit, 14);
            hitEntity = true;
        }

        NetworkMessage.sendToAllAround(TargetPoints.convert(player, 20),
            this, MSG_REFLECT, player, reflector);
    }

    @Environment(EnvType.CLIENT)
    private static void spawnClientEffect(Player target) {
        DummyRenderData.get(target).addRenderHook(new RailgunHandEffect());
    }

    @Environment(EnvType.CLIENT)
    @Listener(channel=MSG_PERFORM, side=Side.CLIENT)
    private void performClient(Player player, double length) {
        ACSounds.playClient(
            player.level(),
            player.getX(), player.getY(), player.getZ(),
            "em.railgun",
            SoundSource.AMBIENT,
            0.5f,
            1.0f
        );
        player.level().addFreshEntity(new EntityRailgunFX(player, length));
    }

    private void performServer(Player player) {
        AbilityContext ctx = AbilityContext.of(player, this);

        float exp = ctx.getSkillExp();

        float cp = lerpf(200, 450, exp);
        float overload = lerpf(180, 120, exp);
        if (ctx.consume(overload, cp)) {
            float dmg = lerpf(60, 110, exp);
            float energy = lerpf(900, 2000, exp);

            double[] length = { 45d };
            hitEntity = false;
            RangedRayDamage damage = new RangedRayDamage.Reflectible(ctx, 2, energy, reflector -> {
                reflectServer(player, reflector);
                length[0] = Math.min(length[0], reflector.distanceTo(player));
            });
            damage.startDamage = dmg;
            damage.perform();

            if (hitEntity) ctx.addSkillExp(0.01f); else ctx.addSkillExp(0.005f);

            ctx.setCooldown((int) lerpf(300, 160, exp));
            NetworkMessage.sendToAllAround(
                TargetPoints.convert(player, 20),
                this, MSG_PERFORM,
                player, length[0]);
        }
    }

    @Listener(channel=MSG_COIN_PERFORM, side=Side.SERVER)
    private void consumeCoinAtServer(Player player, EntityCoinThrowing coin) {
        if (coin != null) coin.setDead();
        performServer(player);
    }

    @Listener(channel=MSG_ITEM_PERFORM, side=Side.SERVER)
    private void consumeItemAtServer(Player player) {
        ItemStack equipped = player.getMainHandItem();
        if (isAccepted(equipped)) {
            if (!player.getAbilities().instabuild) {
                equipped.shrink(1);
                if (equipped.getCount() == 0) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }

            performServer(player);
        }
    }

    @Environment(EnvType.CLIENT)
    private static class Delegate extends KeyDelegate {

        EntityCoinThrowing coin;

        int chargeTicks = -1;

        void informThrowCoin(EntityCoinThrowing _coin) {
            if (this.coin == null || this.coin.isRemoved()) {
                this.coin = _coin;
                onKeyAbort();
            }
        }

        @Override
        public void onKeyDown() {
            if (coin == null || coin.isRemoved()) {
                coin = null;
                if (Railgun.isAccepted(getPlayer().getMainHandItem())) {
                    Railgun.spawnClientEffect(getPlayer());
                    chargeTicks = 20;
                }
            } else {
                tryShootCoin();
            }
        }

        /**
         * Fires once the coin is falling back (QTE window). Pressing early no longer wastes the coin: holding the key
         *  fires as soon as the window is reached.
         */
        private void tryShootCoin() {
            if (coin != null && !coin.isRemoved() && coin.getProgress() > 0.7) {
                NetworkMessage.sendToServer(INSTANCE,
                    MSG_COIN_PERFORM, getPlayer(), coin);
                coin = null; // Prevent second QTE judgement
            }
        }

        @Override
        public void onKeyTick() {
            if (coin != null) {
                tryShootCoin();
            }
            if (chargeTicks != -1) {
                chargeTicks -= 1;
                if (chargeTicks == 0) {
                    NetworkMessage.sendToServer(INSTANCE,
                        MSG_ITEM_PERFORM, getPlayer());
                }
            }
        }

        @Override
        public void onKeyUp() {
            chargeTicks = -1;
        }

        @Override
        public void onKeyAbort() {
            chargeTicks = -1;
        }

        @Override
        public DelegateState getState() {
            if (coin != null && !coin.isRemoved()) {
                return coin.getProgress() < 0.6 ? DelegateState.CHARGE : DelegateState.ACTIVE;
            } else {
                return chargeTicks == -1 ? DelegateState.IDLE : DelegateState.CHARGE;
            }
        }

        @Override
        public Identifier getIcon() {
            return INSTANCE.getHintIcon();
        }

        @Override
        public int createID() {
            return 0;
        }

        @Override
        public Skill getSkill() {
            return INSTANCE;
        }
    }
}
