package shipwrights.genesis.space.client.map.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.util.schema.Schema;
import shipwrights.genesis.util.schema.SchemaSeeds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.Supplier;

/// Builds a form from a {@link Schema}: rows of widgets that edit the JSON it describes, in place. Knows nothing of what
/// the JSON is for, only its shape, so a form follows whatever codec the schema was worked out from.
final class SchemaRows {
    private final Font font;
    private final CelestialDocuments.Document<?> document;
    private final Supplier<JsonElement> root;
    /// Told whenever a value changes
    private final Runnable onChange;
    /// Told whenever the form's shape changes, as when a field is added, so it can be built again
    private final Runnable onReshape;
    private final Function<ResourceKey<? extends Registry<?>>, List<ResourceLocation>> ids;
    private final SuggestionPopup popup;

    private SchemaRows(Font font, CelestialDocuments.Document<?> document, Supplier<JsonElement> root, Runnable onChange,
                       Runnable onReshape, Function<ResourceKey<? extends Registry<?>>, List<ResourceLocation>> ids,
                       SuggestionPopup popup) {
        this.font = font;
        this.document = document;
        this.root = root;
        this.onChange = onChange;
        this.onReshape = onReshape;
        this.ids = ids;
        this.popup = popup;
    }

    /// Rows editing the JSON {@code json} gives of {@code document}, which {@code setJson} replaces
    ///
    /// @param ids   the entries there are in a registry, to suggest for ids of them
    /// @param popup suggests what to type in the form's boxes
    static List<FormList.Row> build(Font font, CelestialDocuments.Document<?> document, Supplier<JsonElement> json,
                                    Consumer<JsonElement> setJson, Runnable onChange, Runnable onReshape,
                                    Function<ResourceKey<? extends Registry<?>>, List<ResourceLocation>> ids,
                                    SuggestionPopup popup) {
        SchemaRows rows = new SchemaRows(font, document, json, onChange, onReshape, ids, popup);
        List<FormList.Row> built = new ArrayList<>();
        rows.add(built, document.schema(), new Slot() {
            @Override
            public String path() {
                return "";
            }

            @Override
            public JsonElement get() {
                return json.get();
            }

            @Override
            public void set(JsonElement value) {
                setJson.accept(value);
            }

            @Override
            public void remove() {
                setJson.accept(SchemaSeeds.seed(document.schema()));
            }
        }, null, 0, true, List.of());
        return built;
    }

    /// Where a value is in the JSON, to read and replace it
    private interface Slot {
        /// Where it is, as {@code /transformProvider/parent}
        String path();

        @Nullable
        JsonElement get();

        void set(JsonElement value);

        void remove();
    }

    private static Slot field(Slot parent, JsonObject object, String name) {
        String path = parent.path() + "/" + name;
        return new Slot() {
            @Override
            public String path() {
                return path;
            }

            @Override
            public JsonElement get() {
                return object.get(name);
            }

            @Override
            public void set(JsonElement value) {
                object.add(name, value);
            }

            @Override
            public void remove() {
                object.remove(name);
            }
        };
    }

    private static Slot element(Slot parent, JsonArray array, int index) {
        String path = parent.path() + "/" + index;
        return new Slot() {
            @Override
            public String path() {
                return path;
            }

            @Override
            public JsonElement get() {
                return array.get(index);
            }

            @Override
            public void set(JsonElement value) {
                array.set(index, value);
            }

            @Override
            public void remove() {
                array.remove(index);
            }
        };
    }

    /// Puts a starting value for {@code schema} in {@code slot}, then lengthens any lists in the whole document that
    /// it reads better with, as fixed-length lists the new value brought in
    private void seed(Slot slot, Schema schema) {
        slot.set(SchemaSeeds.seed(schema));
        refit();
    }

    private void refit() {
        SchemaSeeds.fit(document.codec(), document.schema(), root.get());
    }

    private void changed() {
        onChange.run();
    }

    private void reshaped() {
        onChange.run();
        onReshape.run();
    }

    private void add(List<FormList.Row> rows, Schema schema, Slot slot, @Nullable Component label, int depth,
                     boolean required, List<AbstractWidget> trailing) {
        add(rows, schema, slot, label, depth, required, null, trailing);
    }

