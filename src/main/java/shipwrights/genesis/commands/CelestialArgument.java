package shipwrights.genesis.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class CelestialArgument implements ArgumentType<CelestialArgument.CelestialSelector> {
    private static final SimpleCommandExceptionType ERROR_EXPECTED_SELECTOR =
            new SimpleCommandExceptionType(Component.literal("Expected celestial selector '@c'"));
    private static final SimpleCommandExceptionType ERROR_UNCLOSED_OPTIONS =
            new SimpleCommandExceptionType(Component.literal("Unclosed celestial selector options"));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_OPTION =
            new DynamicCommandExceptionType(option -> Component.literal("Unknown celestial option '" + option + "'"));
    private static final DynamicCommandExceptionType ERROR_INVALID_ID =
            new DynamicCommandExceptionType(id -> Component.literal("Invalid celestial id '" + id + "'"));
    private static final DynamicCommandExceptionType ERROR_NO_SUCH_CELESTIAL =
            new DynamicCommandExceptionType(id -> Component.literal("No celestial found with id '" + id + "'"));
    private static final SimpleCommandExceptionType ERROR_MULTIPLE_CELESTIALS =
            new SimpleCommandExceptionType(Component.literal("Multiple celestials matched; use @c[id=<id>]"));
    private static final SimpleCommandExceptionType ERROR_TRAILING_CHARACTERS =
            new SimpleCommandExceptionType(Component.literal("Unexpected trailing characters after celestial selector"));

    public static CelestialArgument celestial() {
        return new CelestialArgument();
    }

    public static Celestial getCelestial(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        CelestialSelector selector = context.getArgument(name, CelestialSelector.class);
        List<Celestial> matches = selector.find(context.getSource());
        if (matches.isEmpty()) {
            if (selector.id != null) {
                throw ERROR_NO_SUCH_CELESTIAL.create(selector.id.toString());
            }
            throw ERROR_NO_SUCH_CELESTIAL.create("<any>");
        }
        if (matches.size() > 1) {
            throw ERROR_MULTIPLE_CELESTIALS.create();
        }
        return matches.get(0);
    }

    public static Celestial getCelestial(CommandSourceStack source, String input) throws CommandSyntaxException {
        CelestialSelector selector = parseSelector(input);
        List<Celestial> matches = selector.find(source);
        if (matches.isEmpty()) {
            if (selector.id != null) {
                throw ERROR_NO_SUCH_CELESTIAL.create(selector.id.toString());
            }
            throw ERROR_NO_SUCH_CELESTIAL.create("<any>");
        }
        if (matches.size() > 1) {
            throw ERROR_MULTIPLE_CELESTIALS.create();
        }
        return matches.get(0);
    }

    public static CelestialSelector parseSelector(String input) throws CommandSyntaxException {
        StringReader reader = new StringReader(input);
        CelestialSelector selector = new CelestialArgument().parse(reader);
        if (reader.canRead()) {
            throw ERROR_TRAILING_CHARACTERS.createWithContext(reader);
        }
        return selector;
    }

    public static <S> CompletableFuture<Suggestions> suggest(CommandContext<S> context, SuggestionsBuilder builder) {
        return new CelestialArgument().listSuggestions(context, builder);
    }

    @Override
    public CelestialSelector parse(StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead() || reader.read() != '@') {
            throw ERROR_EXPECTED_SELECTOR.createWithContext(reader);
        }
        if (!reader.canRead() || reader.read() != 'c') {
            throw ERROR_EXPECTED_SELECTOR.createWithContext(reader);
        }

        ResourceLocation id = null;
        if (reader.canRead() && reader.peek() == '[') {
            reader.skip();
            int start = reader.getCursor();
            while (reader.canRead() && reader.peek() != ']') {
                reader.skip();
            }
            if (!reader.canRead()) {
                throw ERROR_UNCLOSED_OPTIONS.createWithContext(reader);
            }
            String content = reader.getString().substring(start, reader.getCursor());
            reader.skip();

            if (!content.isBlank()) {
                String[] parts = content.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    String[] keyValue = trimmed.split("=", 2);
                    if (keyValue.length != 2) {
                        throw ERROR_UNKNOWN_OPTION.create(trimmed);
                    }
                    String key = keyValue[0].trim().toLowerCase(Locale.ROOT);
                    String value = keyValue[1].trim();
                    if (!"id".equals(key)) {
                        throw ERROR_UNKNOWN_OPTION.create(key);
                    }
                    ResourceLocation parsed = ResourceLocation.tryParse(value);
                    if (parsed == null) {
                        throw ERROR_INVALID_ID.create(value);
                    }
                    id = parsed;
                }
            }
        }

        return new CelestialSelector(id);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        if (remaining.isEmpty()) {
            builder.suggest("@c");
            builder.suggest("@c[id=");
            return builder.buildFuture();
        }

        if (!remaining.startsWith("@c")) {
            builder.suggest("@c");
            return builder.buildFuture();
        }

        if (remaining.startsWith("@c[")) {
            String prefix = "@c[";
            String typed = remaining.substring(prefix.length());
            Collection<String> ids = new ArrayList<>();
            if (context.getSource() instanceof SharedSuggestionProvider source) {
                source.registryAccess().registry(GenesisMod.CELESTIALS_KEY)
                        .ifPresent(reg -> reg.keySet().forEach(id -> ids.add(id.toString())));
            }

            if (typed.isEmpty()) {
                builder.suggest("@c[id=");
                SuggestionsBuilder idBuilder = builder.createOffset(builder.getStart() + prefix.length());
                Collection<String> options = new ArrayList<>();
                for (String id : ids) {
                    options.add("id=" + id + "]");
                }
                return SharedSuggestionProvider.suggest(options, idBuilder);
            }

            if (typed.startsWith("id=")) {
                String idPrefix = "@c[id=";
                Collection<String> options = new ArrayList<>();
                for (String id : ids) {
                    options.add(id + "]");
                }
                return SharedSuggestionProvider.suggest(options, builder.createOffset(builder.getStart() + idPrefix.length()));
            }

            if (!typed.contains("=")) {
                builder.suggest("@c[id=");
                SuggestionsBuilder idBuilder = builder.createOffset(builder.getStart() + prefix.length());
                Collection<String> options = new ArrayList<>();
                for (String id : ids) {
                    options.add("id=" + id + "]");
                }
                return SharedSuggestionProvider.suggest(options, idBuilder);
            }

            return builder.buildFuture();
        }

        if (remaining.startsWith("@c[id=")) {
            String prefix = "@c[id=";
            Collection<String> options = new ArrayList<>();
            if (context.getSource() instanceof SharedSuggestionProvider source) {
                source.registryAccess().registry(GenesisMod.CELESTIALS_KEY)
                        .ifPresent(reg -> reg.keySet().forEach(id -> options.add(id.toString())));
            }
            return SharedSuggestionProvider.suggest(options, builder.createOffset(builder.getStart() + prefix.length()));
        }

        if (remaining.startsWith("@c[") && !remaining.contains("=")) {
            builder.suggest("@c[id=");
        }

        return builder.buildFuture();
    }

    public static class CelestialSelector {
        private final @Nullable ResourceLocation id;

        private CelestialSelector(@Nullable ResourceLocation id) {
            this.id = id;
        }

        List<Celestial> find(CommandSourceStack source) {
            Registry<Celestial> registry = source.getServer().registryAccess()
                    .registryOrThrow(GenesisMod.CELESTIALS_KEY);
            List<Celestial> matches = new ArrayList<>();
            if (id != null) {
                Celestial celestial = registry.get(id);
                if (celestial != null) {
                    matches.add(celestial);
                }
                return matches;
            }
            registry.forEach(matches::add);
            return matches;
        }
    }
}
