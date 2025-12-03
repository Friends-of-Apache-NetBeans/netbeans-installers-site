
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.joining;

class PatchUp {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: source patch");
            System.exit(2);
        }
        Path source = Path.of(args[0]);
        Path patch = Path.of(args[1]);
        System.out.println("source = " + source);
        System.out.println("patch = " + patch.toString());
        new PatchUp(source, patch).run();
    }

    static final Pattern START = Pattern.compile("^# patchMe.*");
    static final Pattern END = Pattern.compile("^# /patchMe.*");
    final Path source;
    final Path patch;
    String patchText;

    public PatchUp(Path source, Path patch) {
        this.source = source;
        this.patch = patch;
        try {
            patchText = Files.lines(patch).collect(joining("\n"));
        } catch (IOException ex) {
            System.err.println("can't  open patch file ex = " + patch.toString());
            System.exit(1);
        }
    }

    void run() {

        try {
            Files.lines(source)
                    .forEach(l -> {
                        activeFilter
                                .filter(this, l)
                                .ifPresent(System.out::println);
                    });
        } catch (IOException ex) {
            System.getLogger(PatchUp.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

    }

    Filter activeFilter = Filter.PASS;

    enum Filter {
        PASS {
            @Override
            Optional<String> filter(PatchUp ctx, String in) {
                if (START.matcher(in).matches()) {
                    ctx.activeFilter = Filter.REPLACE;
                    return Optional.of(in + "\n" + ctx.patchText);
                }
                return Optional.of(in);
            }
        }, REPLACE {
            @Override
            Optional<String> filter(PatchUp ctx, String in) {
                if (END.matcher(in).matches()) {
                    ctx.activeFilter = Filter.PASS;
                    return Optional.of(in);
                }
                return Optional.empty();
            }
        };

        abstract Optional<String> filter(PatchUp ctx, String in);
    }
}
