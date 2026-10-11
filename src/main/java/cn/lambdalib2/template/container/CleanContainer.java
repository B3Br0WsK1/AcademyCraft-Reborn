package cn.lambdalib2.template.container;

import com.google.common.primitives.Ints;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This class wraps around vanilla MC's {@link AbstractContainerMenu}, making the dirty part somehow more usable --
 *  Namely, the ItemStack transfering part, replaced by the {@link #addTransferRule(SlotGroup, Predicate, SlotGroup)}
 *  interface.
 */
public abstract class CleanContainer extends AbstractContainerMenu {

    protected CleanContainer(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    /**
     * @return A slot group containing specified slots
     */
    public static SlotGroup gSlots(int ...slots) {
        return new SlotGroup(slots);
    }

    /**
     * @return A slot group containing specified range of slots
     */
    public static SlotGroup gRange(int from, int toExclusive) {
        return new SlotGroup(from, toExclusive);
    }

    /**
     * @return A slot group that is the combination of given groups
     */
    public static SlotGroup gCombine(SlotGroup ... groups) {
        Set<Integer> collector = new HashSet<>();
        for (SlotGroup g : groups) {
            for (int i : g.slots) {
                collector.add(i);
            }
        }

        return new SlotGroup(Ints.toArray(collector));
    }

    private final List<Rule> rules = new ArrayList<>();

    /**
     * 1.12 name of addSlot.
     */
    protected Slot addSlotToContainer(Slot slot) {
        return addSlot(slot);
    }

    /**
     * Adds a stack transfer rule.
     * @param fromSlots Slots that accepts this rule
     * @param inPredicate Tests stacks in from slot, only those passed will be transfered
     * @param toSlots Slot id to transfer stack to
     */
    public void addTransferRule(SlotGroup fromSlots, Predicate<ItemStack> inPredicate, SlotGroup toSlots) {
        Rule rule = new Rule();
        rule.from = fromSlots;
        rule.pred = inPredicate;
        rule.to = toSlots;
        rules.add(rule);
    }

    public void addTransferRule(SlotGroup from, SlotGroup to) {
        addTransferRule(from, s -> true, to);
    }

    @Override
    public final ItemStack quickMoveStack(Player player, int slotID) {
        final Slot slot = getSlot(slotID);
        if (slot.hasItem()) {
            final ItemStack slotStack = slot.getItem();
            final ItemStack stackCopy = slotStack.copy();

            boolean slotChanged = false;
            for (Rule rule : rules) {
                if (rule.from.accepts(slotID) && rule.pred.test(slotStack)) {

                    for (int toSlot : rule.to.slots) {
                        if (slotStack.getCount() <= 0)
                            break;

                        slotChanged |= tryMerge(slotStack, toSlot);
                    }

                    break;
                }
            }

            if (slotStack.getCount() == 0) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else if (slotChanged) {
                slot.setChanged();
            }

            return slotChanged ? stackCopy : ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
    }

    /**
     * Put item(stack) to slot[idx]. If slot is empty, put it. Then check if they can be merged.
     */
    private boolean tryMerge(ItemStack stackToMerge, int idx) {
        Slot slot = this.slots.get(idx);
        ItemStack stack = slot.getItem();
        boolean changed = false;

        if (stack.isEmpty() && slot.mayPlace(stackToMerge)) {
            stack = stackToMerge.copy();
            stack.setCount(1);
            stackToMerge.shrink(1);

            slot.setByPlayer(stack);
            slot.setChanged();
            changed = true;
        }

        if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stackToMerge, stack)) {
            int ideal = stack.getCount() + stackToMerge.getCount();
            int max = Math.min(stackToMerge.getMaxStackSize(), slot.getMaxStackSize(stack));
            if(ideal <= max) {
                stackToMerge.setCount(0);
                stack.setCount(ideal);
                slot.setChanged();
                return true;
            } else if(stack.getCount() < max) {
                stackToMerge.shrink( max - stack.getCount() );
                stack.setCount(max);
                slot.setChanged();
                return true;
            }
        }
        return changed;
    }

    public static class SlotGroup {

        final int[] slots;

        private SlotGroup(int[] slots) {
            this.slots = slots;
        }

        private SlotGroup(int from, int toExclusive) {
            this.slots = new int[toExclusive - from];
            for (int i = from; i < toExclusive; ++i) {
                slots[i - from] = i;
            }
        }

        private boolean accepts(int slot) {
            for (int i : slots) {
                if (i == slot) return true;
            }
            return false;
        }

    }

    private static class Rule {
        SlotGroup from, to;
        Predicate<ItemStack> pred;
    }
}
