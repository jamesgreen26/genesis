package shipwrights.genesis.space.client.map.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.FileUtil;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// The celestial editor's projects, each a {@link CelestialProject} of its own, kept apart as the game keeps its
/// worlds. Each is a folder in {@code genesis/projects}, named after it, holding:
///
/// - {@value #INFO_FILE}: its name, which may be anything, unlike the folder's
/// - {@value #PACK_FOLDER}/: its celestials' files, laid out as a pack: the {@link CelestialProject}'s workspace
public final class EditorProjects {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String INFO_FILE = "project.json";
    private static final String PACK_FOLDER = "pack";
    /// Put in zips by macOS, beside what was zipped
    private static final String MACOS_METADATA = "__MACOSX/";

    private final Path root;

    /// A project
    ///
    /// @param folder    its folder, in the projects folder
    /// @param name      what it's called
    /// @param lastSaved when its celestials, or failing that its info, were last written
    public record Project(Path folder, String name, long lastSaved) {
        /// Where its celestials' files are kept
        public Path workspace() {
            return folder.resolve(PACK_FOLDER);
        }

        /// The folder's name, safe in file names, as of what's exported from it
        public String fileName() {
            return folder.getFileName().toString();
        }
    }

    /// The projects in {@code root}, which needn't exist yet
    public EditorProjects(Path root) {
        this.root = root;
    }

    /// The projects folder, made if it isn't there yet
    public Path root() throws IOException {
        return Files.createDirectories(root);
    }

    /// Every project, most recently saved first
    public List<Project> list() {
        if (!Files.isDirectory(root)) return List.of();
        List<Project> projects = new ArrayList<>();
        try (Stream<Path> folders = Files.list(root)) {
            for (Path folder : folders.filter(EditorProjects::isProject).toList()) projects.add(read(folder));
        } catch (IOException e) {
            LOGGER.error("Couldn't list the celestial editor's projects in {}", root, e);
        }
        projects.sort(Comparator.comparingLong(Project::lastSaved).reversed().thenComparing(Project::name));
        return projects;
    }

    private static boolean isProject(Path folder) {
        return Files.isRegularFile(folder.resolve(INFO_FILE));
    }

    private static Project read(Path folder) {
        String name = folder.getFileName().toString();
        try {
            JsonObject info = JsonParser.parseString(Files.readString(folder.resolve(INFO_FILE))).getAsJsonObject();
            if (info.has("name")) name = info.get("name").getAsString();
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Couldn't read the celestial editor project in {}", folder, e);
        }
        return new Project(folder, name, lastSaved(folder));
    }

    private static long lastSaved(Path folder) {
        for (Path path : List.of(folder.resolve(PACK_FOLDER), folder.resolve(INFO_FILE))) {
            try {
                if (Files.exists(path)) return Files.getLastModifiedTime(path).toMillis();
            } catch (IOException ignored) {
                // Tried the next
            }
        }
        return 0;
    }

    /// A new, empty project called {@code name}
    public Project create(String name) throws IOException {
        Path folder = newFolder(name);
        writeInfo(folder, name);
        return read(folder);
    }

    /// Calls {@code project} {@code name}. Its folder keeps its name.
    public Project rename(Project project, String name) throws IOException {
        writeInfo(project.folder(), name);
        return read(project.folder());
    }

    /// A copy of {@code project}, called {@code name}
    public Project duplicate(Project project, String name) throws IOException {
        Path folder = newFolder(name);
        copyTree(project.folder(), folder);
        writeInfo(folder, name);
        return read(folder);
    }

    /// Deletes {@code project}, and everything in it
    public void delete(Project project) throws IOException {
        deleteTree(project.folder());
    }

    /// A new project called {@code name}, holding the celestials of the pack {@code zip}, as exported: its
    /// {@code assets} and {@code data}, and nothing else. They may be in a folder in the zip, as when a pack's folder
    /// was zipped rather than what's in it.
    ///
    /// @throws IOException if it can't be read, or there's nothing of a pack in it
    public Project importZip(Path zip, String name) throws IOException {
        Path folder = newFolder(name);
        // Normalized as the files' paths are, to tell whether they're in it
        Path workspace = folder.resolve(PACK_FOLDER).toAbsolutePath().normalize();
        int imported = 0;
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            List<? extends ZipEntry> entries = zipFile.stream().filter(entry -> !entry.isDirectory()).toList();
            String root = packRoot(entries);
            for (ZipEntry entry : entries) {
                if (!entry.getName().startsWith(root)) continue;
                String path = entry.getName().substring(root.length());
                if (!isPackFile(path)) continue;
                Path target = workspace.resolve(path).normalize();
                // Never outside the workspace, whatever the zip says
                if (!target.startsWith(workspace)) continue;
                Files.createDirectories(target.getParent());
                try (InputStream in = zipFile.getInputStream(entry)) {
                    Files.copy(in, target);
                }
                imported++;
            }
        } catch (IOException | RuntimeException e) {
            deleteTree(folder);
            throw e instanceof IOException io ? io : new IOException(e);
        }
        if (imported == 0) {
            deleteTree(folder);
            throw new IOException("No assets or data folder in " + zip);
        }
        writeInfo(folder, name);
        return read(folder);
    }

    /// Where the pack starts in the zip: at the top, if its files are there, or else in the first folder that has
    /// them. Folders of metadata macOS adds are passed over.
    private static String packRoot(List<? extends ZipEntry> entries) {
        String found = null;
        for (ZipEntry entry : entries) {
            String name = entry.getName();
            if (name.startsWith(MACOS_METADATA)) continue;
            if (isPackFile(name)) return "";
            int slash = name.indexOf('/');
            if (found == null && slash > 0 && isPackFile(name.substring(slash + 1))) found = name.substring(0, slash + 1);
        }
        return found == null ? "" : found;
    }

    private static boolean isPackFile(String name) {
        return name.startsWith(PackType.CLIENT_RESOURCES.getDirectory() + "/")
                || name.startsWith(PackType.SERVER_DATA.getDirectory() + "/")
                || name.equals(CelestialProject.GENERATORS_FILE);
    }

    /// A folder named for {@code name} that no other project has, made
    private Path newFolder(String name) throws IOException {
        Files.createDirectories(root);
        String fileName = FileUtil.findAvailableName(root, name.isBlank() ? "project" : name, "");
        return Files.createDirectory(root.resolve(fileName));
    }

    private static void writeInfo(Path folder, String name) throws IOException {
        JsonObject info = new JsonObject();
        info.addProperty("name", name);
        Files.writeString(folder.resolve(INFO_FILE), info.toString(), StandardCharsets.UTF_8);
    }

    private static void copyTree(Path from, Path to) throws IOException {
        Files.walkFileTree(from, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(to.resolve(from.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, to.resolve(from.relativize(file).toString()));
                return FileVisitResult.CONTINUE;
            }
        });
        // Saved now, as far as the list's concerned
        Path pack = to.resolve(PACK_FOLDER);
        if (Files.exists(pack)) Files.setLastModifiedTime(pack, FileTime.fromMillis(System.currentTimeMillis()));
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }
}
