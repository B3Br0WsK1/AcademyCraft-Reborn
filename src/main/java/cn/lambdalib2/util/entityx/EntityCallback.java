package cn.lambdalib2.util.entityx;

import net.minecraft.world.entity.Entity;

public interface EntityCallback<T extends Entity> {

    void execute(T target);

}
