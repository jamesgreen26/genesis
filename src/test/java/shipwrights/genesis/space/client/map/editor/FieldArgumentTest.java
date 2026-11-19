package shipwrights.genesis.space.client.map.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.MinecraftBootstrap;
import shipwrights.genesis.util.schema.Schema;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldArgumentTest {
    private static final ResourceLocation SUN = ResourceLocation.fromNamespaceAndPath("genesis", "sun");
    private static final ResourceLocation MOON = ResourceLocation.fromNamespaceAndPath("genesis", "moon");
    private static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");

    @BeforeAll
    static void setUp() {
        MinecraftBootstrap.bootstrap();
    }

    private static FieldArgument argument(Schema schema) {
        return FieldArgument.of(schema, registry -> List.of(SUN, MOON, OVERWORLD)).orElseThrow();
    }

    private static List<String> suggestions(FieldArgument argument, String text) {
        return argument.suggest(text).join().getList().stream().map(Suggestion::getText).toList();
    }

    @Test
    void numbersTakeExponentsAndRejectFractionsWhereWhole() throws CommandSyntaxException {
        FieldArgument decimal = argument(new Schema.NumberSchema(Schema.NumberKind.DOUBLE));
        assertEquals(new JsonPrimitive(new BigDecimal("6e9")), decimal.parseAll("6e9"));
        assertEquals(new JsonPrimitive(new BigDecimal("-0.25")), decimal.parseAll("-0.25"));
        assertThrows(CommandSyntaxException.class, () -> decimal.parseAll(""));
        assertThrows(CommandSyntaxException.class, () -> decimal.parseAll("1 2"));

        FieldArgument whole = argument(new Schema.NumberSchema(Schema.NumberKind.INT));
        assertEquals(new JsonPrimitive(new BigDecimal("12")), whole.parseAll("12"));
        assertThrows(CommandSyntaxException.class, () -> whole.parseAll("1.5"));
        assertThrows(CommandSyntaxException.class, () -> whole.parseAll("9999999999"));
    }

    @Test
    void numbersOutOfRangeDoNot() throws CommandSyntaxException {
        FieldArgument positive = argument(new Schema.NumberSchema(Schema.NumberKind.DOUBLE, Schema.Range.above(0)));
        assertEquals(new JsonPrimitive(new BigDecimal("0.5")), positive.parseAll("0.5"));
        assertThrows(CommandSyntaxException.class, () -> positive.parseAll("0"));
        assertThrows(CommandSyntaxException.class, () -> positive.parseAll("-1e3"));
        assertTrue(positive.info().isPresent());
        assertTrue(argument(new Schema.NumberSchema(Schema.NumberKind.DOUBLE)).info().isEmpty());

        FieldArgument percent = argument(new Schema.NumberSchema(Schema.NumberKind.INT, Schema.Range.between(0, 100)));
        percent.parseAll("100");
        assertThrows(CommandSyntaxException.class, () -> percent.parseAll("101"));
    }

    @Test
    void colorsReadHexOrDyesAndWriteHex() throws CommandSyntaxException {
        FieldArgument color = argument(new Schema.ColorSchema());
        assertEquals(new JsonPrimitive("#FFD026"), color.parseAll("#ffd026"));
        assertEquals(new JsonPrimitive("#FFFFFF"), color.parseAll("#fff"));
        assertEquals(new JsonPrimitive("#FFD026"), color.parseAll("FFD026"));
        assertTrue(color.parseAll("light_blue").getAsString().startsWith("#"));
        assertThrows(CommandSyntaxException.class, () -> color.parseAll("#ffd02"));
        assertThrows(CommandSyntaxException.class, () -> color.parseAll("mauve"));

        // Packed numbers, as older files have, show as hex
        assertEquals("#FFD026", color.format(new JsonPrimitive(0xFFD026)));
        assertTrue(suggestions(color, "bl").containsAll(List.of("blue", "black", "light_blue")));
        assertEquals(List.of(), suggestions(color, "#f"));
    }

    @Test
    void vectorsNeedEveryAxis() throws CommandSyntaxException {
        FieldArgument vector = argument(new Schema.VectorSchema(List.of("x", "y", "z"), Schema.NumberKind.DOUBLE));
        JsonArray expected = new JsonArray();
        expected.add(new BigDecimal("1"));
        expected.add(new BigDecimal("-2.5"));
        expected.add(new BigDecimal("3e3"));
        assertEquals(expected, vector.parseAll("1 -2.5 3e3"));
        assertEquals(expected, vector.parseAll("1, -2.5, 3e3"));
        assertThrows(CommandSyntaxException.class, () -> vector.parseAll("1 2"));
        assertThrows(CommandSyntaxException.class, () -> vector.parseAll("1 2 3 4"));
        CommandSyntaxException bad = assertThrows(CommandSyntaxException.class, () -> vector.parseAll("1 x 3"));
        assertEquals(2, bad.getCursor());

        JsonArray json = new JsonArray();
        json.add(0);
        json.add(1.5);
        json.add(-2);
        assertEquals("0 1.5 -2", vector.format(json));
    }

    @Test
    void idsSuggestThoseThereAreAndWarnOfOthers() throws CommandSyntaxException {
        FieldArgument id = argument(new Schema.IdSchema(net.minecraft.core.registries.Registries.DIMENSION));
        assertEquals(List.of("genesis:moon"), suggestions(id, "mo"));
        assertTrue(suggestions(id, "genesis:").containsAll(List.of("genesis:sun", "genesis:moon")));

        JsonElement other = id.parseAll("custom:planet");
        assertEquals("custom:planet", other.getAsString());
        assertTrue(id.warning(other).isPresent());
        assertTrue(id.warning(id.parseAll("genesis:sun")).isEmpty());
    }

    @Test
    void dispatchTypesMustBeKnown() {
        Schema.DispatchSchema dispatch = new Schema.DispatchSchema("type",
                new java.util.LinkedHashMap<>(java.util.Map.of(SUN, new Schema.MapSchema(List.of()))));
        FieldArgument type = FieldArgument.dispatchType(dispatch);
        assertThrows(CommandSyntaxException.class, () -> type.parseAll("genesis:moon"));
    }

    @Test
    void choicesSuggestTheirWords() throws CommandSyntaxException {
        FieldArgument dayLength = argument(new Schema.UnionSchema(List.of(
                new Schema.NumberSchema(Schema.NumberKind.DOUBLE), new Schema.ChoiceSchema(List.of("tidally_locked")))));
        assertEquals(List.of("tidally_locked"), suggestions(dayLength, ""));
        assertEquals(List.of("tidally_locked"), suggestions(dayLength, "ti"));
        assertEquals(new JsonPrimitive("tidally_locked"), dayLength.parseAll("tidally_locked"));
        assertTrue(dayLength.parseAll("24000").getAsJsonPrimitive().isNumber());
        assertThrows(CommandSyntaxException.class, () -> dayLength.parseAll("spinning"));
    }

    @Test
    void unionsTryEachKindInTurn() throws CommandSyntaxException {
        FieldArgument union = argument(new Schema.UnionSchema(List.of(
                new Schema.NumberSchema(Schema.NumberKind.DOUBLE), new Schema.StringSchema())));
        assertTrue(union.parseAll("24000").getAsJsonPrimitive().isNumber());
        assertEquals(new JsonPrimitive("tidally_locked"), union.parseAll("tidally_locked"));
        assertEquals(new JsonPrimitive("12 hours"), union.parseAll("12 hours"));
    }
}
