package shipwrights.genesis.space.client;

import org.joml.Vector3f;

/// The faces of a cube as its texture lays them out, in a 3×2 grid:
///
/// ```
/// | north | west | south |
/// | east  | down | up    |
/// ```
///
/// Each face maps a point on it, {@code s} across from its left edge and {@code t} down from its top edge, both from 0
/// to 1, onto the cube from -1 to 1 on each axis. Faces that meet on the cube carry on from one another in the texture's
/// coordinates, so what's drawn across an edge of one face continues onto the next.
public enum CubeFace {
    NORTH(0, 0, 1, 1, -1, -2, 0, 0, 0, -2, 0),
    WEST(1, 0, -1, 1, -1, 0, 0, 2, 0, -2, 0),
    SOUTH(2, 0, -1, 1, 1, 2, 0, 0, 0, -2, 0),
    EAST(0, 1, 1, 1, 1, 0, 0, -2, 0, -2, 0),
    DOWN(1, 1, -1, -1, 1, 2, 0, 0, 0, 0, -2),
    UP(2, 1, -1, 1, -1, 2, 0, 0, 0, 0, 2);

    public static final int COLUMNS = 3;
    public static final int ROWS = 2;

    /// Where in the grid it is
    public final int column, row;
    /// Its top left corner on the cube
    private final int originX, originY, originZ;
    /// Across it, from its left edge to its right
    private final int acrossX, acrossY, acrossZ;
    /// Down it, from its top edge to its bottom
    private final int downX, downY, downZ;

    CubeFace(int column, int row, int originX, int originY, int originZ, int acrossX, int acrossY, int acrossZ,
             int downX, int downY, int downZ) {
        this.column = column;
        this.row = row;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.acrossX = acrossX;
        this.acrossY = acrossY;
        this.acrossZ = acrossZ;
        this.downX = downX;
        this.downY = downY;
        this.downZ = downZ;
    }

    /// The point on the cube {@code s} across the face and {@code t} down it, from -1 to 1 on each axis
    public double x(double s, double t) {
        return originX + s * acrossX + t * downX;
    }

    public double y(double s, double t) {
        return originY + s * acrossY + t * downY;
    }

    public double z(double s, double t) {
        return originZ + s * acrossZ + t * downZ;
    }

    public Vector3f point(float s, float t) {
        return new Vector3f((float) x(s, t), (float) y(s, t), (float) z(s, t));
    }

    /// Its left, top, right and bottom edges in the texture, as UVs from 0 to 1
    public float u0() {
        return column / (float) COLUMNS;
    }

    public float v0() {
        return row / (float) ROWS;
    }

    public float u1() {
        return (column + 1) / (float) COLUMNS;
    }

    public float v1() {
        return (row + 1) / (float) ROWS;
    }
}
