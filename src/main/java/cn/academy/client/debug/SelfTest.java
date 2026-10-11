package cn.academy.client.debug;

import cn.academy.AcademyCraft;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.client.ui.SkillTree;
import cn.academy.ability.develop.PortableDevData;
import cn.academy.client.auxgui.PresetEditUI;
import cn.academy.client.auxgui.TerminalUI;
import cn.academy.client.gui.GuiTutorial;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.academy.entity.*;
import cn.academy.misc.media.MediaGui;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.AppRegistry;
import cn.academy.terminal.TerminalData;
import cn.academy.terminal.app.settings.SettingsUI;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Development-only smoke test. Enabled when the file "academy_selftest" exists in the game directory: after joining
 *  a world it sets up an ability, places all blocks, cycles held items, opens the GUIs and spawns effect entities,
 *  logging every step. Errors are logged with the "[SelfTest]" prefix.
 */
@Environment(EnvType.CLIENT)
public final class SelfTest {

    private SelfTest() {}

    private interface Step {
        void run() throws Exception;
    }

    private record TimedStep(String name, int duration, Step step) {}

    private static final List<TimedStep> steps = new ArrayList<>();
    private static int stepIndex = -1;
    private static int wait = 0;
    private static boolean started = false;

    private static final List<BlockPos> placed = new ArrayList<>();

