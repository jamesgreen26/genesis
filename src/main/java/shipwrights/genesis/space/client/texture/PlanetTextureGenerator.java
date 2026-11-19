package shipwrights.genesis.space.client.texture;

import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.space.client.CubeFace;
import shipwrights.genesis.space.client.texture.PlanetTextureSettings.Group;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;

/// Makes the surface texture of a planet drawn as a cube, laid out as {@link CubeFace} has it, in the blocky,
/// few-coloured look of the game's own: shapes of overlapping rectangles, colours banded by height, checkered dither
/// where bands meet.
///
/// The surface is first worked out smoothly, from noise, as it looks with no blockiness. Blocks then approximate that:
/// each takes the median of what it covers, so it follows whatever covers most of it and ignores details smaller than
/// it is. Where blocks of different colours meet, each is dithered towards the other, a little way either side, as a
/// transition between them.
///
/// Everything's worked out from where each pixel is on the cube, in three dimensions, rather than where it is in the
/// texture. Pixels either side of an edge between faces are next to each other on the cube, so come out alike, and the
/// texture carries on across every edge without a seam. Blocks are placed on the cube likewise, so they fold over edges
/// rather than stopping at them.
///
/// Made in two steps: a {@link Layout}, which is the slow part and says which colour of which group each pixel is, then
/// {@linkplain Layout#paint painted} in the palette's colours. Changing only the colours needs only the second.
///
/// Pure Java, so it can run off the render thread.
public final class PlanetTextureGenerator {
    /// The frequency across the cube of each octave of noise
    private static final double[] OCTAVES = {1.0, 2.0, 4.0, 8.0};
    /// How big blocks are, in pixels on a face 128 across, at half blockiness
    private static final double BLOCK_SIZE = 12;
    /// How much each octave counts compared with the one before, at no noisiness and at full. Halfway, it's 0.45.
    private static final double MIN_PERSISTENCE = 0.1;
    private static final double MAX_PERSISTENCE = 0.8;
    /// How far the noise is pushed about by other noise, so outlines wander rather than following the grid
    private static final double WARP = 0.5;
    private static final double WARP_FREQUENCY = 1.3;
    /// The face size sizes are given for
    private static final double REFERENCE_FACE = 128;
    /// How far apart blocks are, on average, as a fraction of their size, so each overlaps its neighbours
    private static final double BLOCK_STEP = 0.55;
    /// How far each block's moved from where it'd be on a grid, at most, as a fraction of its size
    private static final double BLOCK_JITTER = 0.125;
    /// How big the biggest blocks are, as a fraction of the size asked for. The smallest are just big enough to meet
    /// the next block along wherever they're moved to, which with the above is 0.8, so they're the size asked for on
    /// average.
    private static final double BLOCK_MAX = 1.2;
    /// Blocks smaller than a pixel aren't drawn, but the noise is taken at each pixel
    private static final double MIN_BLOCK = 1;
    /// What blocks' edges are rounded out to, as a fraction of their size, so that where one's mostly covered by
    /// another, what's left showing is never a sliver
    private static final double BLOCK_QUANTUM = 0.2;

    /// How far dither reaches from an edge between colours at full dither, in pixels on a face 128 across
    private static final double DITHER_WIDTH = 8;
    /// Thresholds for ordered dither: a pixel's in the other colour where its threshold's below the share of it, in
    /// sixteenths. Shares of a half, a quarter and an eighth come out as a checkerboard, a dot every other pixel of
    /// every other row, and half as many again, offset, as the game's own textures have.
    private static final int[][] BAYER = {{0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}};
    /// The shares of the other colour dither's drawn with, in sixteenths, from most to least: only these, as others
    /// make patterns too busy to read as shading
    private static final int[] DITHER_LEVELS = {8, 4, 2};
    /// How much further, as a fraction of that, dither reaches where the smooth surface is the colour across the edge
    private static final double DITHER_SMOOTHNESS = 0.5;
    /// How big the areas whose edges are dithered or not are: the frequency across the cube of the noise that picks them
    private static final double COVERAGE_FREQUENCY = 2.5;
    /// Over how much of the edges, as a fraction, dither fades out between where it's drawn and where it isn't
    private static final double COVERAGE_FADE = 0.1;

