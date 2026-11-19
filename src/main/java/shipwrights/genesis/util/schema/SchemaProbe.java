package shipwrights.genesis.util.schema;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import net.minecraft.resources.ResourceLocation;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.util.CodecRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Supplier;
import java.util.stream.Stream;

/// Works out the {@link Schema} of what a codec reads, by having it decode stand-in values that note how they're read.
///
/// Codecs can't be looked into directly, but decoding asks for each part of the input in turn: a number here, a list
/// there, a field of some name. Every request is answered with a stand-in that records it and lets decoding carry on,
/// so one decode lays out the whole shape. A few more decodes then find the rest:
///
/// - Which fields are required: with every field of an object missing, those the codec reports
///   {@code No key <name>} for, rather than falling back to a default.
/// - Alternatives, as from {@link com.mojang.serialization.Codec#either}: with what was asked for first failing, what
///   the codec asks for instead.
/// - Dispatch types: a {@link CodecRegistry} looked up by the stand-in string reports itself as the dispatch's types,
///   and each type's codec is probed in turn.
///
/// A codec whose values mean more than their shape, such as a colour read as a number, can say so itself with
/// {@link #declare}, and is then taken at its word.
///
/// Validation that the stand-ins fail, such as of ranges or resource locations, doesn't matter: only what's asked for
/// counts.
public final class SchemaProbe {
    /// The string every stand-in string reads as
    public static final ResourceLocation PROBE_ID = GenesisMod.id("__schema_probe__");

    /// Deeper than this, a codec is taken to be recursive, and what's below is left unknown
    private static final int MAX_DEPTH = 16;
    /// How many times to look for further alternatives
    private static final int MAX_UNION_ROUNDS = 4;

    private static final Pattern MISSING_KEY = Pattern.compile("No key (\\S+) in");
    private static final String ELEMENT = "[]";

    private static final ThreadLocal<Pass> CURRENT = new ThreadLocal<>();
    /// Dispatch types being probed, so a type that contains itself doesn't recurse forever
    private static final ThreadLocal<Set<Object>> IN_PROGRESS = ThreadLocal.withInitial(HashSet::new);

    private SchemaProbe() {
    }

    /// The schema of what {@code decoder} reads
    public static Schema infer(Decoder<?> decoder) {
        Info root = new Info();
        run(decoder, new Pass(null, Map.of()), root);

        // Alternatives: fail each part the way it's been asked for so far, and see what's asked for instead
        Set<Pair<List<String>, Set<Kind>>> tried = new HashSet<>();
        for (int round = 0; round < MAX_UNION_ROUNDS; round++) {
            boolean found = false;
            for (Map.Entry<List<String>, Info> entry : root.byPath(List.of()).entrySet()) {
                Info info = entry.getValue();
                if (info.kinds.isEmpty() || info.dispatch != null) continue;
                Set<Kind> failing = EnumSet.copyOf(info.kinds);
                if (!tried.add(Pair.of(entry.getKey(), failing))) continue;
                int before = info.kinds.size();
                run(decoder, new Pass(null, Map.of(entry.getKey(), failing)), root);
                found |= info.kinds.size() > before;
            }
            if (!found) break;
        }

        // Required fields: those reported missing when an object has none
        for (Map.Entry<List<String>, Info> entry : root.byPath(List.of()).entrySet()) {
            Info info = entry.getValue();
            if (!info.kinds.contains(Kind.MAP) || info.dispatch != null) continue;
            Pass pass = new Pass(entry.getKey(), Map.of());
            List<String> errors = run(decoder, pass, null);
            if (!pass.missingMapReached) {
                // Never got that far, so can't tell; better to ask for too much than leave something out
                info.required.addAll(info.children.keySet());
                continue;
            }
            for (String error : errors) {
                Matcher matcher = MISSING_KEY.matcher(error);
                while (matcher.find()) info.required.add(matcher.group(1));
            }
        }

        return root.toSchema();
    }

