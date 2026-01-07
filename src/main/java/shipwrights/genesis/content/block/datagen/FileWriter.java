package shipwrights.genesis.content.block.datagen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileWriter {
    static void writeFile(String filename, String contents) {
        try {
            Path path = Path.of(filename);

            Files.createDirectories(path.getParent());

            Files.writeString(
                    path,
                    contents,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            System.out.println("File written successfully: " + filename);
        } catch (IOException e) {
            System.err.println("Error writing file: " + e);
        }
    }
}