    /// Where patches are drawn, by noise that's higher towards the equator
    private static final double PATCH_THRESHOLD = 0.28;
    /// How near the poles their colours start, by the sine of the latitude plus noise, and how wide each band is
    private static final double POLE_EDGE = 0.87;
    private static final double POLE_BAND = 0.05;
    private static final double SPECK_THRESHOLD = 0.33;

    private PlanetTextureGenerator() {}

    /// The texture {@code settings} make, as {@code 0xAARRGGBB} pixels, row by row
    public static int[] generate(PlanetTextureSettings settings) {
        return layout(settings).paint(settings);
    }

    /// Which colour of which group each pixel is, for {@code settings} in any colours
    ///
    /// @param settings what it's for, which it can be {@linkplain #paint painted} with, or with any settings that
    ///                 have the {@linkplain PlanetTextureSettings#sameLayout same layout}
    /// @param codes    for each pixel, row by row, its group's ordinal in the high byte and its colour's index in the
    ///                 group in the low byte
    public record Layout(PlanetTextureSettings settings, short[] codes) {
        public int width() {
            return settings.width();
        }

        public int height() {
            return settings.height();
        }

        public int[] paint(PlanetTextureSettings colors) {
            if (!settings.sameLayout(colors)) throw new IllegalArgumentException("Not laid out for " + colors);
            int[] lookup = new int[Group.values().length << 8];
            for (Group group : Group.values()) {
                List<Integer> groupColors = colors.colors(group);
                for (int i = 0; i < groupColors.size(); i++) lookup[group.ordinal() << 8 | i] = 0xFF000000 | groupColors.get(i);
            }
            int[] pixels = new int[codes.length];
            for (int i = 0; i < codes.length; i++) pixels[i] = lookup[codes[i]];
            return pixels;
        }
    }