    /// Decodes stand-ins with {@code decoder}, merging what's asked for into {@code into}, if given
    ///
    /// @return the errors decoding gave, one per message
    private static List<String> run(Decoder<?> decoder, Pass pass, Info into) {
        Node node = new Node(null, null, List.of());
        CURRENT.set(pass);
        List<String> errors = new ArrayList<>();
        try {
            decoder.decode(new Ops(pass), node).error()
                    .ifPresent(error -> errors.addAll(List.of(error.message().split("; "))));
        } catch (RuntimeException e) {
            // A codec that can't cope with stand-ins, such as one validating a value it got as null. What was asked
            // for up to then still counts.
        } finally {
            CURRENT.remove();
        }
        if (into != null) into.merge(node);
        return errors;
    }

    /// Whether {@code key} is the stand-in string, looked up while probing
    public static boolean isProbeId(Object key) {
        return CURRENT.get() != null && PROBE_ID.equals(key);
    }

    /// Notes that the object holding the stand-in string just read is a dispatch between the types in
    /// {@code registry}, keyed by that string's field
    ///
    /// @return a codec reading nothing, so decoding carries on; each type is probed on its own later
    public static <T> MapCodec<T> dispatchedTo(CodecRegistry<T> registry) {
        Pass pass = CURRENT.get();
        if (pass != null && pass.lastString != null && pass.lastString.parent != null) {
            pass.lastString.parent.dispatch = registry;
            pass.lastString.parent.dispatchKey = pass.lastString.key;
        }
        return MapCodec.unit(() -> null);
    }

    /// Says that what {@code input} is read as has the schema {@code schema}, if it's being probed. A codec calls this
    /// first thing when decoding, so it's described by what it means rather than by what it's read as, and returns
    /// some value of its own without reading {@code input} when it's told it is being probed.
    ///
    /// @return whether {@code input} is a stand-in being probed
    public static <T> boolean declare(DynamicOps<T> ops, T input, Schema schema) {
        if (!(ops instanceof Ops) || !(input instanceof Node node)) return false;
        if (!node.tooDeep() && node.declared == null) node.declared = schema;
        return true;
    }

    /// Notes what's used in place of {@code input}, a field's value, when the field's left out, if it's being probed
    ///
    /// @param defaultValue the default as JSON; only asked for while probing
    public static <T> void noteDefault(DynamicOps<T> ops, T input, Supplier<JsonElement> defaultValue) {
        if (!(ops instanceof Ops) || !(input instanceof Node node) || node.tooDeep() || node.defaultValue != null) return;
        node.defaultValue = defaultValue.get();
    }

    private enum Kind { NUMBER, BOOL, STRING, LIST, MAP }

    /// @param missingMap   the path of the object to show as having no fields, if any
    /// @param failingKinds by path, ways of reading that fail
    private static final class Pass {
        final List<String> missingMap;
        final Map<List<String>, Set<Kind>> failingKinds;
        Node lastString;
        boolean missingMapReached;

        Pass(List<String> missingMap, Map<List<String>, Set<Kind>> failingKinds) {
            this.missingMap = missingMap;
            this.failingKinds = failingKinds;
        }

        boolean fails(Node node, Kind kind) {
            return node.path != null && failingKinds.getOrDefault(node.path, Set.of()).contains(kind);
        }
    }

    /// A stand-in, noting how it's read. Those the ops create, rather than hand to the codec, have no path.
    private static final class Node {
        final Node parent;
        final String key;
        final List<String> path;
        final Set<Kind> kinds = new LinkedHashSet<>();
        Schema.NumberKind numberKind;
        final Map<String, Node> children = new LinkedHashMap<>();
        Node element;
        CodecRegistry<?> dispatch;
        String dispatchKey;
        boolean arbitraryKeys;
        Schema declared;
        JsonElement defaultValue;
        /// The value of a string the ops created, such as a field name
        String string;

        Node(Node parent, String key, List<String> path) {
            this.parent = parent;
            this.key = key;
            this.path = path;
        }

        static Node detached() {
            return new Node(null, null, null);
        }

        Node child(String name) {
            return children.computeIfAbsent(name, k -> new Node(this, k, append(path, k)));
        }

