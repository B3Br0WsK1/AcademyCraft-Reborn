package cn.academy.client.render.item;

import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.item.ImagEnergyItem;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

/**
 * Numeric item model property "academy:energy": stored imaginary energy / max energy (0~1) of an energy item.
 *  Replacement of the 1.12 "energy" item property override.
 */
@Environment(EnvType.CLIENT)
public record EnergyItemProperty() implements RangeSelectItemModelProperty {

    public static final MapCodec<EnergyItemProperty> MAP_CODEC = MapCodec.unit(new EnergyItemProperty());

    @Override
    public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
        if (stack.getItem() instanceof ImagEnergyItem item && item.getMaxEnergy() > 0) {
            return (float) (IFItemManager.instance.getEnergy(stack) / item.getMaxEnergy());
        }
        return 0;
    }

    @Override
    public MapCodec<EnergyItemProperty> type() {
        return MAP_CODEC;
    }

    @StateEventCallback
    private static void init(StateEvents.PreInit ev) {
        RangeSelectItemModelProperties.ID_MAPPER.put(Identifier.fromNamespaceAndPath("academy", "energy"), MAP_CODEC);
    }

}
