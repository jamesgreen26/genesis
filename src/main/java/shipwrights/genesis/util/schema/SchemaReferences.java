package shipwrights.genesis.util.schema;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/// Finds where JSON refers to entries of registries by id, as fields read by
/// {@link shipwrights.genesis.util.MoreCodecs#registryId} do, so what they refer to can be checked to exist, or
/// followed when it's renamed
public final class SchemaReferences {
    private SchemaReferences() {
    }

    /// @param field    the name of the field it's in, or of the list it's in, if any
    /// @param registry the registry it's the id of an entry in
    /// @param id       the id as written, which may not be one
    public record Reference(String field, ResourceKey<? extends Registry<?>> registry, String id) {}

    /// Every id in {@code json} that {@code schema} says refers to a registry entry
    public static List<Reference> find(Schema schema, JsonElement json) {
        List<Reference> found = new ArrayList<>();
        find(schema, json, "", found);
        return found;
    }

    /// Points every reference in {@code json} to {@code from} in {@code registry} at {@code to} instead, in place,
    /// finding them as {@link #find} does
    ///
    /// @return {@code json}, or what replaces it if it's itself such a reference
    public static JsonElement rename(Schema schema, JsonElement json, ResourceKey<? extends Registry<?>> registry,
                                     ResourceLocation from, ResourceLocation to) {
        if (json == null) return null;
        return switch (schema) {
            case Schema.IdSchema id when json.isJsonPrimitive() ->
                    id.registry().equals(registry) && from.equals(ResourceLocation.tryParse(json.getAsString()))
                            ? new JsonPrimitive(to.toString()) : json;
            case Schema.ListSchema list when json instanceof JsonArray array -> {
                for (int i = 0; i < array.size(); i++) {
                    array.set(i, rename(list.element(), array.get(i), registry, from, to));
                }
                yield json;
            }
            case Schema.MapSchema map when json instanceof JsonObject object -> {
                for (Schema.Field each : map.fields()) {
                    JsonElement value = object.get(each.name());
                    if (value != null) object.add(each.name(), rename(each.schema(), value, registry, from, to));
                }
                yield json;
            }
            case Schema.DispatchSchema dispatch when json instanceof JsonObject object -> {
                JsonElement type = object.get(dispatch.typeKey());
                Schema option = type == null || !type.isJsonPrimitive() ? null
                        : dispatch.options().get(ResourceLocation.tryParse(type.getAsString()));
                yield option == null ? json : rename(option, json, registry, from, to);
            }
            case Schema.UnionSchema union -> {
                // The same alternative find takes its references from
                for (Schema alternative : union.alternatives()) {
                    List<Reference> inAlternative = new ArrayList<>();
                    find(alternative, json, "", inAlternative);
                    if (!inAlternative.isEmpty()) yield rename(alternative, json, registry, from, to);
                }
                yield json;
            }
            default -> json;
        };
    }

    private static void find(Schema schema, JsonElement json, String field, List<Reference> found) {
        if (json == null) return;
        switch (schema) {
            case Schema.IdSchema id when json.isJsonPrimitive() ->
                    found.add(new Reference(field, id.registry(), json.getAsString()));
            case Schema.ListSchema list when json instanceof JsonArray array -> {
                for (JsonElement element : array) find(list.element(), element, field, found);
            }
            case Schema.MapSchema map when json instanceof JsonObject object -> {
                for (Schema.Field each : map.fields()) find(each.schema(), object.get(each.name()), each.name(), found);
            }
            case Schema.DispatchSchema dispatch when json instanceof JsonObject object -> {
                JsonElement type = object.get(dispatch.typeKey());
                Schema option = type == null || !type.isJsonPrimitive() ? null
                        : dispatch.options().get(ResourceLocation.tryParse(type.getAsString()));
                if (option != null) find(option, json, field, found);
            }
            case Schema.UnionSchema union -> {
                // Only the first alternative of the JSON's shape, as the codec reads it
                for (Schema alternative : union.alternatives()) {
                    List<Reference> inAlternative = new ArrayList<>();
                    find(alternative, json, field, inAlternative);
                    if (!inAlternative.isEmpty()) {
                        found.addAll(inAlternative);
                        break;
                    }
                }
            }
            default -> {
            }
        }
    }
}
