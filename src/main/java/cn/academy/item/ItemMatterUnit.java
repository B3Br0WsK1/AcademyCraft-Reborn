package cn.academy.item;

import cn.academy.ACBlocks;
import cn.academy.Resources;
import cn.academy.event.MatterUnitHarvestEvent;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.util.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The matter unit class. Have a simple material system for registration.
 *  The material id is stored as the {@link ItemMeta} value.
 * @author WeAthFolD
 */
public class ItemMatterUnit extends Item implements ISubItemProvider {

    public static class MatterMaterial {

        public final String name;
        public final Identifier texture;
        public final Block block;
        private int id;

        public MatterMaterial(String _name, Block block) {
            this(_name, block, Resources.getTexture("items/matter_unit/" + _name + "_mat"));
        }

        public MatterMaterial(String _name, Block _block, Identifier tex) {
            name = _name;
            texture = tex;
            block = _block;
        }

        public int getId() {
            return id;
        }

    }

    private static List<MatterMaterial> materials = new ArrayList<>();

    public static final MatterMaterial MAT_NONE = new MatterMaterial("none", Blocks.AIR);
    public static final MatterMaterial MAT_PHASE_LIQUID = new MatterMaterial("phase_liquid", ACBlocks.imag_phase);

    static {
        // !!! Order is important here, only append at the end
        addMatterMaterial(MAT_NONE);
        addMatterMaterial(MAT_PHASE_LIQUID);
    }

    private static void addMatterMaterial(MatterMaterial mat) {
        for(MatterMaterial prev : materials) {
            if(prev.name.equals(mat.name))
                throw new RuntimeException("Duplicate MatterMaterial Key " + mat.name);
        }
        mat.id = materials.size();
        materials.add(mat);
    }

    public static MatterMaterial getMatterMaterial(String name) {
        for(MatterMaterial mat : materials) {
            if(mat.name.equals(name))
                return mat;
        }
        return null;
    }

    public static List<MatterMaterial> getMaterials() {
        return materials;
    }

    //------

    public ItemMatterUnit(Properties props) {
        super(props);
    }

    public MatterMaterial getMaterial(ItemStack stack) {
        if(stack.getItem() != this)
            return null;
        int meta = ItemMeta.get(stack);
        if (meta < 0 || meta >= materials.size()) {
            return MAT_NONE;
        }
        return materials.get(meta);
    }

    public void setMaterial(ItemStack stack, MatterMaterial mat) {
        ItemMeta.set(stack, mat.id);
    }

    public void setMaterial(ItemStack stack, String name) {
        setMaterial(stack, getMatterMaterial(name));
    }

    public ItemStack create(String name) {
        return create(getMatterMaterial(name));
    }

    public ItemStack create(MatterMaterial mat) {
        ItemStack ret = new ItemStack(this);
        setMaterial(ret, mat);
        return ret;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean isNone = getMaterial(stack) == MAT_NONE;
        BlockHitResult rayRes = getPlayerPOVHitResult(world, player,
            isNone ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);

        if (rayRes.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        BlockPos pos = rayRes.getBlockPos();

        if (!world.mayInteract(player, pos)) {
            return InteractionResult.PASS;
        }
        if (!player.mayUseItemAt(pos, rayRes.getDirection(), stack)) {
            return InteractionResult.PASS;
        }

        if (isNone) {
            Block b = world.getBlockState(pos).getBlock();
            for(MatterMaterial m : materials) {
                if(m != MAT_NONE && m.block == b) {
                    if (!world.isClientSide()) {
                        // Match, merge the stack.
                        ItemStack newStack = create(m);
                        int left = PlayerUtils.mergeStackable(player.getInventory(), newStack);
                        if(left > 0) {
                            newStack.setCount(left);
                            PlayerUtils.dropItem(player, newStack);
                        }
                        // --stackSize
                        if(!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                        // Clear block
                        world.removeBlock(pos, false);
                        EventHub.BUS.post(new MatterUnitHarvestEvent(player, m));
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        } else {
            if (!world.isClientSide()) {
                BlockPos target = world.getBlockState(pos).canBeReplaced() ? pos : pos.relative(rayRes.getDirection());
                if (!world.getBlockState(target).canBeReplaced()) {
                    return InteractionResult.PASS;
                }
                world.setBlock(target, getMaterial(stack).block.defaultBlockState(), Block.UPDATE_ALL);

                ItemStack newStack = create(MAT_NONE);
                if(!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                int left = PlayerUtils.mergeStackable(player.getInventory(), newStack);
                if(left > 0) {
                    newStack.setCount(left);
                    PlayerUtils.dropItem(player, newStack);
                }
                EventHub.BUS.post(new MatterUnitHarvestEvent(player, MAT_NONE));
            }
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        MatterMaterial mat = getMaterial(stack);
        return Component.translatable("item.ac_matter_unit_" + (mat == null ? "none" : mat.name) + ".name");
    }

    @Override
    public void addSubItems(Consumer<ItemStack> output) {
        for(MatterMaterial mat : materials) {
            output.accept(create(mat));
        }
    }

}
