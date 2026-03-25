package shipwrights.genesis.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.Registry;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.commands.CelestialArgument;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.teleportation.impl.EntityTeleporter;

import java.util.Collection;
import java.util.List;

@Mixin(TeleportCommand.class)
public class TeleportCommandMixin {
    @Unique
    private static final com.mojang.brigadier.exceptions.SimpleCommandExceptionType ERROR_SPACE_NOT_LOADED =
            new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Space dimension not loaded"));

    @Inject(method = "register", at = @At("TAIL"))
    private static void registerCelestialTeleport(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo ci) {
        RootCommandNode<CommandSourceStack> root = dispatcher.getRoot();
        LiteralCommandNode<CommandSourceStack> teleportNode = genesis$getLiteral(root, "teleport");
        if (teleportNode != null) {
            genesis$addCelestialBranches(teleportNode);
        }
        LiteralCommandNode<CommandSourceStack> tpNode = genesis$getLiteral(root, "tp");
        if (tpNode != null && tpNode.getRedirect() == null) {
            genesis$addCelestialBranches(tpNode);
        }
    }

    @Unique
    private static LiteralCommandNode<CommandSourceStack> genesis$getLiteral(CommandNode<CommandSourceStack> root, String name) {
        CommandNode<CommandSourceStack> node = root.getChild(name);
        if (node instanceof LiteralCommandNode<CommandSourceStack> literal) {
            return literal;
        }
        return null;
    }

    @Unique
    private static void genesis$addCelestialBranches(LiteralCommandNode<CommandSourceStack> base) {
        ArgumentBuilder<CommandSourceStack, ?> celestialDestination = Commands.argument("celestial", CelestialArgument.celestial())
                .executes(ctx -> genesis$teleportSelfToCelestial(ctx.getSource(), CelestialArgument.getCelestial(ctx, "celestial")));
        base.addChild(celestialDestination.build());

        CommandNode<CommandSourceStack> targetsNode = base.getChild("targets");
        if (targetsNode == null) {
            ArgumentBuilder<CommandSourceStack, ?> targetsBuilder = Commands.argument("targets", EntityArgument.entities());
            targetsNode = targetsBuilder.build();
            base.addChild(targetsNode);
        }

        ArgumentBuilder<CommandSourceStack, ?> targetsToCelestial = Commands.argument("celestial", CelestialArgument.celestial())
                .executes(ctx -> genesis$teleportTargetsToCelestial(
                        ctx.getSource(),
                        EntityArgument.getEntities(ctx, "targets"),
                        CelestialArgument.getCelestial(ctx, "celestial")
                ));
        targetsNode.addChild(targetsToCelestial.build());
    }

    @Unique
    private static int genesis$teleportSelfToCelestial(CommandSourceStack source, Celestial celestial) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Entity entity = source.getEntityOrException();
        return genesis$teleportTargetsToCelestial(source, List.of(entity), celestial);
    }

    @Unique
    private static int genesis$teleportTargetsToCelestial(
            CommandSourceStack source,
            Collection<? extends Entity> targets,
            Celestial celestial
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerLevel spaceLevel = genesis$getSpaceLevel(source);
        Vec3 pos = genesis$toVec3(celestial, spaceLevel);

        for (Entity entity : targets) {
            EntityTeleporter.teleportEntityAndPassengers(entity, spaceLevel, pos);
        }

        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(spaceLevel);
        String celestialId = registry.getResourceKey(celestial).orElseThrow().location().toString();

        if (targets.size() == 1) {
            Entity target = targets.iterator().next();
            source.sendSuccess(() -> Component.literal(
                    "Teleported " + target.getName().getString() + " to celestial " + celestialId
            ), true);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "Teleported " + targets.size() + " entities to celestial " + celestialId
            ), true);
        }

        return targets.size();
    }

    @Unique
    private static ServerLevel genesis$getSpaceLevel(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ResourceKey<Level> spaceKey = ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM);
        ServerLevel level = source.getServer().getLevel(spaceKey);
        if (level == null) {
            throw ERROR_SPACE_NOT_LOADED.create();
        }
        return level;
    }

    @Unique
    private static Vec3 genesis$toVec3(Celestial celestial, ServerLevel spaceLevel) {
        long ticks = GenesisMod.getTicks(spaceLevel);
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(spaceLevel);
        Vector3dc pos = celestial.getPosition(ticks, registry);
        return new Vec3(pos.x(), pos.y() + celestial.getActualSize(), pos.z());
    }
}
