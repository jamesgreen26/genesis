package shipwrights.dataplanets.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import shipwrights.dataplanets.DataplanetsMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataPackUtil {
    public static <T> void write(MinecraftServer server, String path, String fileName, T jsonModel, Codec<T> codec) {
        try {
            Path basePath = server.storageSource.getLevelPath(LevelResource.DATAPACK_DIR);
            Path resolvedPath = basePath.resolve(path);

            Files.createDirectories(resolvedPath);

            JsonElement json = codec.encodeStart(JsonOps.INSTANCE, jsonModel)
                    .resultOrPartial(error -> DataplanetsMod.LOGGER.error("Failed to encode json: {}", error))
                    .orElseThrow(() -> new IOException("Failed to encode json"));

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = gson.toJson(json);

            Path filePath = resolvedPath.resolve(fileName);
            Files.writeString(filePath, jsonString);
        } catch (IOException e) {
            DataplanetsMod.LOGGER.error("Failed to write datapack: ", e);
        }
    }
}
