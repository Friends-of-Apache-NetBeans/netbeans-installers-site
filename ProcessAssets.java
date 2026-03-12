
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Parse the build properties file. Build properties and and yaml files differ
 * in allowed syntax. Also, not all properties are relevant.
 *
 * For the general properties, in particular jdk info, replace dost with dashes.
 *
 * For the various binaries, map the package extension and os/arch info to yaml
 * properties.
 *
 */
class ProcessAssets {

    public static void main(String... argv) throws IOException {
        if (argv.length < 1) {
            throw new IllegalArgumentException("need the release tag");
        }
        var releaseTag = argv[0];
        Path workingDir = Path.of("");
//        PrintStream out = System.out;
        Pattern pattern = Pattern.compile(".*[-._](?<arch>arm64|x86_64|amd64)?\\.(?<installerType>exe|pkg|rpm|deb)$");
        String fileName = "dist/assets.txt";
        Path path = Path.of(fileName);
        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            String[] parts = line.split(";");
            Matcher m = pattern.matcher(parts[0]);
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

                String ext = m.group("installerType");
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

            Path outFile = workingDir.resolve("_data/").resolve(packageDir).resolve(releaseTag + ".yaml");
            try (var out = new PrintStream(outFile.toFile());) {
                readProperties(out);
                out.println("  "+installer
                        + "-file: " + parts[0]);
                String url = parts[1];
                out.println("  "+installer
                        + "-url: " + url);

                String sha = parts[2].split(":")[1];
                out.println("  "+installer
                        + "-hash: " + sha);
                String downloads = parts[3];
                out.println("  "+installer
                        + "-downloads: " + downloads);
            }
        }
    }

    static void readProperties(PrintStream out) {
        Map<String, String> props = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(Path.of("dist", "effective.properties"));
            boolean first=true;
            for (String line : lines) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] split = line.split("\\s?=\\s?");
                if (split.length > 1) {
                    String key = split[0].replaceAll("\\.", "-");
                    String value = split[1];
                    props.put(key, split[1]);
                    out.println((first?"- ":"  ")+key + ": " + value);
                }
                first=false;
            }
        } catch (IOException ex) {
            System.getLogger(ProcessAssets.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

    }
}