    public static Layout layout(PlanetTextureSettings settings) {
        int face = settings.faceSize();
        int width = settings.width();
        int count = width * settings.height();
        double scale = face / REFERENCE_FACE;
        // Twice as big at full blockiness as halfway, and gone at none
        double blockScale = scale * 2 * settings.blockiness();
        SplittableRandom random = new SplittableRandom(settings.seed());
        Noise[] noise = new Noise[6];
        for (int i = 0; i < noise.length; i++) noise[i] = new Noise(random);

        // Where each pixel is on the cube
        double[] x = new double[count], y = new double[count], z = new double[count];
        for (CubeFace cubeFace : CubeFace.values()) {
            for (int row = 0; row < face; row++) {
                for (int column = 0; column < face; column++) {
                    double s = (column + 0.5) / face, t = (row + 0.5) / face;
                    int index = (cubeFace.row * face + row) * width + cubeFace.column * face + column;
                    x[index] = cubeFace.x(s, t);
                    y[index] = cubeFace.y(s, t);
                    z[index] = cubeFace.z(s, t);
                }
            }
        }
        Cube cube = new Cube(face, x, y, z);

        // The height as it is with no blockiness
        double[] smooth = new double[count];
        double persistence = MIN_PERSISTENCE + (MAX_PERSISTENCE - MIN_PERSISTENCE) * settings.noisiness();
        for (int i = 0; i < count; i++) {
            double wx = noise[3].at(x[i] * WARP_FREQUENCY + 10, y[i] * WARP_FREQUENCY + 10, z[i] * WARP_FREQUENCY + 10) * WARP;
            double wy = noise[4].at(x[i] * WARP_FREQUENCY + 20, y[i] * WARP_FREQUENCY + 20, z[i] * WARP_FREQUENCY + 20) * WARP;
            double wz = noise[5].at(x[i] * WARP_FREQUENCY + 30, y[i] * WARP_FREQUENCY + 30, z[i] * WARP_FREQUENCY + 30) * WARP;
            double total = 0;
            for (int octave = 0; octave < OCTAVES.length; octave++) {
                double frequency = OCTAVES[octave], offset = octave * 7.1;
                total += Math.pow(persistence, octave) * noise[octave % 3].at((x[i] + wx) * frequency + offset,
                        (y[i] + wy) * frequency + offset, (z[i] + wz) * frequency + offset);
            }
            smooth[i] = total;
        }

        // And in blocks that approximate it. The same blocks for everything, so patches and poles follow their edges.
        Blocks blocks = cube.blocks(BLOCK_SIZE * blockScale, random);
        double[] height = blocks == null ? smooth.clone() : blocks.median(smooth);

        // By rank, so every colour gets its share whatever the noise comes to
        double[] sorted = height.clone();
        Arrays.sort(sorted);
        for (int i = 0; i < count; i++) {
            height[i] = rank(sorted, height[i]);
            smooth[i] = rank(sorted, smooth[i]);
        }

        // Only worked out if there are patches to draw
        double[] patchiness = new double[count];
        if (!settings.colors(Group.PATCHES).isEmpty()) {
            for (int i = 0; i < count; i++) {
                patchiness[i] = noise[1].at(x[i] * 1.6 + 40, y[i] * 1.6 + 40, z[i] * 1.6 + 40) + 0.3 * (1 - Math.abs(y[i]));
            }
            if (blocks != null) patchiness = blocks.median(patchiness);
        }
        // Each pixel's colour, in blocks and smoothly
        Bands bands = new Bands(settings);
        short[] codes = new short[count];
        short[] smoothCodes = new short[count];
        for (int i = 0; i < count; i++) {
            boolean patch = patchiness[i] > PATCH_THRESHOLD;
            codes[i] = bands.code(height[i], patch);
            smoothCodes[i] = bands.code(smooth[i], patch);
        }

        int poles = settings.colors(Group.POLES).size();
        if (poles > 0) {
            double[] smoothCap = new double[count];
            for (int i = 0; i < count; i++) smoothCap[i] = nearPole(noise[0], x[i], y[i], z[i]);
            double[] cap = blocks == null ? smoothCap : blocks.median(smoothCap);
            for (int i = 0; i < count; i++) {
                if (cap[i] >= POLE_EDGE) codes[i] = poleCode(cap[i], poles);
                if (smoothCap[i] >= POLE_EDGE) smoothCodes[i] = poleCode(smoothCap[i], poles);
            }
        }

        dither(cube, codes, smoothCodes, settings.ditherSpread() * DITHER_WIDTH * scale,
                settings.ditherCoverage(), noise[2]);

        int specks = settings.colors(Group.SPECKS).size();
        if (specks > 0) {
            for (int i = 0; i < count; i++) {
                if (!sparse(i % width, i / width)) continue;
                double speck = noise[1].at(x[i] * 9 + 99, y[i] * 9 + 99, z[i] * 9 + 99);
                if (speck > SPECK_THRESHOLD) {
                    codes[i] = code(Group.SPECKS, Math.min(specks - 1, (int) ((speck - SPECK_THRESHOLD) / 0.1)));
                }
            }
        }
        return new Layout(settings, codes);
    }

    private static short poleCode(double cap, int poles) {
        return code(Group.POLES, Math.min(poles - 1, (int) ((cap - POLE_EDGE) / POLE_BAND)));
    }