    /// Adds the rows editing the value in {@code slot}
    ///
    /// @param label        what the value's called, or null for the document itself, which has no row of its own
    /// @param required     whether it must be there. If not, and it's typed, it's left out when its box is empty; if
    ///                     it's made of several values, a checkbox puts it in or takes it out.
    /// @param defaultValue what it comes to when left out, if it's optional and that's known
    /// @param trailing     widgets to put after the value's own, as a list element's remove button
    private void add(List<FormList.Row> rows, Schema schema, Slot slot, @Nullable Component label, int depth,
                     boolean required, @Nullable JsonElement defaultValue, List<AbstractWidget> trailing) {
        JsonElement value = slot.get();
        Checkbox include = null;
        if (!required) {
            Optional<FieldArgument> argument = schema instanceof Schema.BoolSchema ? Optional.empty() : FieldArgument.of(schema, ids);
            if (argument.isPresent()) {
                addOptionalBox(rows, argument.get(), slot, label, depth, defaultValue, trailing);
                return;
            }
            include = Checkbox.builder(Component.empty(), font)
                    .selected(value != null)
                    .tooltip(Tooltip.create(Component.translatable("genesis.editor.optional")))
                    .onValueChange((checkbox, selected) -> {
                        if (!selected) {
                            slot.remove();
                        } else if (defaultValue != null) {
                            // Starting from what it came to while left out
                            slot.set(defaultValue.deepCopy());
                            refit();
                        } else {
                            seed(slot, schema);
                        }
                        reshaped();
                    })
                    .build();
            if (value == null) {
                rows.add(new FormList.Row(font, label == null ? Component.empty() : label, depth, include, trailing, 0x909090)
                        .keyed(slot.path()));
                return;
            }
        }
        addValue(rows, schema, slot, label, depth, include, trailing);
    }

    /// A box for an optional value, left out while it's empty. Its label's faint while it's left out.
    private void addOptionalBox(List<FormList.Row> rows, FieldArgument argument, Slot slot, @Nullable Component label,
                                int depth, @Nullable JsonElement defaultValue, List<AbstractWidget> trailing) {
        Component name = label == null ? Component.empty() : label;
        ArgumentBox box = new ArgumentBox(font, argument, popup, slot.get(), typed -> {
            boolean adding = slot.get() == null;
            slot.set(typed);
            // Built again to show it's no longer left out, carrying on typing in the new box
            if (adding) reshaped();
            else changed();
        }, name, () -> {
            if (slot.get() == null) return;
            slot.remove();
            reshaped();
        }, defaultValue);
        List<AbstractWidget> widgets = new ArrayList<>(List.of(box));
        widgets.addAll(trailing);
        rows.add(new FormList.Row(font, name, depth, null, widgets, slot.get() == null ? 0x909090 : 0xFFFFFF)
                .keyed(slot.path()));
    }

    /// Adds the rows editing the value in {@code slot}, which is there
    ///
    /// @param include the checkbox putting it in or taking it out, if it's optional
    private void addValue(List<FormList.Row> rows, Schema schema, Slot slot, @Nullable Component label, int depth,
                          @Nullable AbstractWidget include, List<AbstractWidget> trailing) {
        // A plain on/off is quicker clicked than typed
        if (schema instanceof Schema.BoolSchema) {
            rows.add(row(label, depth, include, boolButton(slot), trailing, slot));
            return;
        }
        // Whatever's typed as one value, in one box
        Optional<FieldArgument> argument = FieldArgument.of(schema, ids);
        if (argument.isPresent()) {
            rows.add(row(label, depth, include, argumentBox(slot, argument.get(), label), trailing, slot));
            return;
        }
        switch (schema) {
            case Schema.UnionSchema union -> addUnion(rows, union, slot, label, depth, include, trailing);
            case Schema.ListSchema list -> addList(rows, list, slot, label, depth, include, trailing);
            case Schema.MapSchema map -> addMap(rows, map, slot, label, depth, include, trailing, null);
            case Schema.DispatchSchema dispatch -> addDispatch(rows, dispatch, slot, label, depth, include, trailing);
            default -> throw new IllegalStateException("No way to edit " + schema);
        }
    }

