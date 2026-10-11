package cn.academy.util;
import cn.academy.ability.AbilityContext;
import cn.academy.event.BlockDestroyEvent;
import cn.lambdalib2.util.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import cn.lambdalib2.event.EventHub;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import static cn.lambdalib2.util.VecUtils.add;
import static cn.lambdalib2.util.VecUtils.multiply;
import static cn.lambdalib2.util.VecUtils.subtract;




/**
 * A super boomy ranged ray damage. it starts out a ranged ray in the given position and direction,
 *     and destroy blocks in the path, also damages entities. It takes account of global damage switches.
 * @author WeAthFolD
 */
public class RangedRayDamage {
    
    static final double STEP = 0.9;

    public final Player player;
    public final Level world;
    public EntityLook look;
    public Vec3 pos, dir;
    public final AbilityContext ctx;


    public double range;
    public float totalEnergy; // decrements [hardness of block] when hit a block
    public int maxIncrement = 50;
    public float dropProb = 0.05f;
    
    public Predicate<Entity> entitySelector = EntitySelectors.everything();
    public float startDamage = 10.0f; // ATTN: LINEAR 1.0*startDamage at dist 0; 0.2 * startDamage at maxIncrement
    
    private Vec3 start, slope;
    
    public RangedRayDamage(AbilityContext ctx, double _range, float _energy) {
        this.ctx = ctx;

        pos = ctx.player.position();
        look = new EntityLook(ctx.player);
        dir = look.toVec3();

        this.player = ctx.player;
        this.world = ctx.player.level();
        this.range = _range;
        this.totalEnergy = _energy;

        entitySelector = EntitySelectors.exclude(ctx.player);
    }
    
    /**
     * BOOM!
     */
    public void perform() {
        Set<int[]> processed = new HashSet<>();
        
        float yaw = -MathUtils.PI_F * 0.5f - look.yaw,
                pitch = look.pitch;
        
        start = pos;
        slope = dir;
        
        Vec3 vp0 = new Vec3(0, 0, 1);
        vp0.xRot(pitch);
        vp0.yRot(yaw);
        
        Vec3 vp1 = new Vec3(0, 1, 0);
        vp1.xRot(pitch);
        vp1.yRot(yaw);

        double maxDistance = Double.MAX_VALUE;
        
        /* Apply Entity Damage */ {
            Vec3 v0 = add(start, add(multiply(vp0, -range), multiply(vp1, -range))),
                    v1 = add(start, add(multiply(vp0, range), multiply(vp1, -range))),
                    v2 = add(start, add(multiply(vp0, range), multiply(vp1, range))),
                    v3 = add(start, add(multiply(vp0, -range), multiply(vp1, range))),
                    v4 = add(v0, multiply(slope, maxIncrement)),
                    v5 = add(v1, multiply(slope, maxIncrement)),
                    v6 = add(v2, multiply(slope, maxIncrement)),
                    v7 = add(v3, multiply(slope, maxIncrement));
            AABB aabb = WorldUtils.minimumBounds(v0, v1, v2, v3, v4, v5, v6, v7);

            Predicate<Entity> areaSelector = target -> {
                Vec3 dv = subtract(new Vec3(target.getX(), target.getY(), target.getZ()), start);
                Vec3 proj = dv.cross(slope);
                return proj.length() < range * 1.2;
            };
            List<Entity> targets = WorldUtils.getEntities(world, aabb, entitySelector.and(areaSelector));
            targets.sort((lhs, rhs) -> {
                double dist1 = ctx.player.distanceToSqr(lhs.getX(), lhs.getY(), lhs.getZ());
                double dist2 = ctx.player.distanceToSqr(rhs.getX(), rhs.getY(), rhs.getZ());
                return Double.valueOf(dist1).compareTo(dist2);
            });

            for(Entity e : targets) {
                if (!attackEntity(e)) {
                    maxDistance = e.distanceToSqr(ctx.player);
                    break;
                }
            }
        }

        if(ctx.canBreakBlock(world)) {
            for(double s = -range; s <= range; s += STEP) {
                for(double t = -range; t <= range; t += STEP) {
                    double rr = range * RandUtils.ranged(0.9, 1.1);

                    if(s * s + t * t > rr * rr)
                        continue;
                    
                    Vec3 pos = add(start,
                        add(
                            multiply(vp0, s),
                            multiply(vp1, t)));
                    
                    //int[] coords = { (int) pos.x, (int) pos.y, (int) pos.z };
                    int[] coords = { (int)Math.floor( pos.x), (int)Math.floor( pos.y), (int)Math.floor( pos.z)};
                    if(processed.contains(coords))
                        continue;
                    
                    processed.add(coords);
                }
            }
            
            float ave = totalEnergy / processed.size();
            for(int[] coords : processed) {
                processLine(coords[0], coords[1], coords[2], 
                    slope, ave * RandUtils.rangef(0.95f, 1.05f), maxDistance);
            }
        }
        

    }
    
