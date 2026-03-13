
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
/**
 *
 * @author homberghp {@code <pieter.van.den.hombergh@gmail.com>}
 */
public class AssetsProcessor {

    public static void main(String... argv) throws IOException {
        if (argv.length < 1) {
            throw new IllegalArgumentException("need the release tag");
        }
        var releaseTag = argv[0];
        String dist = "dist";
        if (argv.length >= 2) {
            dist = argv[1];
        }
        new AssetsProcessor(releaseTag, dist).process();
    }
    final String releaseTag;
    final Path distDir;
    final Path assetsFile;
    final Path propFile;
    Path workingDir = Path.of("");

    public AssetsProcessor(String releaseTag, String dist) {
        this.releaseTag = releaseTag;
        this.distDir = workingDir.resolve(dist);
        assetsFile = distDir.resolve("assets.txt");
        if (!Files.exists(assetsFile)) {
            throw new IllegalArgumentException("assetPath = " + assetsFile.toString() + " does not exist");
        }
        propFile = distDir.resolve("effective.properties");
        if (!Files.exists(propFile)) {
            throw new IllegalArgumentException("assetPath = " + propFile.toString() + " does not exist");
        }
    }

    Map<String, String> properties;
    static Pattern patternJDK = Pattern.compile("^.*[-._](?<arch>arm64|x86_64|amd64)?\\.(?<ext>exe|pkg|rpm|deb)$");
    static Pattern patternNoJDK = Pattern.compile("^.*\\.(?<ext>exe|pkg|rpm|deb)$");

    void process() throws IOException {
        properties = readProperties();
        properties.entrySet().forEach(System.out::println);
        List<String> lines = Files.readAllLines(assetsFile);
        lines.forEach(System.err::println);
        if ("none".equals(properties.get("jdk-variant"))) {
            processSansJDK(lines);
        } else {
            processWithJDK(lines);
        }
    }

    record Asset(String fileName, String url, String hash, String downloadCount) {

    }

    void processWithJDK(List<String> lines) throws FileNotFoundException {
        for (String line : lines) {
            Asset asset = lineToAsset(line);
            Matcher m = patternJDK.matcher(asset.fileName);
            String installer = "";
            String packageDir = "";
            if (m.matches()) {
                final var arch = switch (m.group("arch")) {
                    case "x86_64" ->
                        "x64";
                    case "arm64" ->
                        "arm64";
                    case "amd64" ->
                        "x64";
                    default ->
                        "x64";

                };

                String ext = m.group("ext");
                var d = "";
                switch (ext) {
                    case "exe" -> {
                        installer = "windows-exe-x64";
                        packageDir = "windows_x64";
                    }
                    case "pkg" -> {
                        installer = "macos-pkg-" + arch;
                        packageDir = "macos_" + arch;
                    }
                    case "deb" -> {
                        installer = "linux-deb-" + arch;
                        packageDir = "deb_" + arch;
                    }
                    case "rpm" -> {
                        installer = "linux-rpm-" + arch;
                        packageDir = "rpm_" + arch;
                    }
                    default -> {
                        installer = "windows-exe-x64";
                        packageDir = "windows_x64";
                    }
                };
            } else {
                installer = "windows-exe-x64";
                packageDir = "windows_x64";
            }

            try {
                writeYaml(packageDir, installer, asset);
            } catch (IOException ex) {
                System.getLogger(AssetsProcessor.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
    }

    void writeYaml(String packageDir, String installer, Asset asset) throws FileNotFoundException, IOException {
        Path outFile = workingDir.resolve("_data/").resolve(packageDir).resolve(releaseTag + ".yaml");
        Files.createDirectories(outFile.getParent());
        try (var out = new PrintStream(outFile.toFile());) {
            printPropertiesAsYaml(properties, out);
            writeAssetAsYaml(out, installer, asset);
        }
    }

    void writeAssetAsYaml(final PrintStream out, String installer, Asset asset) {
        out.println("  " + installer
                + "-file: " + asset.fileName);
        out.println("  " + installer
                + "-url: " + asset.url);

        String sha = asset.hash.split(":")[1];
        out.println("  " + installer
                + "-hash: " + sha);
        out.println("  " + installer
                + "-downloads: " + asset.downloadCount);
    }

    Asset lineToAsset(String line) throws IllegalArgumentException {
        String[] parts = line.split(";");
        if (parts.length < 3) {
            throw new IllegalArgumentException("asset lines must have at least 4 values");
        }
        var asset = new Asset(parts[0], parts[1], parts[2], parts[3]);
        return asset;
    }

    void printPropertiesAsYaml(Map<String, String> properties, final PrintStream out) {
        boolean first = true;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            out.println((first ? "- " : "  ") + entry.getKey() + ": " + entry.getValue());
            first = false;
        }
    }

    Map<String, String> readProperties() {
        Map<String, String> props = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(propFile);
            boolean first = true;
            for (String line : lines) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] split = line.split("\\s?=\\s?");
                if (split.length > 1) {
                    String key = split[0].replaceAll("\\.", "-");
                    String value = split[1];
                    props.put(key, value);
                }
                first = false;
            }
        } catch (IOException ex) {
            System.getLogger(AssetsProcessor.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        return props;
    }

    /**
     * Process the assets without a specified JDK. Writes a yaml file for two
     * architectures for deb and macos and one for windows-exe.
     *
     * @param lines assets lines
     */
    void processSansJDK(List<String> lines) throws FileNotFoundException, IOException {
        for (String line : lines) {
            Asset asset = lineToAsset(line);
            Matcher m = patternNoJDK.matcher(asset.fileName);
            String packageDir = "";
            String installer = "";
            if (m.matches()) {
                String ext = m.group("ext");
                switch (ext) {
                    case "deb" -> {
                        packageDir = "deb_x64";
                        installer = "linux-deb-x64";
                        writeYaml(packageDir, installer, asset);
                        packageDir = "deb_arm64";
                        installer = "linux-deb-arm64";
                        writeYaml(packageDir, installer, asset);
                    }
                    case "exe" -> {
                        packageDir = "windows_x64";
                        installer = "windows-exe-x64";
                        writeYaml(packageDir, installer, asset);
                    }
                    case "rpm" -> {
                        packageDir = "rpm_x64";
                        installer = "linux-rpm-x64";
                        writeYaml(packageDir, installer, asset);
                    }
                    case "pkg" -> {
                        packageDir = "macos_x64";
                        installer = "macos-pkg-x64";
                        writeYaml(packageDir, installer, asset);
                        packageDir = "macos_arm64";
                        installer = "macos-pkg-arm64";
                        writeYaml(packageDir, installer, asset);
                    }
                    default -> {
                    }
                }
            }
        }
    }

}
