package g_mungus.genesis.asteroid.generation.model;


import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

public class ModelVoxelSet {

    public Map<Vector3i, VoxelWithFaces> voxels = new HashMap<>();

    public void optimize() {
        for (Map.Entry<Vector3i, VoxelWithFaces> entry : voxels.entrySet()) {
            Vector3i pos = entry.getKey();
            VoxelWithFaces voxel = entry.getValue();

            checkAndDisableFace(pos, voxel, new Vector3i(0, 0, 1), "north", "south");
            checkAndDisableFace(pos, voxel, new Vector3i(1, 0, 0), "east", "west");
            checkAndDisableFace(pos, voxel, new Vector3i(0, 0, -1), "south", "north");
            checkAndDisableFace(pos, voxel, new Vector3i(-1, 0, 0), "west", "east");
            checkAndDisableFace(pos, voxel, new Vector3i(0, 1, 0), "up", "down");
            checkAndDisableFace(pos, voxel, new Vector3i(0, -1, 0), "down", "up");
        }
        removeFullyEnclosedVoxels();
    }

    private void removeFullyEnclosedVoxels() {
        voxels.entrySet().removeIf(entry -> allFacesDisabled(entry.getValue()));
    }

    private boolean allFacesDisabled(VoxelWithFaces voxel) {
        return !voxel.north && !voxel.east && !voxel.south &&
                !voxel.west && !voxel.up && !voxel.down;
    }

    private void checkAndDisableFace(
            Vector3i pos,
            VoxelWithFaces voxel,
            Vector3i offset,
            String thisFace,
            String neighborFace
    ) {
        Vector3i neighborPos = new Vector3i(pos).add(offset);
        VoxelWithFaces neighbor = voxels.get(neighborPos);

        if (neighbor != null) {
            disableFace(voxel, thisFace);
            disableFace(neighbor, neighborFace);
        }
    }

    private void disableFace(VoxelWithFaces voxel, String face) {
        switch (face) {
            case "north" -> voxel.north = false;
            case "east" -> voxel.east = false;
            case "south" -> voxel.south = false;
            case "west" -> voxel.west = false;
            case "up" -> voxel.up = false;
            case "down" -> voxel.down = false;
        }
    }

    public static class VoxelWithFaces {
        public boolean north = true;
        public boolean east = true;
        public boolean south = true;
        public boolean west = true;
        public boolean up = true;
        public boolean down = true;

        VoxelWithFaces() {}
    }
}