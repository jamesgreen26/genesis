package shipwrights.genesis.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

public class ZapBubbleParticle extends TextureSheetParticle {
    private static final float ACCELERATION_SCALE = 0.0025F;
    private int INITIAL_LIFETIME = 300;
    private static final int CURVE_ENDPOINT_TIME = 300;
    private static final float FALL_ACC = 0.25F;
    private static final float WIND_BIG = 2.0F;
    private float rotSpeed;
    private final float particleRandom;
    private final float spinAcceleration;


    protected ZapBubbleParticle(ClientLevel arg, double d, double e, double f, SpriteSet arg2) {
        super(arg, d, e, f);
        this.setSprite(arg2.get(this.random.nextInt(12), 12));
        this.rotSpeed = (float)Math.toRadians(this.random.nextBoolean() ? (double)-30.0F : (double)30.0F);
        this.particleRandom = this.random.nextFloat();
        this.spinAcceleration = (float)Math.toRadians(this.random.nextBoolean() ? (double)-5.0F : (double)5.0F);
        INITIAL_LIFETIME -= (int) Math.floor(particleRandom*50);
        this.lifetime = INITIAL_LIFETIME;
        this.gravity = -7.5E-4F;
        float g = this.random.nextBoolean() ? 0.4F : 0.5F;
        this.quadSize = g;
        this.setSize(g, g);
        this.friction = 1.0F;
    }

    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public int getLightColor(float partialTicks) {
        return 0xF000F0; // full bright (same value used by glow textures)
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.lifetime-- <= 0) {
            spawnChild();
            this.remove();
        }

        if (!this.removed) {
            float f = (float)(INITIAL_LIFETIME - this.lifetime);
            float g = Math.min(f / INITIAL_LIFETIME, 1.0F);
            double d = Math.cos(Math.toRadians((double)(this.particleRandom * 60.0F))) * (double)2.0F * Math.pow((double)g, (double)1.25F);
            double e = Math.sin(Math.toRadians((double)(this.particleRandom * 60.0F))) * (double)2.0F * Math.pow((double)g, (double)1.25F);
            this.xd += d * (double)0.00025F;
            this.zd += e * (double)0.00025F;
            this.yd -= (double)this.gravity;
            this.rotSpeed += this.spinAcceleration / 20.0F;
            this.oRoll = this.roll;
            this.roll += this.rotSpeed / 20.0F;
            this.move(this.xd, this.yd, this.zd);
            if (this.onGround || this.lifetime < 299 && (this.xd == (double)0.0F || this.zd == (double)0.0F)) {
                spawnChild();
                this.remove();
            }

            if (!this.removed) {
                this.xd *= (double)this.friction;
                this.yd *= (double)this.friction;
                this.zd *= (double)this.friction;
            }
        }

    }

    private void spawnChild() {
        // spawn the new particle at this particle's final position
        // (replace ModParticles.CHILD with your actual registry object)
        level.addParticle(
                ParticleTypes.ELECTRIC_SPARK,  // or ParticleTypes.FLAME, etc.
                this.x,
                this.y,
                this.z,
                0.0, 0.0, 0.0              // velocity of the child particle
        );
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet arg) {
            this.sprite = arg;
        }

        public Particle createParticle(SimpleParticleType arg, ClientLevel arg2, double d, double e, double f, double g, double h, double i) {
            ZapBubbleParticle zap_bubble_particle = new ZapBubbleParticle(arg2, d, e, f, sprite);
            zap_bubble_particle.pickSprite(this.sprite);
            return zap_bubble_particle;
        }
    }
}