    /// Dithers each colour towards the next, either side of where they meet, so there's a transition between them: half
    /// and half at the edge, thinning to none at {@code radius} pixels from it. Ordered dither, by {@link #BAYER}, with
    /// the side lower in the palette taking the other's colour where its threshold's lowest and the other side where
    /// it's highest, so between them they make one ramp from one colour to the other.
    ///
    /// It follows the edges of the colours as they are, in blocks, but reaches further where the smooth surface is the
    /// colour across the edge, so keeps a little of its shape.
    ///
    /// Only some edges are dithered, as many as {@code coverage} asks for: those in areas picked out by noise, so
    /// they're dithered or not a stretch at a time. Dither narrows towards where it stops, rather than stopping short.
    ///
    /// @param coverage how much of the edges are dithered, from 0 to 1
    private static void dither(Cube cube, short[] codes, short[] smoothCodes, double radius,
                               double coverage, Noise noise) {
        if (radius < 0.5 || coverage <= 0) return;
        int count = codes.length;
        int width = cube.face * CubeFace.COLUMNS;
        int[] neighbours = cube.neighbours();
        double limit = radius * (1 + DITHER_SMOOTHNESS);

        // How far each pixel is from an edge of its colour, by the nearest pixel on one, and the colour across it
        double[] distance = new double[count];
        Arrays.fill(distance, Double.POSITIVE_INFINITY);
        int[] nearest = new int[count];
        short[] across = new short[count];
        int[] queue = new int[count];
        int head = 0, tail = 0;
        for (int i = 0; i < count; i++) {
            for (int n = 0; n < 4; n++) {
                int j = neighbours[i * 4 + n];
                if (codes[j] == codes[i]) continue;
                // Towards the colour the smooth surface is, if it's one of those across
                if (Double.isInfinite(distance[i]) || smoothCodes[i] == codes[j]) across[i] = codes[j];
                distance[i] = 0.5;
                nearest[i] = i;
            }
            if (distance[i] == 0.5) queue[tail++] = i;
        }

        // How much of its spread each edge pixel's dithered with: by where it ranks, among them all, in noise
        double[] edgeNoise = new double[tail];
        for (int n = 0; n < tail; n++) {
            int i = queue[n];
            edgeNoise[n] = noise.at(cube.x[i] * COVERAGE_FREQUENCY + 70, cube.y[i] * COVERAGE_FREQUENCY + 70,
                    cube.z[i] * COVERAGE_FREQUENCY + 70);
        }
        double[] sortedNoise = edgeNoise.clone();
        Arrays.sort(sortedNoise);
        double[] covered = new double[count];
        for (int n = 0; n < tail; n++) {
            double rank = rank(sortedNoise, edgeNoise[n]);
            // All of it when the coverage is full, and none when it's nothing
            covered[queue[n]] = Math.max(0, Math.min(1, (coverage * (1 + COVERAGE_FADE) - rank) / COVERAGE_FADE));
        }
        // Outwards from the edges, within each colour, taking whichever edge pixel's nearest
        while (head != tail) {
            int i = queue[head];
            head = (head + 1) % count;
            for (int n = 0; n < 4; n++) {
                int j = neighbours[i * 4 + n];
                if (codes[j] != codes[i]) continue;
                double reach = cube.distance(j, nearest[i]) + 0.5;
                if (reach >= distance[j] || reach > limit) continue;
                boolean queued = !Double.isInfinite(distance[j]);
                distance[j] = reach;
                nearest[j] = nearest[i];
                across[j] = across[i];
                if (!queued) {
                    queue[tail] = j;
                    tail = (tail + 1) % count;
                }
            }
        }

        for (int i = 0; i < count; i++) {
            if (Double.isInfinite(distance[i])) continue;
            double spread = radius * covered[nearest[i]];
            if (spread <= 0) continue;
            double strength = 1 - distance[i] / spread + (smoothCodes[i] == across[i] ? DITHER_SMOOTHNESS : 0);
            // Up to half the other colour, at the edge
            int share = ditherLevel(Math.max(0, Math.min(1, strength)) * 8);
            if (share == 0) continue;
            int threshold = BAYER[(i / width) & 3][(i % width) & 3];
            if (codes[i] > across[i]) threshold = 15 - threshold;
            if (threshold < share) codes[i] = across[i];
        }
    }

    /// How near a pole a point is, by the sine of its latitude, pushed about by noise so its edge isn't round
    private static double nearPole(Noise noise, double x, double y, double z) {
        double latitude = Math.abs(y) / Math.sqrt(x * x + y * y + z * z);
        return latitude + 0.25 * noise.at(x * 3 + 50, y * 3 + 50, z * 3 + 50);
    }

    /// Where {@code value} ranks among {@code sorted}, from 0 to 1
    private static double rank(double[] sorted, double value) {
        int index = Arrays.binarySearch(sorted, value);
        if (index < 0) index = -index - 1;
        return Math.min(1, index / (double) Math.max(1, sorted.length - 1));
    }

    /// The most of {@link #DITHER_LEVELS} that's no more than {@code share} sixteenths, rounded to the nearest, or none
    private static int ditherLevel(double share) {
        for (int level : DITHER_LEVELS) {
            if (share >= level * 0.75) return level;
        }
        return 0;
    }

    /// One pixel in four
    private static boolean sparse(int x, int y) {
        return (x & 1) == 0 && (y & 1) == 0;
    }

    private static short code(Group group, int index) {
        return (short) (group.ordinal() << 8 | index);
    }

