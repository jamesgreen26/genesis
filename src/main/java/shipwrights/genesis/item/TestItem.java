package shipwrights.genesis.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import shipwrights.genesis.planets.PlanetData;
import shipwrights.genesis.util.PlanetUtil;

import java.util.Optional;

public class TestItem extends Item {
    public TestItem(Properties arg) {
        super(arg);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level arg, Player arg2, InteractionHand arg3) {
        if(!arg.isClientSide && arg3==InteractionHand.MAIN_HAND)
        {
            Optional<PlanetData> data = PlanetUtil.celestialRaycast(arg2.position(),arg2.position().add(arg2.getForward().scale(100000)),arg.getGameTime());
            data.ifPresent(planetData -> arg2.sendSystemMessage(Component.literal("BODY FOUND: " + planetData.dimensionID)));
        }
        return super.use(arg, arg2, arg3);
    }
}
