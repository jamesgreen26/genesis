package shipwrights.genesis.space.client.map.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.util.MoreCodecs;
import shipwrights.genesis.util.schema.Schema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/// How a value in the editor's forms is typed: read from text with brigadier, as a command's argument is, and suggested
/// as it's typed. Each reads the JSON the document has there, and {@linkplain #format writes} it back as text.
///
/// Suggestions are asked for without a command context, which none of these need.
abstract class FieldArgument implements ArgumentType<JsonElement> {
    private static final SimpleCommandExceptionType TRAILING =
            new SimpleCommandExceptionType(Component.translatable("genesis.editor.syntax.trailing"));
    private static final DynamicCommandExceptionType EXPECTED =
            new DynamicCommandExceptionType(hint -> Component.translatable("genesis.editor.syntax.expected", hint));
    private static final Dynamic2CommandExceptionType VECTOR_LENGTH = new Dynamic2CommandExceptionType(
            (count, axes) -> Component.translatable("genesis.editor.syntax.vector", count, axes));
    private static final DynamicCommandExceptionType INVALID_COLOR =
            new DynamicCommandExceptionType(text -> Component.translatable("genesis.editor.syntax.color", text));
    private static final Dynamic2CommandExceptionType NOT_A_CHOICE = new Dynamic2CommandExceptionType(
            (text, choices) -> Component.translatable("genesis.editor.syntax.choice", text, choices));
    /// As vanilla's, which is private here
    private static final SimpleCommandExceptionType INVALID_ID =
            new SimpleCommandExceptionType(Component.translatable("argument.id.invalid"));
    private static final DynamicCommandExceptionType UNKNOWN_TYPE =
            new DynamicCommandExceptionType(id -> Component.translatable("genesis.editor.syntax.unknown_type", id));
    private static final DynamicCommandExceptionType OUT_OF_RANGE =
            new DynamicCommandExceptionType(range -> Component.translatable("genesis.editor.syntax.range", range));
    private static final DynamicCommandExceptionType INVALID_JSON =
            new DynamicCommandExceptionType(message -> Component.translatable("genesis.editor.syntax.json", message));

    /// Reads the whole of {@code text}
    ///
    /// @throws CommandSyntaxException if it isn't one, or there's more after it
    final JsonElement parseAll(String text) throws CommandSyntaxException {
        StringReader reader = new StringReader(text);
        JsonElement value = parse(reader);
        reader.skipWhitespace();
        if (reader.canRead()) throw TRAILING.createWithContext(reader);
        return value;
    }

    /// The suggestions for {@code text}, typed up to where the cursor is
    final CompletableFuture<Suggestions> suggest(String text) {
        return listSuggestions(null, new SuggestionsBuilder(text, text.toLowerCase(Locale.ROOT), 0));
    }

