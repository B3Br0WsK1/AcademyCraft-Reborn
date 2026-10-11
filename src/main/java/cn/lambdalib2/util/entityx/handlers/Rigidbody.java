package cn.lambdalib2.util.entityx.handlers;

import cn.lambdalib2.util.BlockSelectors;
import cn.lambdalib2.util.IBlockSelector;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.entityx.MotionHandler;
import cn.lambdalib2.util.entityx.event.CollideEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

public class Rigidbody extends MotionHandler {

    public double gravity = 0.00; //block/tick^2
    public double linearDrag = 1.0;

    public Predicate<Entity> entitySel;
    public IBlockSelector blockFil = BlockSelectors.filNormal;

    public boolean accurateCollision = false;

    @Override
    public String getID() {
        return "Rigidbody";
    }

    @Override
    public void onStart() {}

    @Override
    public void onUpdate() {
        Entity target = getTarget();
        Vec3 motion = target.getDeltaMovement();

        //Collision detection
        RayTraceResult result = null;
        if(accurateCollision) {
            float hw = target.getBbWidth() / 2, hh = target.getBbHeight();
            double px = target.getX(), py = target.getY(), pz = target.getZ();
            Vec3[] points = {
                    new Vec3(px - hw, py,      pz - hw),
                    new Vec3(px - hw, py,      pz + hw),
                    new Vec3(px + hw, py,      pz + hw),
                    new Vec3(px + hw, py,      pz - hw),
                    new Vec3(px - hw, py + hh, pz - hw),
                    new Vec3(px - hw, py + hh, pz + hw),
                    new Vec3(px + hw, py + hh, pz + hw),
                    new Vec3(px + hw, py + hh, pz - hw),
            };
            for(Vec3 vec : points) {
                Vec3 next = VecUtils.add(vec, motion);
                result = Raytrace.perform(target.level(), vec, next, entitySel, blockFil);
                if(result.typeOfHit != RayTraceResult.Type.MISS)
                    break;
            }
        } else {
            Vec3 cur = target.position(),
                    next = cur.add(motion);
            result = Raytrace.perform(target.level(), cur, next, entitySel, blockFil);
        }

        if(result != null && result.typeOfHit != RayTraceResult.Type.MISS) {
            getEntityX().postEvent(new CollideEvent(result)); //Let the event handlers do the actual job.
        }

        //Velocity update
        motion = target.getDeltaMovement();
        motion = new Vec3(motion.x * linearDrag, (motion.y - gravity) * linearDrag, motion.z * linearDrag);
        target.setDeltaMovement(motion);

        target.setOldPosAndRot();
        target.setPos(target.getX() + motion.x, target.getY() + motion.y, target.getZ() + motion.z);
    }

}
