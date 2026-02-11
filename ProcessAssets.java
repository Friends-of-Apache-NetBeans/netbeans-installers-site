
//import module java.base;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

class ProcessAssets {

    public static void main(String... argv) throws IOException {
        readProperties();
        Pattern pattern = Pattern.compile(".*[-._](?<arch>arm64|x86_64|amd64)?\\.(?<installerType>exe|pkg|rpm|deb)$");
        String fileName = "dist/assets.txt";
        Path path = Path.of(fileName);
        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            String[] parts = line.split(";");
//            System.out.println("parts[0] = " + parts[0]);
            Matcher m = pattern.matcher(parts[0]);
            String installer = null;
            if (m.matches()) {
//                System.out.println("m = " + m.groupCount());
                String installerMatch = m.group("installerType");
                String archMatch = switch (m.group("arch")) {
                    case "x86_64" ->
                        "x64";
                    case "arm64" ->
                        "arm";
                    case "amd64" ->
                        "x64";
                    default ->
                        "unknown";
                };
//                System.out.println("archMatch = " + archMatch);
                installer = switch (installerMatch) {
                    case "exe" ->
                        "windows-exe-x64";
                    case "pkg" ->
                        "macos-pkg-" + archMatch;
                    case "deb" ->
                        "linux-deb-" + archMatch;
                    case "rpm" ->
                        "linux-rpm-" + archMatch;
                    default ->
                        "windows-exe-x64";
                };
            } else {
                installer = "windows-exe-x64";
            }
            System.out.println(installer
                    + "-file: " + parts[0]);
            String url = parts[1];
            System.out.println(installer
                    + "-url: " + url);

            String sha = parts[2].split(":")[1];
            System.out.println(installer
                    + "-hash: " + sha);
            System.out.println("");
        }
    }

    static void readProperties() {
        Map<String, String> props = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(Path.of("dist", "build.properties"));
            for (String line : lines) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] split = line.split("\\s?=\\s?");
                if (split.length > 1) {
                    props.put(split[0], split[1]);
                }
            }
        } catch (IOException ex) {
            System.getLogger(ProcessAssets.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        System.out.println("netbeans-version: " + props.get("netbeans.version"));
        System.out.println("jdk-version: " + props.get("jdk.version"));
        System.out.println("");
    }
}