    /// {@code json}, as it'd be typed; for JSON that isn't of this kind, as near as can be
    String format(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull()) return "";
        return json.isJsonPrimitive() ? json.getAsString() : json.toString();
    }

    /// What to type, shown in an empty box
    abstract Component hint();

    /// How wide its box is
    int width() {
        return 110;
    }

    /// Something to point out about a value that'll do as it is, such as an id of something that isn't there yet
    Optional<Component> warning(JsonElement value) {
        return Optional.empty();
    }

    /// What to know about what may be typed, shown while it's typed in when there's nothing else to show, such as the
    /// numbers it may be
    Optional<Component> info() {
        return Optional.empty();
    }

    /// The colour {@code text} means, if it's a colour, to show beside it
    OptionalInt color(String text) {
        return OptionalInt.empty();
    }

    /// Reads the text up to the next space
    private static String readToken(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) reader.skip();
        return reader.getString().substring(start, reader.getCursor());
    }

    // The kinds there are

    /// How a value of {@code schema} is typed, or empty for one made of several values, edited with rows of their own
    ///
    /// @param ids the entries there are in a registry, to suggest for ids of them
    static Optional<FieldArgument> of(Schema schema, Function<ResourceKey<? extends Registry<?>>, List<ResourceLocation>> ids) {
        if (schema instanceof Schema.NumberSchema number) return Optional.of(new NumberArgument(number.kind(), number.range()));
        if (schema instanceof Schema.BoolSchema) return Optional.of(new BoolArgument());
        if (schema instanceof Schema.StringSchema) return Optional.of(new StringArgument());
        if (schema instanceof Schema.ChoiceSchema choice) return Optional.of(new ChoiceArgument(choice.values()));
        if (schema instanceof Schema.ColorSchema) return Optional.of(new ColorArgument());
        if (schema instanceof Schema.VectorSchema vector) return Optional.of(new VectorArgument(vector.axes(), vector.kind()));
        if (schema instanceof Schema.IdSchema id) return Optional.of(new IdArgument(ids.apply(id.registry()), false));
        if (schema instanceof Schema.UnknownSchema) return Optional.of(new JsonArgument());
        if (schema instanceof Schema.UnionSchema union) {
            List<FieldArgument> alternatives = new ArrayList<>();
            for (Schema alternative : union.alternatives()) {
                // Greedy alternatives would read what's meant for those after them; JSON could be anything
                if (alternative instanceof Schema.UnknownSchema) return Optional.empty();
                Optional<FieldArgument> argument = of(alternative, ids);
                if (argument.isEmpty()) return Optional.empty();
                alternatives.add(argument.get());
            }
            return Optional.of(new UnionArgument(alternatives));
        }
        // Lists, maps and dispatches
        return Optional.empty();
    }

    /// A dispatch's type, of those there are
    static FieldArgument dispatchType(Schema.DispatchSchema dispatch) {
        return new IdArgument(List.copyOf(dispatch.options().keySet()), true);
    }

    /// A number, of any size and precision the kind allows, written as JSON writes them, with exponents
    /// A number, of any precision the kind allows, written as JSON writes them, with exponents, and in range
    static final class NumberArgument extends FieldArgument {
        private final Schema.NumberKind kind;
        private final Schema.Range range;

        NumberArgument(Schema.NumberKind kind, Schema.Range range) {
            this.kind = kind;
            this.range = range;
        }

        NumberArgument(Schema.NumberKind kind) {
            this(kind, Schema.Range.ALL);
        }

        @Override
        public JsonElement parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            String token = readToken(reader);
            if (token.isEmpty()) {
                throw (kind.isWhole() ? CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedInt()
                        : CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedDouble()).createWithContext(reader);
            }
            try {
                BigDecimal number = new BigDecimal(token);
                switch (kind) {
                    case INT -> number.intValueExact();
                    case LONG -> number.longValueExact();
                    default -> {
                    }
                }
                if (!range.contains(number)) {
                    reader.setCursor(start);
                    throw OUT_OF_RANGE.createWithContext(reader, range.toString());
                }
                return new JsonPrimitive(number);
            } catch (NumberFormatException | ArithmeticException e) {
                reader.setCursor(start);
                throw switch (kind) {
                    case INT -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(reader, token);
                    case LONG -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidLong().createWithContext(reader, token);
                    default -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidDouble().createWithContext(reader, token);
                };
            }
        }

        @Override
        Component hint() {
            Component kindName = Component.translatable(kind.isWhole() ? "genesis.editor.hint.whole" : "genesis.editor.hint.number");
            return range.isAll() ? kindName : Component.literal(range.toString());
        }

        @Override
        Optional<Component> info() {
            if (range.isAll()) return Optional.empty();
            return Optional.of(Component.translatable("genesis.editor.syntax.range", range.toString()));
        }

        @Override
        int width() {
            return 72;
        }

        @Override
        public List<String> getExamples() {
            return kind.isWhole() ? List.of("0", "-12", "4000") : List.of("0", "1.5", "-0.25", "6e9");
        }
    }

    static final class BoolArgument extends FieldArgument {
        @Override
        public JsonElement parse(StringReader reader) throws CommandSyntaxException {
            return new JsonPrimitive(reader.readBoolean());
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return SharedSuggestionProvider.suggest(List.of("true", "false"), builder);
        }

        @Override
        Component hint() {
            return Component.translatable("genesis.editor.kind.boolean");
        }

        @Override
        int width() {
            return 44;
        }
    }

    /// Any text, spaces and all
    static final class StringArgument extends FieldArgument {
        @Override
        public JsonElement parse(StringReader reader) {
            String text = reader.getRemaining();
            reader.setCursor(reader.getTotalLength());
            return new JsonPrimitive(text);
        }

        @Override
        Component hint() {
            return Component.translatable("genesis.editor.kind.text");
        }
    }

    /// One of a few words
    static final class ChoiceArgument extends FieldArgument {
        private final List<String> values;

        ChoiceArgument(List<String> values) {
            this.values = values;
        }

        @Override
        public JsonElement parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            String token = readToken(reader);
            if (!values.contains(token)) {
                reader.setCursor(start);
                throw NOT_A_CHOICE.createWithContext(reader, token, String.join(", ", values));
            }
            return new JsonPrimitive(token);
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return SharedSuggestionProvider.suggest(values, builder);
        }

        @Override
        Component hint() {
            return Component.literal(String.join(" | ", values));
        }

        @Override
        public List<String> getExamples() {
            return values;
        }
    }

    /// {@code #RRGGBB}, or a dye's name, for its colour; written as {@code #RRGGBB}
    static final class ColorArgument extends FieldArgument {
        @Override
        public JsonElement parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            String token = readToken(reader);
            OptionalInt color = parseColor(token);
            if (color.isEmpty()) {
                reader.setCursor(start);
                throw INVALID_COLOR.createWithContext(reader, token);
            }
            return new JsonPrimitive(MoreCodecs.hexColor(color.getAsInt()));
        }

        static OptionalInt parseColor(String text) {
            String hex = text.startsWith("#") ? text.substring(1) : text;
            if (hex.length() == 3) hex = hex.replaceAll("(.)", "$1$1");
            if (hex.length() == 6 && hex.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
                return OptionalInt.of(Integer.parseInt(hex, 16));
            }
            DyeColor dye = DyeColor.byName(text.toLowerCase(Locale.ROOT), null);
            return dye == null ? OptionalInt.empty() : OptionalInt.of(dye.getTextColor() & 0xFFFFFF);
        }

        @Override
        String format(@Nullable JsonElement json) {
            // Packed numbers too, as files written before colours were text have them
            if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                return MoreCodecs.hexColor(json.getAsInt());
            }
            return super.format(json);
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            // Dyes by name: what's typed is a hex code already once it starts with #
            if (builder.getRemaining().startsWith("#")) return builder.buildFuture();
            for (DyeColor dye : DyeColor.values()) {
                if (SharedSuggestionProvider.matchesSubStr(builder.getRemainingLowerCase(), dye.getSerializedName())) {
                    builder.suggest(dye.getSerializedName(), Component.literal(MoreCodecs.hexColor(dye.getTextColor())));
                }
            }
            return builder.buildFuture();
        }

        @Override
        OptionalInt color(String text) {
            return parseColor(text.trim());
        }

        @Override
        Component hint() {
            return Component.literal("#RRGGBB");
        }

        @Override
        int width() {
            return 60;
        }

        @Override
        public List<String> getExamples() {
            return List.of("#FFD026", "#fff", "light_blue");
        }
    }

    /// Numbers, one per axis, between spaces or commas: {@code 1 0 -2.5}
    static final class VectorArgument extends FieldArgument {
        private final List<String> axes;
        private final NumberArgument number;

        VectorArgument(List<String> axes, Schema.NumberKind kind) {
            this.axes = axes;
            this.number = new NumberArgument(kind);
        }

        @Override
        public JsonElement parse(StringReader reader) throws CommandSyntaxException {
            JsonArray array = new JsonArray();
            for (int i = 0; i < axes.size(); i++) {
                reader.skipWhitespace();
                if (i > 0 && reader.canRead() && reader.peek() == ',') {
                    reader.skip();
                    reader.skipWhitespace();
                }
                if (!reader.canRead()) throw VECTOR_LENGTH.createWithContext(reader, axes.size(), axisNames());
                // Stops at a comma, as well as a space
                int start = reader.getCursor();
                while (reader.canRead() && !Character.isWhitespace(reader.peek()) && reader.peek() != ',') reader.skip();
                StringReader one = new StringReader(reader.getString().substring(start, reader.getCursor()));
                try {
                    array.add(number.parse(one));
                } catch (CommandSyntaxException e) {
                    // Pointing into the whole of what's typed, not just this number
                    throw new CommandSyntaxException(e.getType(), e.getRawMessage(), reader.getString(),
                            start + Math.max(0, e.getCursor()));
                }
            }
            return array;
        }

        private String axisNames() {
            return String.join(" ", axes);
        }

        @Override
        String format(@Nullable JsonElement json) {
            if (json instanceof JsonArray array) {
                List<String> parts = new ArrayList<>();
                array.forEach(element -> parts.add(element.isJsonPrimitive() ? element.getAsString() : element.toString()));
                return String.join(" ", parts);
            }
            return super.format(json);
        }

        @Override
        Component hint() {
            return Component.literal(axisNames());
        }

        @Override
        int width() {
            return 120;
        }

        @Override
        public List<String> getExamples() {
            return List.of(String.join(" ", axes.stream().map(axis -> "0").toList()));
        }
    }

    /// A resource location, with those there are suggested
    ///
    /// @param strict whether only those there are will do; otherwise others are only warned about, as they may be
    ///               made later
    static final class IdArgument extends FieldArgument {
        private final List<ResourceLocation> known;
        private final boolean strict;

        IdArgument(List<ResourceLocation> known, boolean strict) {
            this.known = known;
            this.strict = strict;
        }

        @Override
        public JsonElement parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            ResourceLocation id = ResourceLocation.read(reader);
            // Nothing at all is read as an id with an empty path
            if (reader.getCursor() == start) throw INVALID_ID.createWithContext(reader);
            if (strict && !known.contains(id)) {
                reader.setCursor(start);
                throw UNKNOWN_TYPE.createWithContext(reader, id.toString());
            }
            return new JsonPrimitive(id.toString());
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            // As later versions do: typed without a namespace, it's matched against every id's path too, not only
            // vanilla's
            String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
            boolean namespaced = typed.indexOf(':') >= 0;
            for (ResourceLocation id : known) {
                boolean matches = namespaced ? SharedSuggestionProvider.matchesSubStr(typed, id.toString())
                        : SharedSuggestionProvider.matchesSubStr(typed, id.getNamespace())
                                || SharedSuggestionProvider.matchesSubStr(typed, id.getPath());
                if (matches) builder.suggest(id.toString());
            }
            return builder.buildFuture();
        }

        @Override
        Optional<Component> warning(JsonElement value) {
            if (known.isEmpty()) return Optional.empty();
            ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
            return id == null || known.contains(id) ? Optional.empty()
                    : Optional.of(Component.translatable("genesis.editor.syntax.unknown_id", id.toString()));
        }

        @Override
        Component hint() {
            return Component.literal("namespace:path");
        }
    }

    /// Any JSON, for what the editor can't make out
    static final class JsonArgument extends FieldArgument {
        @Override
        public JsonElement parse(StringReader reader) throws CommandSyntaxException {
            try {
                JsonElement json = JsonParser.parseString(reader.getRemaining());
                reader.setCursor(reader.getTotalLength());
                return json;
            } catch (JsonParseException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                throw INVALID_JSON.createWithContext(reader, String.valueOf(cause.getMessage()));
            }
        }

        @Override
        Component hint() {
            return Component.translatable("genesis.editor.kind.json");
        }

        @Override
        int width() {
            return 120;
        }
    }

    /// The first of several kinds that reads the whole of what's typed, as {@link com.mojang.serialization.Codec#either}
    /// tries them; suggestions of all of them
    static final class UnionArgument extends FieldArgument {
        private final List<FieldArgument> alternatives;

        UnionArgument(List<FieldArgument> alternatives) {
            this.alternatives = alternatives;
        }

        @Override
        public JsonElement parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            CommandSyntaxException first = null;
            for (FieldArgument alternative : alternatives) {
                reader.setCursor(start);
                try {
                    JsonElement value = alternative.parse(reader);
                    StringReader rest = new StringReader(reader);
                    rest.skipWhitespace();
                    // One that stops short might just be the wrong kind: a number that's really the start of a word
                    if (!rest.canRead()) return value;
                } catch (CommandSyntaxException e) {
                    if (first == null) first = e;
                }
            }
            reader.setCursor(start);
            if (first != null && alternatives.size() == 1) throw first;
            throw EXPECTED.createWithContext(reader, hint());
        }

        @Override
        String format(@Nullable JsonElement json) {
            for (FieldArgument alternative : alternatives) {
                String text = alternative.format(json);
                try {
                    alternative.parseAll(text);
                    return text;
                } catch (CommandSyntaxException e) {
                    // Not this one
                }
            }
            return super.format(json);
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            List<CompletableFuture<Suggestions>> all = alternatives.stream()
                    .map(alternative -> alternative.listSuggestions(context, builder.createOffset(builder.getStart())))
                    .toList();
            return CompletableFuture.allOf(all.toArray(CompletableFuture[]::new)).thenApply(done ->
                    Suggestions.merge(builder.getInput(), all.stream().map(CompletableFuture::join).toList()));
        }

        @Override
        Optional<Component> warning(JsonElement value) {
            for (FieldArgument alternative : alternatives) {
                try {
                    alternative.parseAll(alternative.format(value));
                    return alternative.warning(value);
                } catch (CommandSyntaxException e) {
                    // Not this one
                }
            }
            return Optional.empty();
        }

        @Override
        Optional<Component> info() {
            return alternatives.stream().map(FieldArgument::info).flatMap(Optional::stream).findFirst();
        }

        @Override
        OptionalInt color(String text) {
            return alternatives.stream().map(alternative -> alternative.color(text)).filter(OptionalInt::isPresent)
                    .findFirst().orElse(OptionalInt.empty());
        }

        @Override
        Component hint() {
            return Component.literal(alternatives.stream().map(alternative -> alternative.hint().getString())
                    .collect(Collectors.joining(" | ")));
        }

        @Override
        int width() {
            return alternatives.stream().mapToInt(FieldArgument::width).max().orElse(110);
        }

        @Override
        public List<String> getExamples() {
            return alternatives.stream().flatMap(alternative -> alternative.getExamples().stream()).toList();
        }
    }
}
