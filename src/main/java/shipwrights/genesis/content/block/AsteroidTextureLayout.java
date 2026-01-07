package shipwrights.genesis.content.block;

import java.util.Random;

public class AsteroidTextureLayout {

    public final int size;
    public final int paletteSize;

    private int[] edgeTemplate;

    public final int[][] data;

    public static AsteroidTextureLayout generate(int size, int paletteSize) {
        AsteroidTextureLayout result = new AsteroidTextureLayout(size, paletteSize);

        Random random = new Random();

        result.generateEdgeTemplate(random);
        result.populateData(random);

        return result;
    }

    private void generateEdgeTemplate(Random random) {
        edgeTemplate = new int[size / 2];

        for (int i = 0; i < size / 2; i++) {
            edgeTemplate[i] = random.nextInt(paletteSize);
        }
    }

    private void populateData(Random random) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if ((y == 0 || y == size - 1) && x < size / 2) {
                    data[y][x] = edgeTemplate[x];
                } else if ((y == 0 || y == size - 1) && x >= size / 2) {
                    data[y][x] = edgeTemplate[size - 1 - x];
                } else if ((x == 0 || x == size - 1) && y < size / 2) {
                    data[y][x] = edgeTemplate[y];
                } else if ((x == 0 || x == size - 1) && y >= size / 2) {
                    data[y][x] = edgeTemplate[size - 1 - y];
                } else {
                    data[y][x] = random.nextInt(paletteSize);
                }
            }
        }
    }

    private AsteroidTextureLayout(int size, int paletteSize) {
        this.size = size;
        this.paletteSize = paletteSize;
        this.data = new int[size][size];
    }
}
