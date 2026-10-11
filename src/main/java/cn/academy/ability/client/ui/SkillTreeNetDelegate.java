package cn.academy.ability.client.ui;

import cn.academy.ability.Skill;
import cn.academy.ability.develop.DevelopData;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.ability.develop.action.DevelopActionLevel;
import cn.academy.ability.develop.action.DevelopActionReset;
import cn.academy.ability.develop.action.DevelopActionSkill;
import cn.academy.block.tileentity.TileDeveloper;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.impl.NodeConn;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.util.Side;

/**
 * Server handlers of the skill tree / developer UI. Ported from SkillTree.scala.
 */
public enum SkillTreeNetDelegate {
    INSTANCE;

    public static final String MSG_START_SKILL = "start_skill";
    public static final String MSG_GET_NODE = "get_node";
    public static final String MSG_RESET = "reset";
    public static final String MSG_START_LEVEL = "start_level";

    @StateEventCallback
    private static void __init(StateEvents.Init ev) {
        NetworkS11n.addDirectInstance(SkillTreeNetDelegate.INSTANCE);
    }

    @Listener(channel=MSG_START_SKILL, side=Side.SERVER)
    private void hStartSkill(DevelopData data, IDeveloper developer, Skill skill) {
        data.startDeveloping(developer, new DevelopActionSkill(skill));
    }

    @Listener(channel=MSG_START_LEVEL, side=Side.SERVER)
    private void hStartLevel(DevelopData data, IDeveloper developer) {
        data.startDeveloping(developer, new DevelopActionLevel());
    }

    @Listener(channel=MSG_GET_NODE, side=Side.SERVER)
    private void hGetLinkNodeName(TileDeveloper tile, Future<String> future) {
        NodeConn conn = tile instanceof IWirelessUser u ? WirelessHelper.getNodeConn(u) : null;
        future.sendResult(conn == null ? null : conn.getNode().getNodeName());
    }

    @Listener(channel=MSG_RESET, side=Side.SERVER)
    private void hStartReset(DevelopData data, IDeveloper developer) {
        data.startDeveloping(developer, new DevelopActionReset());
    }

}
