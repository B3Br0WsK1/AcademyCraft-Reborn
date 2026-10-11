package cn.academy.tutorial;
import cn.academy.ACItems;
import cn.academy.AcademyCraft;
import cn.academy.event.TutorialActivatedEvent;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.s11n.nbt.NBTS11n;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.TickScheduler;
import com.google.common.base.Preconditions;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.Side;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import cn.lambdalib2.event.EventHub;
import java.util.BitSet;
import java.util.HashSet;



/**
 * This class simply stores activate data that trigger-type condition needs.
 */
@RegDataPart(Player.class)
public class TutorialData extends DataPart<Player> {

    @StateEventCallback
    private static void __init(StateEvents.Init ev) {
        // Force the property to load, so it will be refreshed once after startup
        canAcquireTutorial();
    }

    public static TutorialData get(Player player) {
        return EntityData.get(player).getPart(TutorialData.class);
    }

    private static final String MSG_ACTIVATE = "activate";

    @SerializeIncluded
    private BitSet savedConditions = new BitSet();
    @SerializeIncluded
    private HashSet<String> activatedTuts = new HashSet<>();
    @SerializeIncluded
    private boolean tutorialAcquired = false;
    @SerializeIncluded
    private int misakaID = -1;

    private final TickScheduler scheduler = new TickScheduler();
    private boolean dirty;

    public TutorialData() {
        setTick(true);
        setClientNeedSync();
        setNBTStorage();

        misakaID = RandUtils.rangei(1000, 19000);

        scheduler.every(3).atOnly(Side.SERVER).run(() -> {
            if (dirty) {
                dirty = false;
                TutorialRegistry.enumeration().forEach(tut -> {
                    if (!activatedTuts.contains(tut.id) &&
                            tut.isActivated(getEntity()) &&
                            !tut.isDefaultInstalled()) {
                        activatedTuts.add(tut.id);

                        onTutorialActivate(tut.id);
                        sendToLocal(MSG_ACTIVATE, tut.id);
                    }
                });

                sync();
            }
        });

        if (canAcquireTutorial()) {
            scheduler.every(10).atOnly(Side.SERVER).run(() -> {
                if (!tutorialAcquired) {
                    Player player = getEntity();
                    player.level().addFreshEntity(new ItemEntity(player.level(),
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            new ItemStack(ACItems.tutorial)));

                    tutorialAcquired = true;
                }
            });
        }
    }

    @Override
    public void tick() {
        scheduler.runTick();
    }

    public int getMisakaID() {
        return misakaID;
    }

    boolean isCondActivate(int index) {
        return savedConditions.get(index);
    }

    void setCondActivate(int index) {
        checkSide(Side.SERVER);

        if (!savedConditions.get(index)) {
            savedConditions.set(index);
            dirty = true;
        }
    }

    @Override
    public void toNBT(CompoundTag tag) {
        NBTS11n.write(tag, this);
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        NBTS11n.read(tag, this);
    }

    @Listener(channel=MSG_ACTIVATE, side={Side.CLIENT, Side.SERVER})
    private void onTutorialActivate(String tutName) {
        ACTutorial tut = TutorialRegistry.getTutorial(tutName);
        Preconditions.checkNotNull(tut);

        EventHub.BUS.post(new TutorialActivatedEvent(getEntity(), tut));
    }

    private static boolean canAcquireTutorial() {
        return AcademyCraft.config.getBoolean("giveCloudTerminal", "generic", true,
                "Whether the player will be given MisakaCloud Terminal on first spawn.");
    }

}