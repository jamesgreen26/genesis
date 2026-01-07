package shipwrights.genesis.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class VerditeParticle extends TextureSheetParticle {
    private static final float ACCELERATION_SCALE = 0.0025F;
    private int INITIAL_LIFETIME = 300;
    private static final int CURVE_ENDPOINT_TIME = 300;
    private static final float FALL_ACC = 0.25F;
    private static final float WIND_BIG = 2.0F;
    private float rotSpeed;
    private final float particleRandom;

    protected VerditeParticle(ClientLevel arg, double d, double e, double f, SpriteSet arg2) {
        super(arg, d, e, f);
        this.setSprite(arg2.get(this.random.nextInt(4), 4));
        this.rotSpeed = (float)Math.toRadians(this.random.nextBoolean() ? (double)-30.0F : (double)30.0F);
        this.particleRandom = this.random.nextFloat();
        INITIAL_LIFETIME += (int) Math.floor(this.random.nextFloat()*50);
        this.lifetime = INITIAL_LIFETIME;
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
            this.remove();
        }

        if (!this.removed) {
            float f = (float)(INITIAL_LIFETIME - this.lifetime);
            float g = Math.min(f / INITIAL_LIFETIME, 1.0F);
            double d = Math.cos(Math.toRadians((double)((this.particleRandom-0.5) * 60.0F))) * (double)2.0F * Math.pow((double)g, (double)1.25F);
            double e = Math.sin(Math.toRadians((double)((this.particleRandom-0.5) * 60.0F))) * (double)2.0F * Math.pow((double)g, (double)1.25F);
            double h = Math.sin(Math.toRadians((double)((this.particleRandom-0.5) * 60.0F))) * (double)2.0F * Math.pow((double)g, (double)1.25F);
            this.xd += d * (double)0.00025F;
            this.zd += e * (double)0.00025F;
            this.yd += h * (double)0.00025F;
            this.move(this.xd, this.yd, this.zd);

            if (!this.removed) {
                this.xd *= (double)this.friction;
                this.yd *= (double)this.friction;
                this.zd *= (double)this.friction;
            }
        }

    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet arg) {
            this.sprite = arg;
        }

        public Particle createParticle(SimpleParticleType arg, ClientLevel arg2, double d, double e, double f, double g, double h, double i) {
            VerditeParticle verdite_particle = new VerditeParticle(arg2, d, e, f, sprite);
            verdite_particle.pickSprite(this.sprite);
            return verdite_particle;
        }
    }
}
