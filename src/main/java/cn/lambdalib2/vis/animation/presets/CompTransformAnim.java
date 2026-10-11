package cn.lambdalib2.vis.animation.presets;

import cn.lambdalib2.vis.CompTransform;
import cn.lambdalib2.vis.animation.Animation;
import cn.lambdalib2.vis.curve.IFittedCurve;
import net.minecraft.world.phys.Vec3;

/**
 * @author WeAthFolD
 */
public class CompTransformAnim extends Animation {
    public class Vec3Anim {
        public IFittedCurve cx, cy, cz;

        Vec3 evaluate(Vec3 old, double t) {
            return new Vec3(
                cx != null ? cx.valueAt(t) : old.x,
                cy != null ? cy.valueAt(t) : old.y,
                cz != null ? cz.valueAt(t) : old.z
            );
        }
    }

    public CompTransform target;

    public Vec3Anim
        animTransform = new Vec3Anim(),
        animPivot = new Vec3Anim(),
        animRotation = new Vec3Anim();

    public IFittedCurve
        curveScale;
    
    public CompTransformAnim(CompTransform _transform) {
        target = _transform;
    }
    
    public CompTransformAnim() {}

    @Override
    public void perform(double timePoint) {
        target.transform = animTransform.evaluate(target.transform, timePoint);
        target.pivotPt = animPivot.evaluate(target.pivotPt, timePoint);
        target.rotation = animRotation.evaluate(target.rotation, timePoint);

        if(curveScale != null)
            target.scale = curveScale.valueAt(timePoint);
    }

}
