package cn.academy.ability.vanilla.generic.client.effect;

import cn.academy.Resources;
import cn.academy.entity.LocalEntity;
import cn.lambdalib2.render.legacy.LegacyMesh;
import cn.lambdalib2.render.legacy.LegacyMeshUtils;
import cn.lambdalib2.render.legacy.SimpleMaterial;
import cn.lambdalib2.util.EntityLook;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.RenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import static cn.lambdalib2.render.legacy.GL11.*;

/**
 * Ported from Scala.
 */
@Environment(EnvType.CLIENT)
public class BloodSprayEffect extends LocalEntity {

    private static Identifier[] tex_grnd, tex_wall;
    private static LegacyMesh mesh;
    private static SimpleMaterial material;

    private static Identifier[] seq(String name, int cnt) {
        Identifier[] ret = new Identifier[cnt];
        for (int x = 0; x < cnt; ++x) {
            ret[x] = Resources.getTexture("effects/blood_spray/" + name + "/" + x);
        }
        return ret;
    }

    private static void initRender() {
        if (mesh != null) return;
        tex_grnd = seq("grnd", 3);
        tex_wall = seq("wall", 3);
        mesh = new LegacyMesh();
        LegacyMeshUtils.createBillboard(mesh, -.5, -.5, .5, .5);
        material = new SimpleMaterial(null);
    }

    private final BlockPos pos;
    public final Direction dir;
    public final int textureID = RandUtils.rangei(0, 10);

    public final double size;
    public final double rotation = RandUtils.ranged(0, 360);
    public final double planeOffsetX, planeOffsetY;

    public BloodSprayEffect(Level world, BlockPos pos, int side) {
        super(world);
        this.pos = pos;
        this.dir = Direction.from3DDataValue(side);
        this.size = RandUtils.ranged(1.1, 1.4) * (side == 0 || side == 1 ? 1.0 : 0.8);
        this.planeOffsetX = rand.nextGaussian() * 0.15;
        this.planeOffsetY = rand.nextGaussian() * 0.15;

        BlockState blockState = world.getBlockState(pos);
        VoxelShape shape = blockState.getShape(world, pos);
        AABB bounds = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();

        double dx = bounds.maxX - bounds.minX,
            dy = bounds.maxY - bounds.minY,
            dz = bounds.maxZ - bounds.minZ;
        double xm = (bounds.minX + bounds.maxX) / 2,
            ym = (bounds.minY + bounds.maxY) / 2,
            zm = (bounds.minZ + bounds.maxZ) / 2;

        setPos(
            pos.getX() + xm + dir.getStepX() * 0.51 * dx,
            pos.getY() + ym + dir.getStepY() * 0.51 * dy,
            pos.getZ() + zm + dir.getStepZ() * 0.51 * dz);

        new EntityLook(dir).applyToEntity(this);
    }

    @Override
    public void onUpdate() {
        if (tickCount > 1200 || world.getBlockState(pos).isAir()) {
            setDead();
        }
    }

    public boolean isWall() {
        return dir == Direction.UP || dir == Direction.DOWN;
    }

    @Override
    public void renderEffect(double x, double y, double z, float partialTicks) {
        initRender();
        Identifier[] list = isWall() ? tex_wall : tex_grnd;
        Identifier texture = list[textureID % list.length];

        material.setTexture(texture);

        RenderUtils.loadTexture(texture);
        glDisable(GL_CULL_FACE);
        glPushMatrix();

        glTranslated(x, y, z);
        glRotatef(-getYRot(), 0, 1, 0);
        glRotatef(-getXRot(), 1, 0, 0);
        glTranslated(planeOffsetX, planeOffsetY, 0);
        glScaled(size, size, size);
        glRotated(rotation, 0, 0, 1);

        mesh.draw(material);

        glPopMatrix();
        glEnable(GL_CULL_FACE);
    }

}