    /// Which colour a height comes to: below the blend, the first palette's, spread over its share of the heights;
    /// above it, the second's, or in patches the patches'
    private record Bands(double blend, int first, int second, int patches) {
        Bands(PlanetTextureSettings settings) {
            this(settings.blend(), settings.colors(Group.FIRST).size(), settings.colors(Group.SECOND).size(),
                    settings.colors(Group.PATCHES).size());
        }

        short code(double height, boolean patch) {
            Group group = group(height, patch);
            return PlanetTextureGenerator.code(group, Math.max(0, Math.min(size(group) - 1, (int) scaled(height, group))));
        }

        private Group group(double height, boolean patch) {
            if (height < blend || blend >= 1) return Group.FIRST;
            return patch && patches > 0 ? Group.PATCHES : Group.SECOND;
        }

        /// {@code height} in bands of {@code group}, from 0 at the bottom of its share
        private double scaled(double height, Group group) {
            if (group == Group.FIRST) return height / Math.max(1e-9, blend) * first;
            return (height - blend) / Math.max(1e-9, 1 - blend) * size(group);
        }

        private int size(Group group) {
            return switch (group) {
                case FIRST -> first;
                case PATCHES -> patches;
                default -> second;
            };
        }
    }

    /// The pixels of a cube's texture, by where they are on it
    private record Cube(int face, double[] x, double[] y, double[] z) {
        /// Where pixel {@code i} is on the cube, in pixels from its corner, from 0 to the face's size
        double texelX(int i) {
            return (x[i] + 1) / 2 * face;
        }

        double texelY(int i) {
            return (y[i] + 1) / 2 * face;
        }

        double texelZ(int i) {
            return (z[i] + 1) / 2 * face;
        }