    private static boolean itemsOnly() {
        try {
            return Files.readString(FabricLoader.getInstance().getGameDir().resolve("academy_selftest")).contains("items");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean skillsOnly() {
        try {
            return Files.readString(FabricLoader.getInstance().getGameDir().resolve("academy_selftest")).contains("skills");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean railgunOnly() {
        try {
            return Files.readString(FabricLoader.getInstance().getGameDir().resolve("academy_selftest")).contains("railgun");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean mdOnly() {
        try {
            return Files.readString(FabricLoader.getInstance().getGameDir().resolve("academy_selftest")).contains("mdball");
        } catch (Exception e) {
            return false;
        }
    }

    private static net.minecraft.world.entity.animal.cow.Cow zombie;

    private static void addMdTest() {
        add("md setup", 20, () -> server(() -> {
            ServerPlayer p = splayer();
            p.setGameMode(GameType.SURVIVAL);
            AbilityData data = AbilityData.get(p);
            data.setCategory(cn.academy.ability.vanilla.VanillaCategories.meltdowner);
            data.setLevel(5);
            data.learnAllSkills();
            CPData.get(p).setActivateState(true);
            p.teleportTo(p.level(), p.getX(), p.getY(), p.getZ(), java.util.Set.of(), 0f, 0f, true);
            zombie = net.minecraft.world.entity.EntityTypes.COW.create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            zombie.setPos(p.getX(), p.getY(), p.getZ() + 6);
            zombie.setNoAi(true);
            p.level().addFreshEntity(zombie);
        }));
        for (cn.academy.ability.Skill skill : new cn.academy.ability.Skill[] {
                cn.academy.ability.vanilla.meltdowner.skill.ElectronBomb.Instance,
                cn.academy.ability.vanilla.meltdowner.skill.ScatterBomb.INSTANCE }) {
            final int kid = 9200;
            add(skill.getName() + " prepare", 3, () -> server(() -> {
                ServerPlayer p = splayer();
                CPData cp = CPData.get(p);
                cp.setOverload(0);
                cp.setCP(cp.getMaxCP());
                cn.academy.datapart.CooldownData.of(p).clear();
                zombie.setHealth(zombie.getMaxHealth());
            }));
            add(skill.getName() + " keydown", 40, () -> {
                cn.academy.ability.context.ClientRuntime rt = cn.academy.ability.context.ClientRuntime.instance();
                rt.clearKeys(cn.academy.ability.context.ClientRuntime.DEFAULT_GROUP);
                skill.activate(rt, kid);
                cn.lambdalib2.input.Keys.forcedDown.add(kid);
            });
            shot("md_" + skill.getName());
            add(skill.getName() + " keyup", 60, () -> cn.lambdalib2.input.Keys.forcedDown.remove(kid));
            add(skill.getName() + " result", 5, () -> server(() -> AcademyCraft.log.info("[SelfTest] " + skill.getName()
                + " zombie health " + zombie.getHealth() + "/" + zombie.getMaxHealth() + " alive=" + zombie.isAlive())));
        }
    }

    private static int qtePolls = 0;
    private static int rgKey;

    private static void pollQte() {
        cn.academy.entity.EntityCoinThrowing coin = cn.academy.item.ItemCoin.getPlayerCoin(mc().player);
        double prog = coin == null ? -1 : coin.getProgress();
        AcademyCraft.log.info("[SelfTest] railgun poll coin=" + coin + " progress=" + prog + " y=" + (coin == null ? 0 : coin.getY()) + " py=" + mc().player.getY()
            + " state=" + currentDelegate.getState());
        if (prog > 0.75 || ++qtePolls > 200) {
            AcademyCraft.log.info("[SelfTest] railgun pressing key, cp=" + CPData.get(mc().player).getCP());
            cn.lambdalib2.input.Keys.forcedDown.add(rgKey);
            return;
        }
        steps.add(stepIndex + 1, new TimedStep("railgun poll", 0, SelfTest::pollQte));
    }

    private static void addRailgunTest() {
        add("rg setup", 20, () -> server(() -> {
            ServerPlayer p = splayer();
            p.setGameMode(GameType.SURVIVAL);
            AbilityData data = AbilityData.get(p);
            data.setCategory(cn.academy.ability.vanilla.VanillaCategories.electromaster);
            data.setLevel(5);
            data.learnAllSkills();
            CPData.get(p).setActivateState(true);
            CPData.get(p).setOverload(0);
            CPData.get(p).setCP(CPData.get(p).getMaxCP());
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(cn.academy.ACItems.coin, 16));
            p.teleportTo(p.level(), -8.5, p.getY(), 8.5, java.util.Set.of(), 0f, 0f, true);
            cn.academy.datapart.PresetData pd = cn.academy.datapart.PresetData.get(p);
            pd.setPreset(pd.getCurrentID(), new cn.academy.datapart.PresetData.Preset(new cn.academy.ability.Controllable[] {
                cn.academy.ability.vanilla.electromaster.skill.Railgun.INSTANCE, null, null, null }));
        }));
        add("rg delegate", 5, () -> {
            cn.academy.ability.context.ClientRuntime rt = cn.academy.ability.context.ClientRuntime.instance();
            rgKey = cn.academy.ability.ctrl.ClientHandler.getKeyMapping(0);
            for (cn.academy.ability.context.KeyDelegate d : rt.getDelegates(cn.academy.ability.context.ClientRuntime.DEFAULT_GROUP)) currentDelegate = d;
        });
        add("rg throw", 0, () -> mc().gameMode.useItem(mc().player, InteractionHand.MAIN_HAND));
        add("rg poll", 0, SelfTest::pollQte);
        add("rg release", 10, () -> cn.lambdalib2.input.Keys.forcedDown.remove(rgKey));
        add("rg result", 20, () -> AcademyCraft.log.info("[SelfTest] railgun after, cp=" + CPData.get(mc().player).getCP()
            + " server cp=" + CPData.get(splayer()).getCP()));
        add("third person", 10, () -> mc().options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
        add("rg throw tp", 6, () -> mc().gameMode.useItem(mc().player, InteractionHand.MAIN_HAND));
        shot("railgun_tp_coin");
        add("rg wait", 40, () -> {});
    }

    private static boolean enabled() {
        return Files.exists(FabricLoader.getInstance().getGameDir().resolve("academy_selftest"));
    }

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        if (!enabled()) return;
        AcademyCraft.log.info("[SelfTest] enabled");
        buildSteps();
        ClientTickEvents.END_CLIENT_TICK.register(SelfTest::tick);
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static void server(Step s) throws Exception {
        MinecraftServer server = mc().getSingleplayerServer();
        Exception[] err = new Exception[1];
        server.executeBlocking(() -> {
            try {
                s.run();
            } catch (Exception e) {
                err[0] = e;
            }
        });
        if (err[0] != null) throw err[0];
    }

    private static ServerPlayer splayer() {
        return mc().getSingleplayerServer().getPlayerList().getPlayer(mc().player.getUUID());
    }

    private static void add(String name, int duration, Step step) {
        steps.add(new TimedStep(name, duration, step));
    }

    private static void shot(String name) {
        add("shot " + name, 5, () -> net.minecraft.client.Screenshot.grab(mc(), true));
    }

    private static void screen(String name, Supplier<Screen> s) {
        add("screen " + name, 30, () -> mc().gui.setScreen(s.get()));
        shot(name);
        add("close " + name, 5, () -> mc().gui.setScreen(null));
    }

    private static void buildSteps() {
        add("setup ability", 10, () -> server(() -> {
            ServerPlayer p = splayer();
            p.setGameMode(GameType.CREATIVE);
            AbilityData data = AbilityData.get(p);
            data.setCategory(CategoryManager.INSTANCE.getCategory(0));
            data.setLevel(5);
            data.learnAllSkills();
            CPData.get(p).setCP(CPData.get(p).getMaxCP());
            TerminalData td = TerminalData.get(p);
            td.install();
            for (App app : AppRegistry.enumeration()) td.installApp(app);
        }));

        if (mdOnly()) {
            addMdTest();
            add("done", 1000000, () -> AcademyCraft.log.info("[SelfTest] finished"));
            return;
        }
        if (railgunOnly()) {
            addRailgunTest();
            add("done", 1000000, () -> AcademyCraft.log.info("[SelfTest] finished"));
            return;
        }
        if (itemsOnly() || skillsOnly()) {
            add("third person off", 5, () -> {});
        } else
        add("place blocks", 20, () -> server(() -> {
            ServerPlayer p = splayer();
            BlockPos base = p.blockPosition().offset(3, -1, 3);
            int i = 0;
            for (Item item : BuiltInRegistries.ITEM) {
                if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("academy")) continue;
                if (!(item instanceof BlockItem)) continue;
                BlockPos ground = base.offset((i % 6) * 5, 0, (i / 6) * 5);
                ++i;
                for (int dx = -2; dx <= 2; ++dx)
                    for (int dz = -2; dz <= 2; ++dz) {
                        p.level().setBlockAndUpdate(ground.offset(dx, 0, dz), Blocks.STONE.defaultBlockState());
                        for (int dy = 1; dy <= 5; ++dy)
                            p.level().setBlockAndUpdate(ground.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                    }
                ItemStack stack = new ItemStack(item);
                p.setItemInHand(InteractionHand.MAIN_HAND, stack);
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false);
                try {
                    stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, hit));
                    placed.add(ground.above());
                } catch (Exception e) {
                    AcademyCraft.log.error("[SelfTest] placing " + item + " failed", e);
                }
            }
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.teleportTo(p.level(), base.getX() + 12.5, base.getY() + 5, base.getZ() - 6, java.util.Set.of(), 0f, 25f, true);
        }));

        add("look at blocks", 60, () -> {});
        shot("blocks");

        // Hold every item in hand for a while (first and third person item rendering)
        add("hold items", 0, () -> {
            if (skillsOnly()) return;
            List<Item> items = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("academy")) items.add(item);
            }
            java.util.Collections.reverse(items);
            for (Item item : items) {
                String id = BuiltInRegistries.ITEM.getKey(item).getPath();
                if (List.of("mag_hook", "silbarn", "developer_portable", "coin", "energy_unit", "matter_unit").contains(id)) {
                    steps.add(stepIndex + 1, new TimedStep("shot hold " + id, 3, () -> net.minecraft.client.Screenshot.grab(mc(), true)));
                }
                steps.add(stepIndex + 1, new TimedStep("hold " + item, 8, () -> server(() ->
                    splayer().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item)))));
            }
        });
        add("third person", 20, () -> mc().options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
        add("hold silbarn tp", 20, () -> server(() -> splayer().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(cn.academy.ACItems.silbarn))));
        shot("tp_silbarn");
        add("hold maghook tp", 20, () -> server(() -> splayer().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(cn.academy.ACItems.mag_hook))));
        shot("tp_maghook");
        add("hold dev tp", 20, () -> server(() -> splayer().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(cn.academy.ACItems.developer_portable))));
        shot("tp_dev");
        add("first person", 5, () -> mc().options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        if (itemsOnly()) {
            add("done", 1000000, () -> AcademyCraft.log.info("[SelfTest] finished"));
            return;
        }
        if (skillsOnly()) {
            addSkillTests();
            add("done", 1000000, () -> AcademyCraft.log.info("[SelfTest] finished"));
            return;
        }
        add("empty hand", 5, () -> server(() -> splayer().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)));

        // Machine GUIs: use every placed block
        add("use blocks", 0, () -> {
            for (BlockPos pos : placed) {
                steps.add(stepIndex + 1, new TimedStep("close gui " + pos, 5, () -> mc().gui.setScreen(null)));
                steps.add(stepIndex + 1, new TimedStep("use " + pos, 30, () -> {
                    LocalPlayer p = mc().player;
                    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                    mc().gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hit);
                }));
            }
        });

        screen("skill tree", SkillTree::skillTreeAppUI);
        screen("developer", () -> SkillTree.developerUI(PortableDevData.get(mc().player)));
        screen("preset edit", PresetEditUI::new);
        add("screen tutorial", 100, () -> mc().gui.setScreen(new GuiTutorial()));
        shot("tutorial");
        add("close tutorial", 5, () -> mc().gui.setScreen(null));
        screen("settings", SettingsUI::new);
        screen("media", MediaGui::new);

        add("terminal", 60, () -> AuxGui.register(new TerminalUI()));
        shot("terminal");
        add("terminal close", 10, () -> {
            for (AuxGui g : cn.lambdalib2.auxgui.AuxGuiHandler.active()) {
                if (g instanceof TerminalUI) g.dispose();
            }
        });

        add("effects", 15, () -> {
            LocalPlayer p = mc().player;
            List<Entity> list = new ArrayList<>();
            list.add(new EntityArc(p));
            list.add(new EntityMDRay(p, 20));
            list.add(new EntityRailgunFX(p, 20));
            list.add(new EntityDiamondShield(p));
            list.add(new EntityMdShield(p));
            list.add(new EntityIntensifyEffect(p));
            list.add(new EntitySurroundArc(p));
            list.add(new EntityTPMarking(p));
            list.add(new EntityMineRayBasic(p));
            EntityRippleMark mark = new EntityRippleMark(p.level());
            mark.setPos(p.getX() + 2, p.getY(), p.getZ());
            list.add(mark);
            EntityBloodSplash splash = new EntityBloodSplash(p.level());
            splash.setPos(p.getX() + 1, p.getY() + 1, p.getZ());
            list.add(splash);
            for (Entity e : list) p.level().addFreshEntity(e);
        });

        shot("effects");
        add("wait effects", 40, () -> {});
        add("third person", 30, () -> mc().options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
        shot("third_person");
        add("first person", 20, () -> mc().options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        addSkillTests();
        add("done", 1000000, () -> AcademyCraft.log.info("[SelfTest] finished"));
    }

    private static cn.academy.ability.context.KeyDelegate currentDelegate;

    /**
     * For every category: switch to it, then activate every controllable skill through its key delegate
     *  (key down, hold for a while, key up).
     */
    private static void addSkillTests() {
        int cats = CategoryManager.INSTANCE.getCategoryCount();
        for (int c = 0; c < cats; ++c) {
            final int cid = c;
            cn.academy.ability.Category cat = CategoryManager.INSTANCE.getCategory(cid);
            add("category " + cat.getName(), 20, () -> server(() -> {
                ServerPlayer p = splayer();
                AbilityData data = AbilityData.get(p);
                data.setCategory(cat);
                data.setLevel(5);
                data.learnAllSkills();
                CPData.get(p).setActivateState(true);
            }));
            int keyId = 0;
            for (cn.academy.ability.Skill skill : cat.getSkillList()) {
                if (!skill.canControl()) continue;
                final int kid = 9000 + (keyId++);
                add("skill " + skill.getName() + " close screens", 2, () -> mc().gui.setScreen(null));
                add("skill " + skill.getName() + " prepare", 3, () -> server(() -> {
                    ServerPlayer p = splayer();
                    CPData cp = CPData.get(p);
                    cp.setOverload(0);
                    cp.setCP(cp.getMaxCP());
                    cn.academy.datapart.CooldownData.of(p).clear();
                }));
                add("skill " + skill.getName() + " keydown", 30, () -> {
                    cn.academy.ability.context.ClientRuntime rt = cn.academy.ability.context.ClientRuntime.instance();
                    skill.activate(rt, kid);
                    cn.lambdalib2.input.Keys.forcedDown.add(kid);
                });
                add("skill " + skill.getName() + " keyup", 25, () -> {
                    cn.lambdalib2.input.Keys.forcedDown.remove(kid);
                    AcademyCraft.log.info("[SelfTest] skill " + skill.getName() + " contexts alive so far: "
                        + cn.academy.ability.context.ContextManager.localAliveCount);
                });
            }
            shot("skills_" + cat.getName());
            add("deactivate " + cat.getName(), 10, () -> {
                cn.academy.ability.context.ClientRuntime.instance().clearKeys(cn.academy.ability.context.ClientRuntime.DEFAULT_GROUP);
                server(() -> CPData.get(splayer()).setActivateState(false));
            });
        }
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        if (!started) {
            started = true;
            wait = 60; // let the world settle
            return;
        }
        if (wait > 0) {
            --wait;
            return;
        }
        ++stepIndex;
        if (stepIndex >= steps.size()) return;
        TimedStep s = steps.get(stepIndex);
        AcademyCraft.log.info("[SelfTest] step " + stepIndex + ": " + s.name());
        try {
            s.step().run();
        } catch (Throwable e) {
            AcademyCraft.log.error("[SelfTest] step '" + s.name() + "' failed", e);
        }
        wait = s.duration();
    }

}