    private FormList.Row row(@Nullable Component label, int depth, @Nullable AbstractWidget leading,
                             @Nullable AbstractWidget value, List<AbstractWidget> trailing, Slot slot) {
        List<AbstractWidget> widgets = new ArrayList<>();
        if (value != null) widgets.add(value);
        widgets.addAll(trailing);
        return new FormList.Row(font, label == null ? Component.empty() : label, depth, leading, widgets).keyed(slot.path());
    }

    private void addMap(List<FormList.Row> rows, Schema.MapSchema map, Slot slot, @Nullable Component label, int depth,
                        @Nullable AbstractWidget include, List<AbstractWidget> trailing, @Nullable String skip) {
        if (!(slot.get() instanceof JsonObject)) seed(slot, map);
        JsonObject object = (JsonObject) slot.get();
        int fieldDepth = depth;
        if (label != null) {
            rows.add(row(label, depth, include, null, trailing, slot));
            fieldDepth++;
        }
        for (Schema.Field field : map.fields()) {
            if (field.name().equals(skip)) continue;
            add(rows, field.schema(), field(slot, object, field.name()), fieldName(field.name()), fieldDepth,
                    field.required(), field.defaultValue(), List.of());
        }
    }

    private void addDispatch(List<FormList.Row> rows, Schema.DispatchSchema dispatch, Slot slot, @Nullable Component label,
                             int depth, @Nullable AbstractWidget include, List<AbstractWidget> trailing) {
        if (!(slot.get() instanceof JsonObject object) || !object.has(dispatch.typeKey())) {
            seed(slot, dispatch);
        }
        JsonObject object = (JsonObject) slot.get();
        JsonElement typeJson = object.get(dispatch.typeKey());
        ResourceLocation type = typeJson != null && typeJson.isJsonPrimitive() ? ResourceLocation.tryParse(typeJson.getAsString()) : null;

        Slot typeSlot = field(slot, object, dispatch.typeKey());
        ArgumentBox typeBox = argumentBox(typeSlot, FieldArgument.dispatchType(dispatch), label, chosen -> {
            ResourceLocation id = ResourceLocation.tryParse(chosen.getAsString());
            if (id == null || id.equals(type)) return;
            // Another type: its own fields in place of this one's
            slot.set(SchemaSeeds.seedDispatch(dispatch, id.toString()));
            refit();
            reshaped();
        });
        rows.add(row(label == null ? fieldName(dispatch.typeKey()) : label, depth, include, typeBox, trailing, typeSlot));

        Schema option = type == null ? null : dispatch.options().get(type);
        if (option instanceof Schema.MapSchema fields) {
            addMap(rows, fields, slot, null, depth + 1, null, List.of(), dispatch.typeKey());
        } else if (option != null) {
            add(rows, option, slot, fieldName(dispatch.typeKey()), depth + 1, true, List.of());
        }
    }

    private void addList(List<FormList.Row> rows, Schema.ListSchema list, Slot slot, @Nullable Component label, int depth,
                         @Nullable AbstractWidget include, List<AbstractWidget> trailing) {
        if (!(slot.get() instanceof JsonArray)) seed(slot, list);
        JsonArray array = (JsonArray) slot.get();
        Button addButton = Button.builder(Component.literal("+"), button -> {
            array.add(SchemaSeeds.seed(list.element()));
            refit();
            reshaped();
        }).size(16, 16).tooltip(Tooltip.create(Component.translatable("genesis.editor.add"))).build();
        List<AbstractWidget> headerWidgets = new ArrayList<>(List.of(addButton));
        headerWidgets.addAll(trailing);
        rows.add(new FormList.Row(font, label == null ? Component.empty() : label, depth, include, headerWidgets)
                .keyed(slot.path()));

        for (int i = 0; i < array.size(); i++) {
            int index = i;
            Button removeButton = Button.builder(Component.literal("−"), button -> {
                array.remove(index);
                reshaped();
            }).size(16, 16).tooltip(Tooltip.create(Component.translatable("genesis.editor.remove"))).build();
            add(rows, list.element(), element(slot, array, index), Component.literal("#" + (index + 1)), depth + 1, true,
                    List.of(removeButton));
        }
    }