        /// How far apart pixels {@code i} and {@code j} are on the cube, in pixels, as the crow flies
        double distance(int i, int j) {
            double dx = texelX(i) - texelX(j), dy = texelY(i) - texelY(j), dz = texelZ(i) - texelZ(j);
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        /// The four pixels next to each, left, right, up and down, four to a pixel: on the face beyond, where a pixel's
        /// on an edge
        int[] neighbours() {
            int width = face * CubeFace.COLUMNS;
            int[] neighbours = new int[x.length * 4];
            int[][] steps = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (FaceAxes axes : FACE_AXES) {
                for (int row = 0; row < face; row++) {
                    for (int column = 0; column < face; column++) {
                        int index = (axes.face.row * face + row) * width + axes.face.column * face + column;
                        for (int n = 0; n < 4; n++) {
                            neighbours[index * 4 + n] = pixelAt(axes.face, column + steps[n][0], row + steps[n][1]);
                        }
                    }
                }
            }
            return neighbours;
        }

        /// The pixel at {@code column}, {@code row} of {@code cubeFace}, or if that's just off it, the pixel it'd be
        /// folded over the edge onto
        private int pixelAt(CubeFace cubeFace, int column, int row) {
            int width = face * CubeFace.COLUMNS;
            if (column >= 0 && column < face && row >= 0 && row < face) {
                return (cubeFace.row * face + row) * width + cubeFace.column * face + column;
            }
            double s = (column + 0.5) / face, t = (row + 0.5) / face;
            double[] point = {cubeFace.x(s, t), cubeFace.y(s, t), cubeFace.z(s, t)};
            FaceAxes from = FACE_AXES.get(cubeFace.ordinal());
            for (int axis = 0; axis < 3; axis++) {
                double excess = Math.abs(point[axis]) - 1;
                if (axis == from.normal || excess <= 0) continue;
                // Over the edge, as far onto the next face as it was off this one
                point[axis] = Math.signum(point[axis]);
                point[from.normal] -= Math.signum(point[from.normal]) * excess;
                double at = point[axis] > 0 ? 1 : 0;
                for (FaceAxes to : FACE_AXES) {
                    if (to.normal != axis || to.normalAt != at) continue;
                    double[] texel = {(point[0] + 1) / 2 * face, (point[1] + 1) / 2 * face, (point[2] + 1) / 2 * face};
                    int toColumn = (int) Math.round(to.acrossSign * (texel[to.across] - to.originAt[to.across] * face) - 0.5);
                    int toRow = (int) Math.round(to.downSign * (texel[to.down] - to.originAt[to.down] * face) - 0.5);
                    return (to.face.row * face + toRow) * width + to.face.column * face + toColumn;
                }
            }
            throw new IllegalStateException("Nowhere past " + cubeFace + " " + column + ", " + row);
        }

        /// Overlapping blocks about {@code size} pixels across, covering the cube, or null if they'd be smaller than
        /// a pixel.
        ///
        /// Blocks are boxes in the cube's space, each of its own random size, moved about from a grid closer together
        /// than they are big, so they overlap. Where they do, the one on top, chosen at random, is shown, so they look
        /// like rectangles laid over one another. None of their edges go further than they do, so they don't line up
        /// into long, thin strips, as blocks on a grid would.
        ///
        /// Every block's at least big enough to reach the next along the grid, wherever both are moved to, so between
        /// them they cover the whole cube.
        @Nullable
        Blocks blocks(double size, SplittableRandom random) {
            if (size < MIN_BLOCK) return null;
            double step = size * BLOCK_STEP;
            double jitter = size * BLOCK_JITTER;
            double minExtent = step + 2 * jitter;
            double maxExtent = Math.max(minExtent, size * BLOCK_MAX);
            double phase = random.nextDouble() * step;
            double quantum = size * BLOCK_QUANTUM;
            // Not small ones, whose edges it would only pull into line with one another
            boolean rounded = quantum >= 2;

            // The grid along each axis, as far as a block could reach the cube, and which of it could reach a face
            int first = (int) Math.floor((-jitter - maxExtent - phase) / step);
            int last = (int) Math.ceil((face + jitter - phase) / step);
            int count = last - first + 1;
            boolean[] nearFace = new boolean[count];
            List<Integer> nearIndices = new ArrayList<>();
            for (int n = 0; n < count; n++) {
                double at = phase + (first + n) * step;
                double lowest = at - jitter, highest = at + jitter + maxExtent;
                nearFace[n] = lowest <= 0 && highest >= 0 || lowest <= face && highest >= face;
                if (nearFace[n]) nearIndices.add(n);
            }

            Blocks blocks = new Blocks(x.length);
            double[] start = new double[3], end = new double[3];
            for (int i = 0; i < count; i++) {
                for (int j = 0; j < count; j++) {
                    // Only blocks that could reach a face: any, once one axis is near one, or else those near one on
                    // the last axis
                    boolean anyK = nearFace[i] || nearFace[j];
                    int ks = anyK ? count : nearIndices.size();
                    for (int kn = 0; kn < ks; kn++) {
                        int k = anyK ? kn : nearIndices.get(kn);
                        int[] cells = {i, j, k};
                        for (int axis = 0; axis < 3; axis++) {
                            start[axis] = phase + (first + cells[axis]) * step + (random.nextDouble() * 2 - 1) * jitter;
                            end[axis] = start[axis] + minExtent + random.nextDouble() * (maxExtent - minExtent);
                            // Only ever outwards, so they still cover the cube between them
                            if (rounded) {
                                start[axis] = Math.floor(start[axis] / quantum) * quantum;
                                end[axis] = Math.ceil(end[axis] / quantum) * quantum;
                            }
                        }
                        paint(start, end, random.nextDouble(), blocks);
                    }
                }
            }
            return blocks;
        }

        /// Adds the block from {@code start} to {@code end}, in pixels from the cube's corner, to {@code blocks}, with
        /// the pixels it covers on every face it reaches, and puts it on top wherever it's above what's there
        private void paint(double[] start, double[] end, double priority, Blocks blocks) {
            int width = face * CubeFace.COLUMNS;
            int block = blocks.begin();
            for (FaceAxes axes : FACE_AXES) {
                // The face's pixels, taken as half a pixel inside the cube, as the pixels of the faces beside it along
                // their edges are, and covered the same way as them: from a block's start to before its end. A block
                // then covers a pixel along an edge just when it covers the pixel beside it on the next face, so it
                // folds over the edge rather than stopping at it. Taken as on the face itself, a block that only
                // touched it, as one rounded out to it would, would be drawn on it and not the next face.
                double plane = axes.normalAt == 0 ? 0.5 : face - 0.5;
                if (start[axes.normal] > plane || end[axes.normal] <= plane) continue;
                int columnFrom = axes.from(start[axes.across], end[axes.across], true, face);
                int columnTo = axes.to(start[axes.across], end[axes.across], true, face);
                int rowFrom = axes.from(start[axes.down], end[axes.down], false, face);
                int rowTo = axes.to(start[axes.down], end[axes.down], false, face);
                for (int row = rowFrom; row <= rowTo; row++) {
                    int rowStart = (axes.face.row * face + row) * width + axes.face.column * face;
                    for (int column = columnFrom; column <= columnTo; column++) {
                        blocks.cover(block, rowStart + column, priority);
                    }
                }
            }
            blocks.end();
        }
    }

