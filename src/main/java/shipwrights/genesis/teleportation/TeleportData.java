package shipwrights.genesis.teleportation;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public record TeleportData(Vector3d newPos, Quaterniond rotation, Vector3dc velocity, Vector3dc omega) {}
