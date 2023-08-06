package pi_man.receiver.world.item.ammunition;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import pi_man.receiver.client.ReceiverItemRenderer;
import pi_man.receiver.sounds.ModSoundEvents;
import pi_man.receiver.world.item.ModItems;

import java.util.function.Consumer;

public class Ammunition extends Item {

    private final boolean recoils;
    public Ammunition(Properties p_41383_, boolean recoils) {
        super(p_41383_);
        this.recoils = recoils;
    }

    public boolean recoils() {
        return recoils;
    }

    public Ammunition fire(Level level, LivingEntity entity, float deltaTheta, float deltaPhi) {
        level.playSound(entity, entity.blockPosition(), ModSoundEvents.RIFLE_SHOT.get(), SoundSource.MASTER, 1.0f, 1.0f);
        Vec3 start = entity.getEyePosition();
        float rotx = Mth.lerp(10, entity.xRotO, entity.getXRot());
        float roty = Mth.lerp(10, entity.yRotO, entity.getYRot());
        Vec3 dir = new Vec3(new Matrix3f().rotationYXZ(roty * -(float)Math.PI / 180.0f, rotx * (float)Math.PI / 180.0f, 0.0f).rotateZYX(deltaTheta, 0.0f, deltaPhi).transform(new Vector3f(0, 0, 1)));
        Vec3 end = start.add(dir.scale(64.0));

        BlockHitResult blockHitResult = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, entity));
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            end = blockHitResult.getLocation();
        }
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(level, entity, start, end, new AABB(start, end).inflate(1.0), entity1 -> entity1 instanceof LivingEntity, 0.0f);
        if (entityHitResult != null && entityHitResult.getType() == HitResult.Type.ENTITY) {
            entityHitResult.getEntity().invulnerableTime = 0;
            entityHitResult.getEntity().hurt(new DamageSource("bullet").setProjectile(), 50.0f);
        }
        else if (blockHitResult.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = blockHitResult.getLocation();
            Vec3 normal = new Vec3 (blockHitResult.getDirection().getNormal().getX(), blockHitResult.getDirection().getNormal().getY(), blockHitResult.getDirection().getNormal().getZ());
            BlockPos blockPos = blockHitResult.getBlockPos();
            Vec3 pdir = dir.subtract(normal.scale(normal.dot(dir) * 2));
            int n = level.random.nextIntBetweenInclusive(2, 4);
            for (int i = 0; i < n; i++) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(blockPos)), hitPos.x, hitPos.y, hitPos.z, pdir.x, pdir.y, pdir.z);
            }
            SoundType soundType = level.getBlockState(blockPos).getSoundType(level, blockPos, entity);
            level.playSound(entity, blockPos, soundType.getBreakSound(), SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
        }
        for (Vec3 pos = start; pos.subtract(end).lengthSqr() > 0.2f; pos = pos.add(dir.scale(0.1f))) {
            level.addParticle(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 0, 0, 0);
        }

        return ModItems._30_06_Casing.get();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            /**
             * Queries this item's renderer.
             * <p>
             * Only used if {@link BakedModel#isCustomRenderer()} returns {@code true} or {@link BlockState#getRenderShape()}
             * returns {@link RenderShape#ENTITYBLOCK_ANIMATED}.
             * <p>
             * By default, returns vanilla's block entity renderer.
             */
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ReceiverItemRenderer.getInstance();
            }
        });
    }
}
