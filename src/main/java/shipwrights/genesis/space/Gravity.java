package shipwrights.genesis.space;

import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;

/// Scales vanilla gravity by the {@linkplain Celestial#mass() mass} of the celestial a dimension represents. Space has
/// no gravity, and dimensions without a celestial keep vanilla gravity.
///
/// Applied to living entities, other falling entities (items, projectiles, falling blocks...), particles and fall damage
/// through mixins and {@link #onLivingFall}.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class Gravity {

    private Gravity() {
    }

    /// Multiplier on vanilla gravity in this level: 0 in space, the celestial's mass on a celestial, otherwise 1
    public static double multiplier(Level level) {
        if (GenesisMod.isSpaceDimension(level)) return 0;
        // Tolerates the registry being absent rather than crashing a tick
        return level.registryAccess().registry(Celestial.REGISTRY_KEY)
                .flatMap(celestials -> celestials.getOptional(level.dimension().location()))
                .map(Celestial::mass)
                .orElse(1.0);
    }

    /// Fall damage counts blocks fallen rather than impact speed, so it's scaled separately to match how hard a fall
    /// lands under this gravity. Scales the distance rather than the damage: impact energy goes with gravity × height,
    /// so this makes a fall equivalent to the vanilla one with the same impact. Scaling damage instead would still hurt
    /// on any fall past the safe height, as vanilla rounds damage up, so even ordinary low-gravity jumps would hurt
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        double multiplier = multiplier(event.getEntity().level());
        if (multiplier != 1) {
            event.setDistance((float) (event.getDistance() * multiplier));
        }
    }
}
