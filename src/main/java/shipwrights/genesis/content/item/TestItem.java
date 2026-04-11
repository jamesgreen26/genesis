package shipwrights.genesis.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.SpaceLevel;

public class TestItem extends Item {
    public TestItem(Properties arg) {
        super(arg);
    }

    @Override
    public InteractionResult useOn(UseOnContext arg) {
        if(arg.getLevel() instanceof ServerLevel serverLevel && arg.getHand()==InteractionHand.MAIN_HAND)
        {
            ray(serverLevel,arg.getPlayer(),arg.getHand());
        }
        return super.useOn(arg);
    }

    public static void ray(Level arg, Player arg2, InteractionHand arg3)
    {
        if(!arg.isClientSide && arg3==InteractionHand.MAIN_HAND)
        {
            Vec3 v3d = arg2.getForward();

            Vector3d origin = new Vector3d(arg2.position().x,arg2.position().y,arg2.position().z);
            Vector3d direction = new Vector3d(v3d.x,v3d.y,v3d.z);
            Registry<Celestial> registry = GenesisMod.getCelestialRegistry(arg);
            var result = SpaceLevel.celestialRaycast(registry, GenesisMod.getTicks(arg), 0f, origin, direction, celestialType -> true);
            if (result != null) {
                arg2.sendSystemMessage(Component.literal("BODY FOUND: " + registry.getResourceKey(result.getFirst()).orElseThrow().location()));
                Vector3dc pos = result.getFirst().getPosition(GenesisMod.getTicks(arg), registry);
                arg2.sendSystemMessage(Component.literal("Position: " + (int) pos.x() + " " + (int) pos.y() + " " + (int) pos.z()));
            }
        }
    }
}
