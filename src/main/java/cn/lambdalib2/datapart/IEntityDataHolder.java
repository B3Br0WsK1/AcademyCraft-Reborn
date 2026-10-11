package cn.lambdalib2.datapart;

/**
 * Implemented on every {@link net.minecraft.world.entity.Entity} by mixin; holds the attached {@link EntityData}.
 */
public interface IEntityDataHolder {

    EntityData<?> lambdalib2$getEntityData();

    void lambdalib2$setEntityData(EntityData<?> data);

}
