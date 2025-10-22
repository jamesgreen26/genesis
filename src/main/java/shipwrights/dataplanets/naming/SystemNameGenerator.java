package shipwrights.dataplanets.naming;

import net.minecraft.util.RandomSource;

import static shipwrights.dataplanets.DataplanetsMod.FANTASY_SYSTEM_NAME_GENERATOR;

public interface SystemNameGenerator {

    static SystemNameGenerator get(boolean scientific) {
        if (scientific) {
            return new ScientificSystemNameGenerator();
        } else {
            return FANTASY_SYSTEM_NAME_GENERATOR;
        }
    }

    /**
     * Generate a system name
     * @param random Random source for generation
     * @return The generated system name
     */
    String generate(RandomSource random);

    class ScientificSystemNameGenerator implements SystemNameGenerator {

        private static final String CONSONANTS = "bcdfghjklmnpqrstvwxyz";
        private static final String ALL_LETTERS = "abcdefghijklmnopqrstuvwxyz";

        @Override
        public String generate(RandomSource random) {
            StringBuilder name = new StringBuilder();

            // First letter - consonant
            name.append(CONSONANTS.charAt(random.nextInt(CONSONANTS.length())));

            // Second letter - any letter
            name.append(ALL_LETTERS.charAt(random.nextInt(ALL_LETTERS.length())));

            // Three digits (100-999)
            int number = random.nextInt(100, 1000);
            name.append(number);

            return name.toString();
        }
    }
}