    private void processLine(int x0, int y0, int z0, Vec3 slope, float energy, double maxDistSq) {
        Plotter plotter = new Plotter(x0, y0, z0, slope.x, slope.y, slope.z);
        int incrs = 0;
        for(int i = 0; i <= maxIncrement && energy > 0; ++i) {
            ++incrs;
            int[] coords = plotter.next();
            int x = coords[0], y = coords[1], z = coords[2];
            BlockPos pos = new BlockPos(x, y, z);

            int dx = x0 - x, dy = y0 - y, dz = z0 - z;
            int dsq = dx*dx + dy*dy + dz*dz;
            if (dsq > maxDistSq) break;

            boolean snd = incrs < 20;
            
            energy = destroyBlock(energy, pos, snd);
            
            if(RandUtils.ranged(0, 1) < 0.05) {
                Direction dir = Direction.values()[RandUtils.rangei(0, 6)];
                energy = destroyBlock(energy, pos.relative(dir), snd);
            }
        }
    }
    
    private float destroyBlock(float energy, BlockPos pos, boolean snd) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        float hardness = blockState.getDestroySpeed(world, pos);
        if(hardness < 0)
            hardness = 233333;
        if(!EventHub.BUS.post(new BlockDestroyEvent(player, pos)) && energy >= hardness) {
            if(!blockState.isAir()) {
                if (world instanceof ServerLevel sl && RandUtils.ranged(0, 1) < dropProb)
                    Block.dropResources(blockState, sl, pos);

                if(snd && RandUtils.ranged(0, 1) < 0.1) {
                    SoundType st = blockState.getSoundType();
                    SoundEvent breakSnd = st.getBreakSound();
                    world.playSound(null,
                        pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F,
                        breakSnd,
                        SoundSource.BLOCKS,
                        (st.getVolume() + 1.0F) / 2.0F,
                        st.getPitch());
                }
            }
            world.removeBlock(pos, false);
            return energy - hardness;
        }
        return 0;
    }

    protected boolean attackEntity(Entity target) {
        Vec3 dv = subtract(new Vec3(target.getX(), target.getY(), target.getZ()), start);
        float dist = Math.min(maxIncrement, (float) dv.cross(slope).length());
        
        float realDmg = this.startDamage * MathUtils.lerpf(1, 0.2f, dist / maxIncrement);
        return applyAttack(target, realDmg);
    }

    protected boolean applyAttack(Entity target, float damage) {
        ctx.attack(target, damage);
        return true;
    }

    public static class Reflectible extends RangedRayDamage {

        public final Consumer<Entity> callback;

        public Reflectible(AbilityContext ctx,
                           double _range, float _energy, Consumer<Entity> callback) {
            super(ctx, _range, _energy);
            this.callback = callback;
        }

        @Override
        protected boolean applyAttack(Entity target, float damage) {
            boolean[] result = new boolean[] { true }; // for lambda modification
            ctx.attackReflect(target, damage, () -> {
                callback.accept(target);
                result[0] = false;
            });
            return result[0];
        }

    }

}