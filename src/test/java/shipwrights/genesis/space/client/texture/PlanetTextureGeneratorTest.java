package shipwrights.genesis.space.client.texture;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.space.client.CubeFace;
import shipwrights.genesis.space.client.CubeFaceTest;
import shipwrights.genesis.space.client.texture.PlanetTextureSettings.Group;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanetTextureGeneratorTest {
    private static final int FACE = 64;

    /// Between them, every part of the generator: the defaults, grey with specks; the same without blocks; one like the
    /// overworld, with patches and poles; and one in a single palette, without specks
    private enum Kind {
        DEFAULT(PlanetTextureSettings.DEFAULT),
        SMOOTH(PlanetTextureSettings.DEFAULT.withBlockiness(0)),
        EARTHLIKE(new PlanetTextureSettings(FACE, 0, 0.55, 0.5, 0.5, 0.5, 0.75, Map.of(
                Group.FIRST, List.of(0x001C45, 0x002356, 0x003461, 0x003C6F),
                Group.SECOND, List.of(0x789056, 0x3E7A39, 0x386D33, 0x305F2C, 0x274D24),
                Group.PATCHES, List.of(0xC8B88D, 0xD5C496, 0xBDAE86, 0xB1A37C, 0xA69974),
                Group.POLES, List.of(0x99CDE4, 0xF3FFFF)))),
        PLAIN(new PlanetTextureSettings(FACE, 0, 0, 0.8, 0.9, 0.2, 1, Map.of(
                Group.FIRST, List.of(0x3B1A14),
                Group.SECOND, List.of(0x3B1A14, 0x5A2A1D, 0x7A3B24, 0x96502E, 0xB0683A, 0xC98A55, 0xDFB07C))));

        final PlanetTextureSettings settings;

        Kind(PlanetTextureSettings settings) {
            this.settings = settings;
        }
    }

    private static PlanetTextureSettings settings(Kind kind, long seed) {
        return kind.settings.withFaceSize(FACE).withSeed(seed);
    }

    @Test
    void isThreeFacesWideAndTwoHigh() {
        assertEquals(FACE * 3 * FACE * 2, PlanetTextureGenerator.generate(settings(Kind.DEFAULT, 1)).length);
    }

    @Test
    void theSameSeedMakesTheSamePlanet() {
        for (Kind preset : Kind.values()) {
            assertArrayEquals(PlanetTextureGenerator.generate(settings(preset, 42)),
                    PlanetTextureGenerator.generate(settings(preset, 42)), preset.toString());
            assertFalse(Arrays.equals(PlanetTextureGenerator.generate(settings(preset, 42)),
                    PlanetTextureGenerator.generate(settings(preset, 43))), preset.toString());
        }
    }

    @Test
    void usesOnlyThePalettesColoursAndMostOfThem() {
        for (Kind preset : Kind.values()) {
            PlanetTextureSettings settings = settings(preset, 7);
            Set<Integer> palette = new HashSet<>();
            settings.palette().values().forEach(colors -> colors.forEach(color -> palette.add(0xFF000000 | color)));
            Set<Integer> used = new HashSet<>();
            for (int pixel : PlanetTextureGenerator.generate(settings)) used.add(pixel);
            assertTrue(palette.containsAll(used), preset.toString());
            for (Group group : List.of(Group.FIRST, Group.SECOND)) {
                // Not the first palette when blended away
                if (group == Group.FIRST && settings.blend() == 0) continue;
                int colors = settings.colors(group).size();
                long colorsUsed = settings.colors(group).stream().filter(color -> used.contains(0xFF000000 | color)).count();
                assertTrue(colorsUsed >= colors - 1, () -> preset + " used " + colorsUsed + " of " + colors + " " + group);
            }
        }
    }

    @Test
    void recolouringReusesTheLayout() {
        PlanetTextureSettings settings = settings(Kind.EARTHLIKE, 3);
        PlanetTextureSettings recoloured = settings.withColor(Group.FIRST, 0, 0x123456);
        assertTrue(settings.sameLayout(recoloured));
        assertArrayEquals(PlanetTextureGenerator.generate(recoloured),
                PlanetTextureGenerator.layout(settings).paint(recoloured));
    }

    /// Colours change no more across the cube's edges than between any two pixels side by side within a face
    @Test
    void carriesOnAcrossEveryEdge() {
        // Several seeds, as blocks are placed by them, and only some might come out wrong along an edge
        for (Kind preset : Kind.values()) for (long seed : new long[]{1, 3, 11}) {
            PlanetTextureSettings settings = settings(preset, seed);
            int[] pixels = PlanetTextureGenerator.generate(settings);
            int width = settings.width();

            double seams = 0;
            int seamCount = 0;
            for (CubeFace face : CubeFace.values()) {
                for (int i = 0; i < FACE; i++) {
                    // Off the left and right edges, and the top and bottom
                    int[][] edges = {{-1, i, 0, i}, {FACE, i, FACE - 1, i}, {i, -1, i, 0}, {i, FACE, i, FACE - 1}};
                    for (int[] edge : edges) {
                        CubeFaceTest.Neighbour neighbour = CubeFaceTest.acrossEdge(face, FACE, edge[0], edge[1]);
                        int here = pixel(pixels, width, face, edge[2], edge[3]);
                        int there = pixel(pixels, width, neighbour.face(), neighbour.column(FACE), neighbour.row(FACE));
                        seams += difference(here, there);
                        seamCount++;
                    }
                }
            }

            double inside = 0;
            int insideCount = 0;
            for (CubeFace face : CubeFace.values()) {
                for (int row = 0; row < FACE; row++) {
                    for (int column = 0; column + 1 < FACE; column++) {
                        inside += difference(pixel(pixels, width, face, column, row), pixel(pixels, width, face, column + 1, row));
                        insideCount++;
                    }
                }
            }
            double seamMean = seams / seamCount, insideMean = inside / insideCount;
            assertTrue(seamMean <= insideMean * 1.25,
                    () -> preset + " " + settings.seed() + ": " + seamMean + " across edges, but " + insideMean + " within faces");
        }
    }

    private static int pixel(int[] pixels, int width, CubeFace face, int column, int row) {
        return pixels[(face.row * FACE + row) * width + face.column * FACE + column];
    }

    private static int difference(int a, int b) {
        return Math.abs((a >> 16 & 0xFF) - (b >> 16 & 0xFF)) + Math.abs((a >> 8 & 0xFF) - (b >> 8 & 0xFF))
                + Math.abs((a & 0xFF) - (b & 0xFF));
    }

    @Test
    void writesAPngOfItsSize() throws IOException {
        PlanetTextureSettings settings = settings(Kind.PLAIN, 5);
        int[] pixels = PlanetTextureGenerator.generate(settings);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(
                PlanetTextureGenerator.png(settings.width(), settings.height(), pixels)));
        assertEquals(settings.width(), image.getWidth());
        assertEquals(settings.height(), image.getHeight());
        assertEquals(pixels[1234], image.getRGB(1234 % settings.width(), 1234 / settings.width()));
    }

    @Test
    void settingsSurviveBeingSaved() {
        PlanetTextureSettings settings = settings(Kind.EARTHLIKE, -9).withBlend(0.4).withNoisiness(0.3).withBlockiness(0.7).withDitherSpread(0.9).withDitherCoverage(0.3);
        var json = PlanetTextureSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow();
        assertEquals(settings, PlanetTextureSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void theBlendIsHowMuchIsInTheFirstPalette() {
        for (double blend : new double[]{0.2, 0.7}) {
            PlanetTextureSettings settings = settings(Kind.DEFAULT, 4).withBlend(blend)
                    .withColors(Group.SPECKS, List.of());
            Set<Integer> first = new HashSet<>();
            settings.colors(Group.FIRST).forEach(color -> first.add(0xFF000000 | color));
            int[] pixels = PlanetTextureGenerator.generate(settings);
            double share = Arrays.stream(pixels).filter(first::contains).count() / (double) pixels.length;
            // Give or take dither where the palettes meet
            assertEquals(blend, share, 0.05, () -> "blend " + blend);
        }
    }

    /// Without blocks, which smooth over fine detail, or dither, which adds its own, noisiness is all there is to it
    @Test
    void noisierIsBusier() {
        PlanetTextureSettings settings = settings(Kind.SMOOTH, 8).withDitherSpread(0)
                .withColors(Group.SPECKS, List.of());
        double calm = changesBetweenNeighbours(PlanetTextureGenerator.generate(settings.withNoisiness(0)));
        double noisy = changesBetweenNeighbours(PlanetTextureGenerator.generate(settings.withNoisiness(1)));
        assertTrue(noisy > calm * 1.5, () -> calm + " calm, " + noisy + " noisy");
    }

    /// How often pixels side by side differ
    private static double changesBetweenNeighbours(int[] pixels) {
        int width = FACE * 3, changes = 0;
        for (int i = 0; i + 1 < pixels.length; i++) if (i % width != width - 1 && pixels[i] != pixels[i + 1]) changes++;
        return changes / (double) pixels.length;
    }

    /// Blocks approximate the surface as it is without them: they mostly agree with it, and much more than with
    /// another surface
    @Test
    void blocksFollowTheSmoothSurface() {
        for (double blockiness : new double[]{0.25, 0.5, 1}) {
            PlanetTextureSettings blocky = settings(Kind.DEFAULT, 6).withBlockiness(blockiness)
                    .withColors(Group.SPECKS, List.of());
            int[] blocks = PlanetTextureGenerator.generate(blocky);
            double own = agreement(blocky, blocks, PlanetTextureGenerator.generate(blocky.withBlockiness(0)));
            double other = agreement(blocky, blocks, PlanetTextureGenerator.generate(blocky.withBlockiness(0).withSeed(60)));
            assertTrue(own > 0.75 && own > other + 0.2,
                    () -> "At " + blockiness + ", " + own + " within a band of its own surface, " + other + " of another");
        }
    }

    /// How much of {@code a} is within a band of {@code b}
    private static double agreement(PlanetTextureSettings settings, int[] a, int[] b) {
        int nearby = 0;
        for (int i = 0; i < a.length; i++) if (Math.abs(rank(settings, a[i]) - rank(settings, b[i])) <= 1) nearby++;
        return nearby / (double) a.length;
    }

    /// Dither's a transition where colours meet: it only changes pixels near an edge between them, and more of them
    /// the more there is
    @Test
    void ditherHugsTheEdgesBetweenColours() {
        PlanetTextureSettings settings = settings(Kind.DEFAULT, 9).withColors(Group.SPECKS, List.of())
                .withDitherCoverage(1);
        int[] hard = PlanetTextureGenerator.generate(settings.withDitherSpread(0));
        int width = settings.width();
        // As far as dither reaches at full, on these faces, past the edge's own pixel
        int reach = 8 * FACE / 128 * 3 / 2 + 1;
        int previous = 0;
        for (double dither : new double[]{0.25, 0.5, 1}) {
            int[] dithered = PlanetTextureGenerator.generate(settings.withDitherSpread(dither));
            int changed = 0;
            for (CubeFace face : CubeFace.values()) {
                for (int row = 0; row < FACE; row++) {
                    for (int column = 0; column < FACE; column++) {
                        int here = pixel(hard, width, face, column, row);
                        if (pixel(dithered, width, face, column, row) == here) continue;
                        changed++;
                        // Faces' edges aside, where the edge it's near may be on the next face
                        if (column < reach || row < reach || column >= FACE - reach || row >= FACE - reach) continue;
                        boolean nearEdge = false;
                        for (int dy = -reach; dy <= reach && !nearEdge; dy++) {
                            for (int dx = -reach; dx <= reach && !nearEdge; dx++) {
                                nearEdge = pixel(hard, width, face, column + dx, row + dy) != here;
                            }
                        }
                        int c = column, r = row;
                        assertTrue(nearEdge, () -> "Dithered far from any edge at " + face + " " + c + ", " + r);
                    }
                }
            }
            int changedAt = changed;
            int before = previous;
            assertTrue(changed > previous, () -> changedAt + " changed at " + dither + ", " + before + " with less");
            previous = changed;
        }
    }

    /// Dither thins out away from an edge: the colour across it is half of what's next to the edge, and less further in
    @Test
    void ditherThinsAwayFromTheEdge() {
        PlanetTextureSettings settings = settings(Kind.DEFAULT, 12).withColors(Group.SPECKS, List.of())
                .withDitherSpread(1).withDitherCoverage(1);
        int[] hard = PlanetTextureGenerator.generate(settings.withDitherSpread(0));
        int[] dithered = PlanetTextureGenerator.generate(settings);
        int width = settings.width();
        // How many pixels changed, by how far they are from the nearest edge in the undithered texture, within faces
        int[] changed = new int[4], all = new int[4];
        for (CubeFace face : CubeFace.values()) {
            for (int row = 4; row < FACE - 4; row++) {
                for (int column = 4; column < FACE - 4; column++) {
                    int here = pixel(hard, width, face, column, row);
                    int distance = 4;
                    for (int d = 1; d < 4 && distance == 4; d++) {
                        for (int dy = -d; dy <= d && distance == 4; dy++) {
                            for (int dx = -d; dx <= d && distance == 4; dx++) {
                                if (pixel(hard, width, face, column + dx, row + dy) != here) distance = d - 1;
                            }
                        }
                    }
                    if (distance == 4) continue;
                    all[distance]++;
                    if (pixel(dithered, width, face, column, row) != here) changed[distance]++;
                }
            }
        }
        double nextToEdge = changed[0] / (double) all[0], further = changed[2] / (double) all[2];
        assertEquals(0.5, nextToEdge, 0.15, "share changed next to an edge");
        assertTrue(further < nextToEdge * 0.75, () -> further + " changed two pixels from an edge, " + nextToEdge + " next to one");
    }

    /// Coverage picks how much of the edges are dithered: none at all at nothing, and more the more there is
    @Test
    void coverageIsHowMuchOfTheEdgesAreDithered() {
        PlanetTextureSettings settings = settings(Kind.DEFAULT, 10).withColors(Group.SPECKS, List.of());
        int[] hard = PlanetTextureGenerator.generate(settings.withDitherSpread(0));
        assertArrayEquals(hard, PlanetTextureGenerator.generate(settings.withDitherCoverage(0)));
        int previous = 0;
        for (double coverage : new double[]{0.25, 0.5, 0.75, 1}) {
            int[] dithered = PlanetTextureGenerator.generate(settings.withDitherCoverage(coverage));
            int changed = 0;
            for (int i = 0; i < hard.length; i++) if (dithered[i] != hard[i]) changed++;
            int changedAt = changed, before = previous;
            assertTrue(changed > previous, () -> changedAt + " changed at " + coverage + ", " + before + " with less");
            previous = changed;
        }
    }

    /// Where {@code pixel}'s colour is among the first and second palettes, from low to high
    private static int rank(PlanetTextureSettings settings, int pixel) {
        List<Integer> all = new java.util.ArrayList<>(settings.colors(Group.FIRST));
        all.addAll(settings.colors(Group.SECOND));
        return all.indexOf(pixel & 0xFFFFFF);
    }
}