    /// Blocks covering the cube: which pixels each covers, and which is on top at each pixel
    private static final class Blocks {
        /// The pixels each block covers, one block after another, and where each block's start
        private int[] pixels = new int[1024];
        private int pixelCount;
        private int[] starts = new int[64];
        private int blockCount;
        /// For each pixel, the block on top, and how high it is
        private final int[] top;
        private final double[] priorities;

        Blocks(int pixelCount) {
            top = new int[pixelCount];
            Arrays.fill(top, -1);
            priorities = new double[pixelCount];
            Arrays.fill(priorities, -1);
        }

        /// Starts a block, returning it
        int begin() {
            if (blockCount + 1 >= starts.length) starts = Arrays.copyOf(starts, starts.length * 2);
            starts[blockCount] = pixelCount;
            return blockCount;
        }

        void cover(int block, int pixel, double priority) {
            if (pixelCount == pixels.length) pixels = Arrays.copyOf(pixels, pixels.length * 2);
            pixels[pixelCount++] = pixel;
            if (priority > priorities[pixel]) {
                priorities[pixel] = priority;
                top[pixel] = block;
            }
        }

        /// Finishes the block begun last, keeping it if it covers anything
        void end() {
            if (pixelCount > starts[blockCount]) {
                blockCount++;
                starts[blockCount] = pixelCount;
            }
        }

        /// {@code values} in blocks: at each pixel, the median of them over the block on top there
        double[] median(double[] values) {
            // Only for blocks on top somewhere, as those under others everywhere aren't seen
            boolean[] seen = new boolean[blockCount];
            for (int block : top) if (block >= 0) seen[block] = true;
            double[] medians = new double[blockCount];
            double[] scratch = new double[64];
            for (int block = 0; block < blockCount; block++) {
                if (!seen[block]) continue;
                int from = starts[block], size = starts[block + 1] - from;
                if (scratch.length < size) scratch = new double[Math.max(size, scratch.length * 2)];
                for (int n = 0; n < size; n++) scratch[n] = values[pixels[from + n]];
                medians[block] = select(scratch, size, size / 2);
            }
            double[] result = new double[values.length];
            // Nothing should be left uncovered, but in case
            for (int i = 0; i < values.length; i++) result[i] = top[i] >= 0 ? medians[top[i]] : values[i];
            return result;
        }
    }

    /// The {@code k}th smallest of the first {@code size} of {@code values}, which are reordered: Hoare's quickselect,
    /// as only the median's wanted, not every value in order
    private static double select(double[] values, int size, int k) {
        int low = 0, high = size - 1;
        while (low < high) {
            double pivot = values[(low + high) >>> 1];
            int i = low, j = high;
            while (i <= j) {
                while (values[i] < pivot) i++;
                while (values[j] > pivot) j--;
                if (i <= j) {
                    double swap = values[i];
                    values[i] = values[j];
                    values[j] = swap;
                    i++;
                    j--;
                }
            }
            if (k <= j) high = j;
            else if (k >= i) low = i;
            else return values[k];
        }
        return values[k];
    }

    /// Which way each axis of a face runs on the cube, to find the pixels a box on the cube covers
    private static final List<FaceAxes> FACE_AXES = Arrays.stream(CubeFace.values()).map(FaceAxes::of).toList();

