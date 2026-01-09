package shipwrights.dataplanets.items;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import shipwrights.dataplanets.systemCreation.PlanetSource;
import shipwrights.dataplanets.systemCreation.SystemCreator;
import shipwrights.dataplanets.runtimeRegistration.ServerPhase;

public class TestPlanetCreationItem extends Item {
    public TestPlanetCreationItem(Properties arg) {
        super(arg);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level arg, Player arg2, InteractionHand arg3) {
        if (arg instanceof ServerLevel level) {
            SystemCreator creator = new SystemCreator();
            SystemCreator.SystemCreationContext context = new SystemCreator.SystemCreationContext(level.getServer(), true, ServerPhase.running);

            PlanetSource source = PlanetSource.createRandom(context.nextPlanetName(), context.random);
            creator.createPlanet(source, context);

        }
        return InteractionResultHolder.success(arg2.getItemInHand(arg3));
    }
}