    private void addUnion(List<FormList.Row> rows, Schema.UnionSchema union, Slot slot, @Nullable Component label, int depth,
                          @Nullable AbstractWidget include, List<AbstractWidget> trailing) {
        int current = 0;
        for (int i = 0; i < union.alternatives().size(); i++) {
            if (matches(union.alternatives().get(i), slot.get())) {
                current = i;
                break;
            }
        }
        int next = (current + 1) % union.alternatives().size();
        Button switchButton = Button.builder(Component.literal("⇄"), button -> {
            seed(slot, union.alternatives().get(next));
            reshaped();
        }).size(16, 16).tooltip(Tooltip.create(Component.translatable("genesis.editor.switch_kind",
                kindName(union.alternatives().get(next))))).build();

        List<AbstractWidget> widgets = new ArrayList<>(List.of(switchButton));
        widgets.addAll(trailing);
        addValue(rows, union.alternatives().get(current), slot, label, depth, include, widgets);
    }

    /// Whether {@code json} is of the shape {@code schema} describes, at its top level
    private static boolean matches(Schema schema, @Nullable JsonElement json) {
        if (json == null) return false;
        return switch (schema) {
            case Schema.NumberSchema number -> json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber();
            case Schema.BoolSchema bool -> json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean();
            case Schema.StringSchema string -> json.isJsonPrimitive() && json.getAsJsonPrimitive().isString();
            case Schema.ColorSchema color -> json.isJsonPrimitive() && !json.getAsJsonPrimitive().isBoolean();
            case Schema.ChoiceSchema choice -> json.isJsonPrimitive() && json.getAsJsonPrimitive().isString();
            case Schema.VectorSchema vector -> json.isJsonArray();
            case Schema.IdSchema id -> json.isJsonPrimitive() && json.getAsJsonPrimitive().isString();
            case Schema.ListSchema list -> json.isJsonArray();
            case Schema.MapSchema map -> json.isJsonObject();
            case Schema.DispatchSchema dispatch -> json.isJsonObject();
            case Schema.UnionSchema union -> union.alternatives().stream().anyMatch(alternative -> matches(alternative, json));
            case Schema.UnknownSchema unknown -> true;
        };
    }

    private static Component kindName(Schema schema) {
        String kind = switch (schema) {
            case Schema.NumberSchema number -> "number";
            case Schema.BoolSchema bool -> "boolean";
            case Schema.StringSchema string -> "text";
            case Schema.ColorSchema color -> "color";
            case Schema.ChoiceSchema choice -> "choice";
            case Schema.VectorSchema vector -> "vector";
            case Schema.IdSchema id -> "id";
            case Schema.ListSchema list -> "list";
            case Schema.MapSchema map -> "object";
            case Schema.DispatchSchema dispatch -> "object";
            case Schema.UnionSchema union -> "any";
            case Schema.UnknownSchema unknown -> "json";
        };
        return Component.translatable("genesis.editor.kind." + kind);
    }

    // Value widgets

    /// A box for the value in {@code slot}, typed as {@code argument} reads it
    private ArgumentBox argumentBox(Slot slot, FieldArgument argument, @Nullable Component label) {
        return argumentBox(slot, argument, label, value -> {
        });
    }

    /// @param after told each value typed, once it's in the slot
    private ArgumentBox argumentBox(Slot slot, FieldArgument argument, @Nullable Component label, Consumer<JsonElement> after) {
        return new ArgumentBox(font, argument, popup, slot.get(), value -> {
            slot.set(value);
            changed();
            after.accept(value);
        }, label == null ? Component.empty() : label);
    }

    private CycleButton<Boolean> boolButton(Slot slot) {
        JsonElement value = slot.get();
        boolean initial = value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean();
        return CycleButton.onOffBuilder(initial).displayOnlyValue()
                .create(0, 0, 44, 16, Component.translatable("genesis.editor.kind.boolean"), (button, chosen) -> {
                    slot.set(new JsonPrimitive(chosen));
                    changed();
                });
    }

    /// {@code genesis.editor.field.<name>}, or failing that the name in words: {@code castsLight} as "Casts light"
    static Component fieldName(String name) {
        String words = name.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ').toLowerCase(Locale.ROOT);
        String fallback = words.isEmpty() ? name : Character.toUpperCase(words.charAt(0)) + words.substring(1);
        return Component.translatableWithFallback("genesis.editor.field." + name, fallback);
    }
}