    /// @param normal   the axis the face is across, and {@code normalAt} where along it, as 0 or 1
    /// @param across   the axis its columns run along, and {@code acrossSign} which way, as 1 or -1
    /// @param down     the axis its rows run along, and {@code downSign} which way
    /// @param originAt where its top left corner is along each axis, from 0 to 1
    private record FaceAxes(CubeFace face, int normal, double normalAt, int across, int acrossSign, int down,
                            int downSign, double[] originAt) {
        static FaceAxes of(CubeFace face) {
            double[] origin = {face.x(0, 0), face.y(0, 0), face.z(0, 0)};
            double[] acrossVector = {face.x(1, 0) - origin[0], face.y(1, 0) - origin[1], face.z(1, 0) - origin[2]};
            double[] downVector = {face.x(0, 1) - origin[0], face.y(0, 1) - origin[1], face.z(0, 1) - origin[2]};
            int across = axisOf(acrossVector), down = axisOf(downVector);
            int normal = 3 - across - down;
            double[] originAt = {(origin[0] + 1) / 2, (origin[1] + 1) / 2, (origin[2] + 1) / 2};
            return new FaceAxes(face, normal, originAt[normal], across, (int) Math.signum(acrossVector[across]),
                    down, (int) Math.signum(downVector[down]), originAt);
        }

        private static int axisOf(double[] vector) {
            for (int axis = 0; axis < 3; axis++) if (vector[axis] != 0) return axis;
            throw new IllegalStateException();
        }

        /// The first column (or row) whose pixels' middles are from {@code low} to before {@code high} along its axis
        int from(double low, double high, boolean columns, int face) {
            int sign = columns ? acrossSign : downSign;
            double origin = originAt[columns ? across : down] * face;
            // A pixel's middle is at origin + sign * (n + 0.5)
            double n = sign > 0 ? Math.ceil(low - origin - 0.5) : Math.floor(origin - 0.5 - high) + 1;
            return (int) Math.max(0, n);
        }

        /// The last such column (or row)
        int to(double low, double high, boolean columns, int face) {
            int sign = columns ? acrossSign : downSign;
            double origin = originAt[columns ? across : down] * face;
            double n = sign > 0 ? Math.ceil(high - origin - 0.5) - 1 : Math.floor(origin - 0.5 - low);
            return (int) Math.min(face - 1, n);
        }
    }

    /// Perlin's gradient noise in three dimensions, with gradients in every direction rather than only along edges
    static final class Noise {
        private final int[] permutation = new int[512];
        private final double[] gradientX = new double[256], gradientY = new double[256], gradientZ = new double[256];

        Noise(SplittableRandom random) {
            int[] shuffled = new int[256];
            for (int i = 0; i < 256; i++) shuffled[i] = i;
            for (int i = 255; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int swap = shuffled[i];
                shuffled[i] = shuffled[j];
                shuffled[j] = swap;
            }
            for (int i = 0; i < 512; i++) permutation[i] = shuffled[i & 255];
            for (int i = 0; i < 256; i++) {
                double gx, gy, gz, length;
                do {
                    gx = random.nextGaussian();
                    gy = random.nextGaussian();
                    gz = random.nextGaussian();
                    length = Math.sqrt(gx * gx + gy * gy + gz * gz);
                } while (length < 1e-9);
                gradientX[i] = gx / length;
                gradientY[i] = gy / length;
                gradientZ[i] = gz / length;
            }
        }

        double at(double x, double y, double z) {
            int xi = (int) Math.floor(x), yi = (int) Math.floor(y), zi = (int) Math.floor(z);
            double xf = x - xi, yf = y - yi, zf = z - zi;
            xi &= 255;
            yi &= 255;
            zi &= 255;
            double u = fade(xf), v = fade(yf), w = fade(zf);
            double total = 0;
            for (int dx = 0; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        int hash = permutation[permutation[permutation[xi + dx] + yi + dy] + zi + dz];
                        double dot = gradientX[hash] * (xf - dx) + gradientY[hash] * (yf - dy) + gradientZ[hash] * (zf - dz);
                        total += (dx == 1 ? u : 1 - u) * (dy == 1 ? v : 1 - v) * (dz == 1 ? w : 1 - w) * dot;
                    }
                }
            }
            return total;
        }

        private static double fade(double t) {
            return t * t * t * (t * (t * 6 - 15) + 10);
        }
    }

    /// {@code pixels}, {@code 0xAARRGGBB} row by row, as a PNG, without alpha
    public static byte[] png(int width, int height, int[] pixels) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }
}
