package shipwrights.genesis.space.client.map.editor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import shipwrights.genesis.MinecraftBootstrap;
import shipwrights.genesis.space.client.map.editor.EditorProjects.Project;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorProjectsTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.bootstrap();
    }

    @Test
    void projectsAreMadeRenamedDuplicatedAndDeleted(@TempDir Path root) throws IOException {
        EditorProjects projects = new EditorProjects(root);
        assertEquals(List.of(), projects.list());

        Project first = projects.create("My Planets");
        Files.createDirectories(first.workspace().resolve("data"));
        Files.writeString(first.workspace().resolve("data/file.json"), "{}");
        Project renamed = projects.rename(first, "Renamed: anything goes?");
        assertEquals("Renamed: anything goes?", renamed.name());
        assertEquals(first.folder(), renamed.folder());

        Project copy = projects.duplicate(renamed, "My Planets");
        assertNotEquals(first.folder(), copy.folder());
        assertEquals("{}", Files.readString(copy.workspace().resolve("data/file.json")));
        assertEquals(2, projects.list().size());

        projects.delete(renamed);
        assertFalse(Files.exists(first.folder()));
        assertEquals(List.of(copy.folder()), projects.list().stream().map(Project::folder).toList());
    }

    @Test
    void importTakesOnlyThePacksFilesAndNothingOutsideTheProject(@TempDir Path root, @TempDir Path temp) throws IOException {
        Path zip = zip(temp.resolve("pack.zip"), Map.of(
                "pack.mcmeta", "{}",
                "assets/custom/genesis/celestial_visualizer/test.json", "{\"a\":1}",
                "data/custom/genesis/celestial/test.json", "{\"b\":2}",
                "data/../../escaped.json", "{}"));
        Project project = new EditorProjects(root).importZip(zip, "pack");

        assertEquals("pack", project.name());
        assertTrue(Files.isRegularFile(project.workspace().resolve("assets/custom/genesis/celestial_visualizer/test.json")));
        assertTrue(Files.isRegularFile(project.workspace().resolve("data/custom/genesis/celestial/test.json")));
        assertFalse(Files.exists(project.workspace().resolve("pack.mcmeta")));
        assertFalse(Files.exists(project.folder().resolve("escaped.json")));
        assertFalse(Files.exists(root.resolve("escaped.json")));
    }

    @Test
    void aPackInAFolderInTheZipIsImported(@TempDir Path root, @TempDir Path temp) throws IOException {
        Path zip = zip(temp.resolve("folder.zip"), Map.of(
                "__MACOSX/Archive/assets/._x.json", "junk",
                "Archive/assets/custom/genesis/celestial_visualizer/test.json", "{\"a\":1}",
                "Archive/data/custom/genesis/celestial/test.json", "{\"b\":2}"));
        Project project = new EditorProjects(root).importZip(zip, "folder");

        assertTrue(Files.isRegularFile(project.workspace().resolve("assets/custom/genesis/celestial_visualizer/test.json")));
        assertTrue(Files.isRegularFile(project.workspace().resolve("data/custom/genesis/celestial/test.json")));
        assertFalse(Files.exists(project.workspace().resolve("Archive")));
    }

    @Test
    void importWorksInAProjectsFolderGivenByARelativePath(@TempDir Path temp) throws IOException {
        Path zip = zip(temp.resolve("pack.zip"), Map.of("data/custom/genesis/celestial/test.json", "{}"));
        // As the game's folder is in a development run: "."
        Path root = Path.of(".").resolve(Path.of("").toAbsolutePath().relativize(temp.resolve("projects")));
        Project project = new EditorProjects(root).importZip(zip, "pack");

        assertTrue(Files.isRegularFile(project.workspace().resolve("data/custom/genesis/celestial/test.json")));
    }

    @Test
    void aZipWithoutAPackIsntImported(@TempDir Path root, @TempDir Path temp) throws IOException {
        Path zip = zip(temp.resolve("other.zip"), Map.of("readme.txt", "hello"));
        EditorProjects projects = new EditorProjects(root);
        assertThrows(IOException.class, () -> projects.importZip(zip, "other"));
        assertEquals(List.of(), projects.list());
    }

    private static Path zip(Path zip, Map<String, String> entries) throws IOException {
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream zipOut = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zipOut.putNextEntry(new ZipEntry(entry.getKey()));
                zipOut.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zipOut.closeEntry();
            }
        }
        return zip;
    }
}
