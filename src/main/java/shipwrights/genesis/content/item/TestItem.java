package shipwrights.genesis.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import shipwrights.genesis.space.SpaceLevel;

public class TestItem extends Item {
    public TestItem(Properties arg) {
        super(arg);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level arg, Player arg2, InteractionHand arg3) {
        if(!arg.isClientSide && arg3==InteractionHand.MAIN_HAND)
        {
            Vec3 v3d = arg2.getForward();

            Vector3d origin = new Vector3d(arg2.position().x,arg2.position().y,arg2.position().z);
            Vector3d direction = new Vector3d(v3d.x,v3d.y,v3d.z);
            SpaceLevel.celestialRaycast(arg.getGameTime(),origin,direction)
                .ifPresent(hit -> arg2.sendSystemMessage(Component.literal("BODY FOUND: " + hit.getCelestial().getID())));
        }
        return super.use(arg, arg2, arg3);
    }
}
