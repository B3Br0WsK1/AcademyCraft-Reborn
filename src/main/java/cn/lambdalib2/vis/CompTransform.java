/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib2.vis;

import cn.lambdalib2.s11n.SerializeType;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.VecUtils;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.render.legacy.GL11;

@SerializeType
public class CompTransform {

    public static final CompTransform identity = new CompTransform();

    public Vec3 transform = Vec3.ZERO;
    
    public Vec3 pivotPt = Vec3.ZERO;
    
    public Vec3 rotation = Vec3.ZERO;
    
    public double scale = 1.0;
    
    public CompTransform setPivot(double x, double y, double z) {
        pivotPt = new Vec3(x, y, z);
        return this;
    }
    
    public CompTransform setTransform(double x, double y, double z) {
        transform = new Vec3(x, y, z);
        return this;
    }
    
    public CompTransform setRotation(double x, double y, double z) {
        rotation = new Vec3(x, y, z);
        return this;
    }
    
    public CompTransform setScale(double val) {
        scale = val;
        return this;
    }
    
    /**
     * @return The transform as a matrix (same as {@link #doTransform()}).
     */
    public org.joml.Matrix4f toMatrix() {
        org.joml.Matrix4f m = new org.joml.Matrix4f();
        m.translate((float) (transform.x + pivotPt.x), (float) (transform.y + pivotPt.y), (float) (transform.z + pivotPt.z));
        m.rotateX((float) Math.toRadians(rotation.x));
        m.rotateY((float) Math.toRadians(rotation.y));
        m.rotateZ((float) Math.toRadians(rotation.z));
        m.scale((float) scale);
        m.translate((float) -pivotPt.x, (float) -pivotPt.y, (float) -pivotPt.z);
        return m;
    }

    @net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    public void doTransform() {
        RenderUtils.glTranslate(VecUtils.add(transform, pivotPt));

        GL11.glRotated(rotation.x, 1, 0, 0);
        GL11.glRotated(rotation.y, 0, 1, 0);
        GL11.glRotated(rotation.z, 0, 0, 1);
        
        GL11.glScaled(scale, scale, scale);
        
        RenderUtils.glTranslate(VecUtils.neg(pivotPt));
    }
    
}
