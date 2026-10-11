package cn.lambdalib2.mixin;

import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.IEntityDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntityDataHolder {

    @Unique
    private EntityData<?> lambdalib2$entityData;

    @Override
    public EntityData<?> lambdalib2$getEntityData() {
        return lambdalib2$entityData;
    }

    @Override
    public void lambdalib2$setEntityData(EntityData<?> data) {
        lambdalib2$entityData = data;
    }

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void lambdalib2$save(ValueOutput output, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!EntityData.needEntityDataFor(self.getClass())) return;
        CompoundTag tag = new CompoundTag();
        EntityData._onEntitySave(self, tag);
        output.store(EntityData.ID, CompoundTag.CODEC, tag);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void lambdalib2$load(ValueInput input, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!EntityData.needEntityDataFor(self.getClass())) return;
        input.read(EntityData.ID, CompoundTag.CODEC).ifPresent(tag -> EntityData._onEntityLoad(self, tag));
    }
}
