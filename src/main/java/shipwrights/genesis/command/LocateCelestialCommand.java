package shipwrights.genesis.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.VantagePoint;

/// {@code /locate celestial <id>}: where a celestial is in space right now, and how far away it is.
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID)
public class LocateCelestialCommand {

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_CELESTIAL = new DynamicCommandExceptionType(id ->
            Component.translatableWithFallback("commands.genesis.locate.celestial.unknown", "There is no celestial with id %s", id));

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        // Brigadier merges this into vanilla's /locate
        event.getDispatcher().register(Commands.literal("locate")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("celestial")
                        .then(Commands.argument("celestial", ResourceKeyArgument.key(Celestial.REGISTRY_KEY))
                                .executes(context -> locate(context.getSource(), getCelestial(context, "celestial"))))));
    }

    private static Holder.Reference<Celestial> getCelestial(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
        ResourceKey<?> key = context.getArgument(argument, ResourceKey.class);
        ResourceKey<Celestial> celestialKey = key.cast(Celestial.REGISTRY_KEY)
                .orElseThrow(() -> ERROR_UNKNOWN_CELESTIAL.create(key.location()));
        return context.getSource().registryAccess().registryOrThrow(Celestial.REGISTRY_KEY).getHolder(celestialKey)
                .orElseThrow(() -> ERROR_UNKNOWN_CELESTIAL.create(celestialKey.location()));
    }

    private static int locate(CommandSourceStack source, Holder.Reference<Celestial> celestial) {
        ServerLevel level = source.getLevel();
        Registry<Celestial> celestials = level.registryAccess().registryOrThrow(Celestial.REGISTRY_KEY);
        Time time = Time.get(level);
        Vector3dc position = celestial.value().transformProvider().getPosition(celestials, time);

        long x = Math.round(position.x()), y = Math.round(position.y()), z = Math.round(position.z());
        // Celestial positions are coordinates in space
        String teleport = "/execute in " + GenesisMod.SPACE + " run tp @s " + x + " " + y + " " + z;
        Component coordinates = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", x, y, z))
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, teleport))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))));
        Component name = Component.literal(celestial.key().location().toString());

        Vec3 sourcePos = source.getPosition();
        Vector3dc sourcePosition = new Vector3d(sourcePos.x, sourcePos.y, sourcePos.z);
        VantagePoint vantagePoint = VantagePoint.get(level, time, sourcePosition);

        // Where the source is in space, if it's anywhere in space at all
        Vector3dc observer;
        if (vantagePoint instanceof VantagePoint.OnCelestial) {
            observer = vantagePoint.getPosition();
        } else if (vantagePoint instanceof VantagePoint.InSpace) {
            observer = sourcePosition;
        } else {
            source.sendSuccess(() -> Component.translatableWithFallback("commands.genesis.locate.celestial.success.no_distance",
                    "%s is at %s", name, coordinates), false);
            return 1;
        }

        int distance = Mth.floor(observer.distance(position));
        source.sendSuccess(() -> Component.translatableWithFallback("commands.genesis.locate.celestial.success",
                "%s is at %s (%s blocks away)", name, coordinates, distance), false);
        return distance;
    }
}
