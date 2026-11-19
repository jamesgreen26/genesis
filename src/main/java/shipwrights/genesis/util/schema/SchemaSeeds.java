package shipwrights.genesis.util.schema;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/// Starting values for a {@link Schema}: the simplest JSON of its shape, which a codec may still reject, such as for
/// being out of range, but that an editor can show and the user fill in.
public final class SchemaSeeds {
    /// The longest a list is tried at when {@linkplain #fit fitting}
    private static final int MAX_FITTED_LENGTH = 8;

    private SchemaSeeds() {
    }

    /// Zeroes, {@code false}, empty strings and lists, objects with just their required fields, and dispatches as
    /// their first type, unless the schema says to start from something else
    public static JsonElement seed(Schema schema) {
        return switch (schema) {
            case Schema.NumberSchema number -> new JsonPrimitive(number.start());
            case Schema.BoolSchema bool -> new JsonPrimitive(bool.seed());
            case Schema.StringSchema string -> new JsonPrimitive("");
            case Schema.ColorSchema color -> new JsonPrimitive("#FFFFFF");
            case Schema.ChoiceSchema choice -> new JsonPrimitive(choice.values().isEmpty() ? "" : choice.values().get(0));
            case Schema.VectorSchema vector -> {
                JsonArray array = new JsonArray();
                vector.axes().forEach(axis -> array.add(0));
                yield array;
            }
            case Schema.IdSchema id -> new JsonPrimitive(id.seed() == null ? "" : id.seed().toString());
            case Schema.ListSchema list -> new JsonArray();
            case Schema.MapSchema map -> {
                JsonObject object = new JsonObject();
                for (Schema.Field field : map.fields()) {
                    if (field.required()) object.add(field.name(), seed(field.schema()));
                }
                yield object;
            }
            case Schema.DispatchSchema dispatch -> dispatch.options().isEmpty()
                    ? new JsonObject()
                    : seedDispatch(dispatch, dispatch.options().keySet().iterator().next().toString());
            case Schema.UnionSchema union -> seed(union.alternatives().get(0));
            case Schema.UnknownSchema unknown -> JsonNull.INSTANCE;
        };
    }

    /// The seed of one of a dispatch's types, with its type set
    public static JsonObject seedDispatch(Schema.DispatchSchema dispatch, String type) {
        JsonObject object = new JsonObject();
        object.addProperty(dispatch.typeKey(), type);
        Schema option = dispatch.options().get(ResourceLocation.tryParse(type));
        if (option != null && seed(option) instanceof JsonObject fields) {
            fields.entrySet().forEach(entry -> object.add(entry.getKey(), entry.getValue()));
        }
        return object;
    }

    /// Lengthens each list in {@code json}, in place, to whichever length up to {@value #MAX_FITTED_LENGTH} leaves
    /// {@code decoder} the fewest errors, preferring the shortest, filled with seeds of its elements. Lists of a fixed
    /// length, such as vectors, then start out the right length. Lists are never shortened.
    public static void fit(Decoder<?> decoder, Schema schema, JsonElement json) {
        // Lists inside the lists fitted are fitted in turn, once their lists have their elements
        ArrayDeque<Fittable> pending = new ArrayDeque<>(lists(schema, json));
        while (!pending.isEmpty()) {
            Fittable list = pending.poll();
            int best = list.array().size();
            int fewest = Integer.MAX_VALUE;
            for (int length = best; length <= MAX_FITTED_LENGTH; length++) {
                list.resize(length);
                int errors = errors(decoder, json);
                if (errors < fewest) {
                    fewest = errors;
                    best = length;
                }
            }
            list.resize(best);
            for (JsonElement element : list.array()) pending.addAll(lists(list.element(), element));
        }
    }

    /// How many errors decoding {@code json} gives
    public static int errors(Decoder<?> decoder, JsonElement json) {
        try {
            return decoder.decode(JsonOps.INSTANCE, json).error()
                    .map(error -> error.message().split("; ").length)
                    .orElse(0);
        } catch (RuntimeException e) {
            return Integer.MAX_VALUE / 2;
        }
    }

    private record Fittable(JsonArray array, Schema element) {
        void resize(int length) {
            while (array.size() > length) array.remove(array.size() - 1);
            while (array.size() < length) array.add(seed(element));
        }
    }

    /// Every list in {@code json} the schema says is one
    private static List<Fittable> lists(Schema schema, JsonElement json) {
        List<Fittable> found = new ArrayList<>();
        switch (schema) {
            case Schema.ListSchema list when json instanceof JsonArray array -> found.add(new Fittable(array, list.element()));
            case Schema.VectorSchema vector when json instanceof JsonArray array ->
                    found.add(new Fittable(array, new Schema.NumberSchema(vector.kind())));
            case Schema.MapSchema map when json instanceof JsonObject object -> {
                for (Schema.Field field : map.fields()) {
                    JsonElement value = object.get(field.name());
                    if (value != null) found.addAll(lists(field.schema(), value));
                }
            }
            case Schema.DispatchSchema dispatch when json instanceof JsonObject object -> {
                JsonElement type = object.get(dispatch.typeKey());
                Schema option = type == null || !type.isJsonPrimitive() ? null
                        : dispatch.options().get(ResourceLocation.tryParse(type.getAsString()));
                if (option != null) found.addAll(lists(option, json));
            }
            case Schema.UnionSchema union -> {
                for (Schema alternative : union.alternatives()) found.addAll(lists(alternative, json));
            }
            default -> {
            }
        }
        return found;
    }

    /// A seed fitted to {@code decoder}
    public static JsonElement fitted(Decoder<?> decoder, Schema schema) {
        JsonElement json = seed(schema);
        fit(decoder, schema, json);
        return json;
    }
}
