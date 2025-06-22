package g_mungus.genesis.asteroid.generation.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joml.Vector3i;

import java.util.Map;

public class VoxelSetToJson {


    public static ObjectNode generateVoxelJson(ModelVoxelSet voxelSet) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        // Textures
        ObjectNode textures = mapper.createObjectNode();
        textures.put("0", "block/stone");
        textures.put("particle", "block/stone");
        root.set("textures", textures);

        // Elements
        ArrayNode elements = mapper.createArrayNode();

        for (Map.Entry<Vector3i, ModelVoxelSet.VoxelWithFaces> entry : voxelSet.voxels.entrySet()) {
            Vector3i pos = entry.getKey();
            ModelVoxelSet.VoxelWithFaces voxel = entry.getValue();

            ObjectNode element = mapper.createObjectNode();

            ArrayNode from = mapper.createArrayNode();
            from.add(pos.x).add(pos.y).add(pos.z);
            element.set("from", from);

            ArrayNode to = mapper.createArrayNode();
            to.add(pos.x + 1).add(pos.y + 1).add(pos.z + 1);
            element.set("to", to);

            ObjectNode faces = mapper.createObjectNode();

            if (voxel.north) {
                faces.set("north", faceJson(mapper, pos, "north"));
            }
            if (voxel.east) {
                faces.set("east", faceJson(mapper, pos, "east"));
            }
            if (voxel.south) {
                faces.set("south", faceJson(mapper, pos, "south"));
            }
            if (voxel.west) {
                faces.set("west", faceJson(mapper, pos, "west"));
            }
            if (voxel.up) {
                faces.set("up", faceJson(mapper, pos, "up"));
            }
            if (voxel.down) {
                faces.set("down", faceJson(mapper, pos, "down"));
            }

            if (!faces.isEmpty()) {
                element.set("faces", faces);
                elements.add(element);
            }
        }

        root.set("elements", elements);
        return root;
    }

    private static ObjectNode faceJson(ObjectMapper mapper, Vector3i pos, String face) {
        ObjectNode faceObj = mapper.createObjectNode();
        ArrayNode uv = mapper.createArrayNode();

        switch (face) {
            case "north" -> {
                // North face: faces -Z → plane is X,Y
                uv.add(pos.x).add(16 - pos.y - 1).add(pos.x + 1).add(16 - pos.y); // Y is flipped
            }
            case "south" -> {
                // South face: faces +Z → plane is X,Y
                uv.add(pos.x).add(16 - pos.y - 1).add(pos.x + 1).add(16 - pos.y);
            }
            case "east" -> {
                // East face: faces +X → plane is Z,Y
                uv.add(pos.z).add(16 - pos.y - 1).add(pos.z + 1).add(16 - pos.y);
            }
            case "west" -> {
                // West face: faces -X → plane is Z,Y
                uv.add(pos.z).add(16 - pos.y - 1).add(pos.z + 1).add(16 - pos.y);
            }
            case "up" -> {
                // Up face: faces +Y → plane is X,Z
                uv.add(pos.x).add(16 - pos.z - 1).add(pos.x + 1).add(16 - pos.z);
            }
            case "down" -> {
                // Down face: faces -Y → plane is X,Z
                uv.add(pos.x).add(16 - pos.z - 1).add(pos.x + 1).add(16 - pos.z);
            }
        }

        faceObj.set("uv", uv);
        faceObj.put("texture", "#0");
        return faceObj;
    }
}