        Node element() {
            if (element == null) element = new Node(this, ELEMENT, append(path, ELEMENT));
            return element;
        }

        boolean tooDeep() {
            return path == null || path.size() > MAX_DEPTH;
        }
    }

    private static List<String> append(List<String> path, String segment) {
        List<String> appended = new ArrayList<>(path);
        appended.add(segment);
        return List.copyOf(appended);
    }

    /// What's been found out about one part of the input, across every decode
    private static final class Info {
        final Set<Kind> kinds = new LinkedHashSet<>();
        Schema.NumberKind numberKind;
        final Map<String, Info> children = new LinkedHashMap<>();
        Info element;
        CodecRegistry<?> dispatch;
        String dispatchKey;
        boolean arbitraryKeys;
        /// What the codec reading it said it is, if it did
        Schema declared;
        /// What's used without it, if it's a field that said
        JsonElement defaultValue;
        final Set<String> required = new HashSet<>();

        void merge(Node node) {
            kinds.addAll(node.kinds);
            if (numberKind == null) numberKind = node.numberKind;
            if (dispatch == null) {
                dispatch = node.dispatch;
                dispatchKey = node.dispatchKey;
            }
            arbitraryKeys |= node.arbitraryKeys;
            if (declared == null) declared = node.declared;
            if (defaultValue == null) defaultValue = node.defaultValue;
            node.children.forEach((name, child) -> children.computeIfAbsent(name, k -> new Info()).merge(child));
            if (node.element != null) {
                if (element == null) element = new Info();
                element.merge(node.element);
            }
        }

        Map<List<String>, Info> byPath(List<String> path) {
            Map<List<String>, Info> all = new LinkedHashMap<>();
            all.put(path, this);
            children.forEach((name, child) -> all.putAll(child.byPath(append(path, name))));
            if (element != null) all.putAll(element.byPath(append(path, ELEMENT)));
            return all;
        }

        Schema toSchema() {
            // Declared alone, or after what was asked for first, as with Codec.either
            if (declared != null && kinds.isEmpty()) return declared;
            if (dispatch != null) return dispatchSchema();

            List<Schema> alternatives = new ArrayList<>();
            for (Kind kind : kinds) {
                alternatives.add(switch (kind) {
                    case NUMBER -> new Schema.NumberSchema(numberKind == null ? Schema.NumberKind.DOUBLE : numberKind);
                    case BOOL -> new Schema.BoolSchema();
                    case STRING -> new Schema.StringSchema();
                    case LIST -> new Schema.ListSchema(element == null ? new Schema.UnknownSchema() : element.toSchema());
                    case MAP -> arbitraryKeys && children.isEmpty()
                            ? new Schema.UnknownSchema()
                            : new Schema.MapSchema(children.entrySet().stream()
                                    .map(e -> new Schema.Field(e.getKey(), e.getValue().toSchema(),
                                            required.contains(e.getKey()), e.getValue().defaultValue))
                                    .toList());
                });
            }
            if (declared != null) alternatives.add(declared);
            return switch (alternatives.size()) {
                case 0 -> new Schema.UnknownSchema();
                case 1 -> alternatives.get(0);
                default -> new Schema.UnionSchema(List.copyOf(alternatives));
            };
        }

        private Schema dispatchSchema() {
            Map<ResourceLocation, Schema> options = new LinkedHashMap<>();
            List<? extends Map.Entry<ResourceLocation, ? extends MapCodec<?>>> types = dispatch.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                    .toList();
            for (Map.Entry<ResourceLocation, ? extends MapCodec<?>> type : types) {
                Object token = Pair.of(dispatch, type.getKey());
                if (!IN_PROGRESS.get().add(token)) {
                    options.put(type.getKey(), new Schema.UnknownSchema());
                    continue;
                }
                try {
                    options.put(type.getKey(), infer(type.getValue().codec()));
                } finally {
                    IN_PROGRESS.get().remove(token);
                }
            }
            return new Schema.DispatchSchema(dispatchKey, options);
        }
    }

