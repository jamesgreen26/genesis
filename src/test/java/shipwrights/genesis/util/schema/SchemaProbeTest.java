package shipwrights.genesis.util.schema;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.MinecraftBootstrap;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.OrbitingTransformProvider;
import shipwrights.genesis.space.StaticTransformProvider;
import shipwrights.genesis.util.CodecRegistry;
import shipwrights.genesis.util.MoreCodecs;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaProbeTest {
    private static final ResourceLocation STATIC = GenesisMod.id("static");
    private static final ResourceLocation ORBITING = GenesisMod.id("orbiting");

    @BeforeAll
    static void setUp() {
        MinecraftBootstrap.bootstrap();
        Celestial.TransformProvider.REGISTRY.putIfAbsent(STATIC, StaticTransformProvider.CODEC);
        Celestial.TransformProvider.REGISTRY.putIfAbsent(ORBITING, OrbitingTransformProvider.CODEC);
    }

    private static Schema.Field field(Schema schema, String name) {
        Schema.MapSchema map = assertInstanceOf(Schema.MapSchema.class, schema);
        return map.fields().stream().filter(f -> f.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("No field " + name + " in " + map));
    }

    @Test
    void celestialFieldsInOrderWithRequiredAndOptional() {
        Schema schema = SchemaProbe.infer(Celestial.CODEC);
        Schema.MapSchema map = assertInstanceOf(Schema.MapSchema.class, schema);
        assertEquals(List.of("castsLight", "castsShadows", "size", "mass", "surfaceTemperature", "heatFalloff",
                "transformProvider"), map.fields().stream().map(Schema.Field::name).toList());

        assertEquals(new Schema.BoolSchema(false), field(schema, "castsLight").schema());
        assertEquals(new Schema.BoolSchema(true), field(schema, "castsShadows").schema());
        assertEquals(new Schema.NumberSchema(Schema.NumberKind.DOUBLE, Schema.Range.above(0)), field(schema, "size").schema());
        assertTrue(field(schema, "size").required());
        assertTrue(field(schema, "mass").required());
        assertFalse(field(schema, "surfaceTemperature").required());
        assertFalse(field(schema, "heatFalloff").required());
        assertTrue(field(schema, "transformProvider").required());
    }

    @Test
    void transformProviderDispatchListsEveryRegisteredType() {
        Schema.DispatchSchema dispatch = assertInstanceOf(Schema.DispatchSchema.class,
                field(SchemaProbe.infer(Celestial.CODEC), "transformProvider").schema());
        assertEquals("type", dispatch.typeKey());
        assertTrue(dispatch.options().keySet().containsAll(List.of(STATIC, ORBITING)));

        Schema position = field(dispatch.options().get(STATIC), "position").schema();
        assertEquals(new Schema.VectorSchema(List.of("x", "y", "z"), Schema.NumberKind.DOUBLE), position);

        Schema orbiting = dispatch.options().get(ORBITING);
        assertEquals(new Schema.IdSchema(Celestial.REGISTRY_KEY, GenesisMod.id("sun")), field(orbiting, "parent").schema());
        assertTrue(field(orbiting, "parent").required());
        assertFalse(field(orbiting, "eccentricity").required());
        Schema.Field rotation = field(orbiting, "rotation");
        assertFalse(rotation.required());
        Schema.UnionSchema dayLength = assertInstanceOf(Schema.UnionSchema.class, field(rotation.schema(), "dayLength").schema());
        assertEquals(List.of(new Schema.NumberSchema(Schema.NumberKind.DOUBLE, Schema.Range.nonZero(), java.math.BigDecimal.valueOf(24000.0)),
                        new Schema.ChoiceSchema(List.of("tidally_locked"))),
                dayLength.alternatives());
        Schema.NumberSchema period = assertInstanceOf(Schema.NumberSchema.class, field(orbiting, "period").schema());
        assertEquals(Schema.Range.nonZero(), period.range());
    }

    @Test
    void codecsDeclareWhatTheirValuesMean() {
        Codec<Tinted> codec = RecordCodecBuilder.create(i -> i.group(
                MoreCodecs.RGB.fieldOf("color").forGetter(Tinted::color),
                MoreCodecs.strictOptionalField(MoreCodecs.RGB, "glow", 0).forGetter(Tinted::glow)
        ).apply(i, Tinted::new));
        Schema schema = SchemaProbe.infer(codec);
        assertEquals(new Schema.ColorSchema(), field(schema, "color").schema());
        assertTrue(field(schema, "color").required());
        assertEquals(new Schema.ColorSchema(), field(schema, "glow").schema());
        assertFalse(field(schema, "glow").required());
    }

    @Test
    void colorsReadNumbersAndTextAndWriteText() {
        var ops = com.mojang.serialization.JsonOps.INSTANCE;
        assertEquals(0xFFD026, MoreCodecs.RGB.parse(ops, new com.google.gson.JsonPrimitive(0xFFD026)).result().orElseThrow());
        assertEquals(0xFFD026, MoreCodecs.RGB.parse(ops, new com.google.gson.JsonPrimitive("#ffd026")).result().orElseThrow());
        assertTrue(MoreCodecs.RGB.parse(ops, new com.google.gson.JsonPrimitive("#ffd02")).error().isPresent());
        assertEquals(new com.google.gson.JsonPrimitive("#FFD026"), MoreCodecs.RGB.encodeStart(ops, 0xFFD026).result().orElseThrow());
    }

    private record Tinted(int color, int glow) {}

    @Test
    void optionalFieldsSayWhatTheyDefaultTo() {
        Schema.DispatchSchema dispatch = assertInstanceOf(Schema.DispatchSchema.class,
                field(SchemaProbe.infer(Celestial.CODEC), "transformProvider").schema());
        Schema orbiting = dispatch.options().get(ORBITING);
        assertEquals(new com.google.gson.JsonPrimitive(0.0), field(orbiting, "eccentricity").defaultValue());
        assertEquals(null, field(orbiting, "distance").defaultValue());

        // Whole values too, as their codecs write them
        JsonElement rotation = field(orbiting, "rotation").defaultValue();
        assertEquals("tidally_locked", rotation.getAsJsonObject().get("dayLength").getAsString());

        Codec<Tinted> codec = RecordCodecBuilder.create(i -> i.group(
                MoreCodecs.RGB.fieldOf("color").forGetter(Tinted::color),
                MoreCodecs.strictOptionalField(MoreCodecs.RGB, "glow", 0xFF8000).forGetter(Tinted::glow)
        ).apply(i, Tinted::new));
        assertEquals(new com.google.gson.JsonPrimitive("#FF8000"), field(SchemaProbe.infer(codec), "glow").defaultValue());
    }

    @Test
    void choicesAcceptOnlyTheirWords() {
        var ops = com.mojang.serialization.JsonOps.INSTANCE;
        Codec<String> choice = MoreCodecs.oneOf("left", "right");
        assertEquals(new Schema.ChoiceSchema(List.of("left", "right")), SchemaProbe.infer(choice));
        assertEquals("right", choice.parse(ops, new com.google.gson.JsonPrimitive("right")).result().orElseThrow());
        assertTrue(choice.parse(ops, new com.google.gson.JsonPrimitive("up")).error().isPresent());
    }

    @Test
    void rangedNumbersDeclareTheirRangeAndCheckIt() {
        Schema.Field heatFalloff = field(SchemaProbe.infer(Celestial.CODEC), "heatFalloff");
        assertEquals(new Schema.NumberSchema(Schema.NumberKind.DOUBLE, Schema.Range.above(0)), heatFalloff.schema());
        assertFalse(heatFalloff.required());

        var ops = com.mojang.serialization.JsonOps.INSTANCE;
        Codec<Integer> percent = MoreCodecs.intRange(Schema.Range.between(0, 100));
        assertEquals(new Schema.NumberSchema(Schema.NumberKind.INT, Schema.Range.between(0, 100)), SchemaProbe.infer(percent));
        assertEquals(100, percent.parse(ops, new com.google.gson.JsonPrimitive(100)).result().orElseThrow());
        assertTrue(percent.parse(ops, new com.google.gson.JsonPrimitive(101)).error().isPresent());
        assertTrue(MoreCodecs.doubleRange(Schema.Range.above(0)).parse(ops, new com.google.gson.JsonPrimitive(0)).error().isPresent());
    }

    @Test
    void rangesSeedInsideThemselves() {
        assertEquals(0, Schema.Range.ALL.seed().signum());
        assertEquals(0, Schema.Range.between(-1, 1).seed().signum());
        assertEquals(1, Schema.Range.above(0).seed().intValueExact());
        assertEquals(5, Schema.Range.atLeast(5).seed().intValueExact());
        assertTrue(Schema.Range.from(0.5, 0.75).contains(Schema.Range.from(0.5, 0.75).seed()));
        Schema.Range narrow = new Schema.Range(new java.math.BigDecimal("0.1"), false, new java.math.BigDecimal("0.2"), false);
        assertTrue(narrow.contains(narrow.seed()));
        Schema.Range negative = new Schema.Range(null, false, new java.math.BigDecimal("-3"), false);
        assertEquals(-4, negative.seed().intValueExact());

        assertEquals("> 0", Schema.Range.above(0).toString());
        assertEquals("0 to 1", Schema.Range.between(0, 1).toString());
        assertEquals("≥ 0 and < 1", Schema.Range.from(0, 1).toString());

        Schema.Range nonZero = Schema.Range.nonZero();
        assertFalse(nonZero.contains(0.0));
        assertTrue(nonZero.contains(-5.0));
        assertEquals(1, nonZero.seed().intValueExact());
        assertEquals("≠ 0", nonZero.toString());
        assertEquals("≥ 0 and ≠ 0 and ≠ 1", Schema.Range.atLeast(0).excluding(0).excluding(1).toString());
        assertEquals(2, Schema.Range.atLeast(0).excluding(0).excluding(1).seed().intValueExact());
    }

    @Test
    void newlyRegisteredTypesAppearWithoutChangesElsewhere() {
        CodecRegistry<Thing> registry = new CodecRegistry<>();
        Codec<Thing> dispatch = ResourceLocation.CODEC.dispatchStable(Thing::type, registry::get);
        Codec<List<Thing>> things = dispatch.listOf();
        ResourceLocation colored = GenesisMod.id("colored");
        registry.put(colored, Colored.CODEC.codec());

        Schema.ListSchema list = assertInstanceOf(Schema.ListSchema.class, SchemaProbe.infer(things));
        Schema.DispatchSchema before = assertInstanceOf(Schema.DispatchSchema.class, list.element());
        assertEquals(List.of(colored), List.copyOf(before.options().keySet()));
        assertEquals(new Schema.NumberSchema(Schema.NumberKind.INT), field(before.options().get(colored), "color").schema());

        ResourceLocation sized = GenesisMod.id("sized");
        registry.put(sized, Sized.CODEC.codec());
        Schema.DispatchSchema after = assertInstanceOf(Schema.DispatchSchema.class,
                ((Schema.ListSchema) SchemaProbe.infer(things)).element());
        assertEquals(List.of(colored, sized), List.copyOf(after.options().keySet()));
        assertFalse(field(after.options().get(sized), "size").required());
    }

    @Test
    void newOrbitsStartWithADayLongPeriod() {
        Schema.DispatchSchema schema = assertInstanceOf(Schema.DispatchSchema.class,
                SchemaProbe.infer(Celestial.TransformProvider.DISPATCH_CODEC));
        JsonObject json = SchemaSeeds.seedDispatch(schema, ORBITING.toString());
        assertEquals(24000, json.get("period").getAsInt());
        assertEquals("genesis:sun", json.get("parent").getAsString());
    }

    @Test
    void fittedSeedsDecode() {
        Codec<Celestial.TransformProvider> codec = Celestial.TransformProvider.DISPATCH_CODEC;
        Schema.DispatchSchema schema = assertInstanceOf(Schema.DispatchSchema.class, SchemaProbe.infer(codec));
        JsonObject json = SchemaSeeds.seedDispatch(schema, STATIC.toString());
        SchemaSeeds.fit(codec, schema, json);

        assertEquals(3, json.getAsJsonArray("position").size());
        assertEquals(0, SchemaSeeds.errors(codec, json), () -> codec.parse(com.mojang.serialization.JsonOps.INSTANCE, json).toString());
    }

    @Test
    void fixedLengthListsInsideListsAreFitted() {
        Codec<List<List<org.joml.Vector3dc>>> codec = MoreCodecs.VECTOR3DC.listOf().listOf();
        Schema schema = SchemaProbe.infer(codec);
        JsonElement json = SchemaSeeds.fitted(codec, schema);
        // An empty outer list already decodes, so nothing needs to be inside it
        assertEquals(0, SchemaSeeds.errors(codec, json));

        JsonArray outer = new JsonArray();
        outer.add(new JsonArray());
        JsonArray withVector = outer.deepCopy();
        withVector.get(0).getAsJsonArray().add(new JsonArray());
        SchemaSeeds.fit(codec, schema, withVector);
        assertEquals(3, withVector.get(0).getAsJsonArray().get(0).getAsJsonArray().size());
    }

    private interface Thing {
        ResourceLocation type();
    }

    private record Colored(int color) implements Thing {
        static final MapCodec<Colored> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.fieldOf("color").forGetter(Colored::color)
        ).apply(i, Colored::new));

        @Override
        public ResourceLocation type() {
            return GenesisMod.id("colored");
        }
    }

    private record Sized(Optional<Double> size) implements Thing {
        static final MapCodec<Sized> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("size").forGetter(Sized::size)
        ).apply(i, Sized::new));

        @Override
        public ResourceLocation type() {
            return GenesisMod.id("sized");
        }
    }
}
