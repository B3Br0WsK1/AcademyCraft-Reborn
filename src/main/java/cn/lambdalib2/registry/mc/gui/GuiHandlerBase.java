/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib2.registry.mc.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

/**
 * A GUI (menu + screen) bound to a block position. Each handler registers its own menu type.
 * <ul>
 *     <li>{@link #getServerContainer} creates the menu on both sides (on the client with the menu id sent by the
 *     server).</li>
 *     <li>{@link #getClientScreen} creates the screen for the client menu.</li>
 * </ul>
 * Use {@link #openGuiContainer} on the server to open it.
 */
public abstract class GuiHandlerBase {

    private ExtendedMenuType<AbstractContainerMenu, BlockPos> menuType;
    private Identifier id;

    void init(Identifier id) {
        this.id = id;
        this.menuType = Registry.register(BuiltInRegistries.MENU, id,
            new ExtendedMenuType<>((containerId, inventory, pos) -> {
                AbstractContainerMenu menu = getServerContainer(containerId, inventory.player, inventory.player.level(), pos);
                if (menu == null) {
                    throw new IllegalStateException("Failed to create client menu for " + id + " at " + pos);
                }
                return menu;
            }, BlockPos.STREAM_CODEC));
    }

    public Identifier getId() {
        return id;
    }

    public ExtendedMenuType<AbstractContainerMenu, BlockPos> getMenuType() {
        return menuType;
    }

    /**
     * Opens the GUI. Should be called in server.
     */
    public final void openGuiContainer(Player player, Level world, int x, int y, int z) {
        openGuiContainer(player, world, new BlockPos(x, y, z));
    }

    public final void openGuiContainer(Player player, Level world, BlockPos pos) {
        if (world.isClientSide() || !(player instanceof ServerPlayer sp)) {
            return;
        }
        sp.openMenu(new ExtendedMenuProvider<BlockPos>() {
            @Override
            public BlockPos getScreenOpeningData(ServerPlayer player) {
                return pos;
            }

            @Override
            public Component getDisplayName() {
                return Component.empty();
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
                return getServerContainer(containerId, p, p.level(), pos);
            }
        });
    }

    /**
     * Creates the menu.
     * @param containerId The menu id. Pass it to the menu ctor together with {@link #getMenuType()}.
     * @return The menu, or null if it can't be opened at the given position.
     */
    protected abstract AbstractContainerMenu getServerContainer(int containerId, Player player, Level world, BlockPos pos);

    /**
     * Creates the screen for the given (client) menu.
     */
    @Environment(EnvType.CLIENT)
    protected abstract Screen getClientScreen(AbstractContainerMenu container, Player player);

}