    /// Stands in for every number, noting which kind the codec takes it as
    private static final class ProbeNumber extends Number {
        private final Node node;

        ProbeNumber(Node node) {
            this.node = node;
        }

        private void note(Schema.NumberKind kind) {
            if (node.numberKind == null) node.numberKind = kind;
        }

        @Override
        public int intValue() {
            note(Schema.NumberKind.INT);
            return 0;
        }

        @Override
        public long longValue() {
            note(Schema.NumberKind.LONG);
            return 0;
        }

        @Override
        public float floatValue() {
            note(Schema.NumberKind.FLOAT);
            return 0;
        }

        @Override
        public double doubleValue() {
            note(Schema.NumberKind.DOUBLE);
            return 0;
        }

        @Override
        public byte byteValue() {
            note(Schema.NumberKind.INT);
            return 0;
        }

        @Override
        public short shortValue() {
            note(Schema.NumberKind.INT);
            return 0;
        }
    }

    private record Ops(Pass pass) implements DynamicOps<Node> {

        private <R> DataResult<R> read(Node input, Kind kind, R value) {
            if (input.tooDeep()) return DataResult.error(() -> "Too deep to probe");
            if (pass.fails(input, kind)) return DataResult.error(() -> "Probe: not a " + kind);
            input.kinds.add(kind);
            return DataResult.success(value);
        }

        @Override
        public Node empty() {
            return Node.detached();
        }

        @Override
        public <U> U convertTo(DynamicOps<U> outOps, Node input) {
            return outOps.empty();
        }

        @Override
        public DataResult<Number> getNumberValue(Node input) {
            return read(input, Kind.NUMBER, new ProbeNumber(input));
        }

        @Override
        public DataResult<Boolean> getBooleanValue(Node input) {
            return read(input, Kind.BOOL, false);
        }

        @Override
        public DataResult<String> getStringValue(Node input) {
            if (input.path == null && input.string != null) return DataResult.success(input.string);
            DataResult<String> result = read(input, Kind.STRING, PROBE_ID.toString());
            if (result.isSuccess()) pass.lastString = input;
            return result;
        }

        @Override
        public DataResult<MapLike<Node>> getMap(Node input) {
            boolean missing = input.path != null && input.path.equals(pass.missingMap);
            if (missing) pass.missingMapReached = true;
            return read(input, Kind.MAP, new MapLike<>() {
                @Override
                public Node get(Node key) {
                    return key.string == null ? null : get(key.string);
                }

                @Override
                public Node get(String key) {
                    return missing ? null : input.child(key);
                }

                @Override
                public Stream<Pair<Node, Node>> entries() {
                    input.arbitraryKeys = true;
                    return Stream.empty();
                }

                @Override
                public String toString() {
                    return "MapLike[probe]";
                }
            });
        }

        @Override
        public DataResult<Stream<Pair<Node, Node>>> getMapValues(Node input) {
            input.arbitraryKeys = true;
            return read(input, Kind.MAP, Stream.empty());
        }

        @Override
        public DataResult<Stream<Node>> getStream(Node input) {
            DataResult<Stream<Node>> result = read(input, Kind.LIST, Stream.empty());
            return result.isSuccess() ? DataResult.success(Stream.of(input.element())) : result;
        }

        @Override
        public Node createNumeric(Number i) {
            return Node.detached();
        }

        @Override
        public Node createString(String value) {
            Node node = Node.detached();
            node.string = value;
            return node;
        }

        @Override
        public Node createMap(Stream<Pair<Node, Node>> map) {
            return Node.detached();
        }

        @Override
        public Node createList(Stream<Node> input) {
            return Node.detached();
        }

        @Override
        public DataResult<Node> mergeToList(Node list, Node value) {
            return DataResult.success(Node.detached());
        }

        @Override
        public DataResult<Node> mergeToMap(Node map, Node key, Node value) {
            return DataResult.success(Node.detached());
        }

        @Override
        public Node remove(Node input, String key) {
            return input;
        }
    }
}
