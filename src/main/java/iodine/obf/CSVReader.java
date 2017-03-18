package iodine.obf;

import net.techcable.srglib.mappings.Mappings;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

public class CSVReader {

    private final Mappings mappings;

    public CSVReader(InputStream inputStream) throws Exception {
        Map<String, String> map = new HashMap<>();
        List<String> lines = readLines(inputStream);
        lines.remove(0);
        lines.forEach((line) -> {
            String[] seperated = line.split(",");
            map.put(seperated[0], seperated[1]);
        });
        mappings = Mappings.createRenamingMappings((javaType -> javaType), (methodData -> {
            if (map.containsKey(methodData.getName())) {
                return map.get(methodData.getName());
            }
            return methodData.getName();
        }), (fieldData -> {
            if (map.containsKey(fieldData.getName())) {
                return map.get(fieldData.getName());
            }
            return fieldData.getName();
        }));
    }

    public CSVReader(List<String> strings) {
        Map<String, String> map = new HashMap<>();
        strings.forEach((line) -> {
            String[] seperated = line.split(",");
            map.put(seperated[0], seperated[1]);
        });
        mappings = Mappings.createRenamingMappings((javaType -> javaType), (methodData -> {
            if (map.containsKey(methodData.getName())) {
                return map.get(methodData.getName());
            }
            return methodData.getName();
        }), (fieldData -> {
            if (map.containsKey(fieldData.getName())) {
                return map.get(fieldData.getName());
            }
            return fieldData.getName();
        }));
    }

    public static List<String> findFile(String fileName, URL url) throws Exception {
        JarInputStream jarInputStream = new JarInputStream(url.openStream());
        for (JarEntry e; (e = jarInputStream.getNextJarEntry()) != null;) {
            Path path = Paths.get(e.getName());
            if (path.getFileName().toString().equals(fileName)) {
                List<String> lines = new ArrayList<>();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader
                        (jarInputStream));
                lines.addAll(bufferedReader.lines().collect(Collectors.toList()));
                return lines;
            }
        }
        return new ArrayList<>();
    }

    public static Mappings readMCPBotPackage(URL url) throws Exception {
        if (SRGReader.isValidUrl(url)) {
            List<String> combined = new ArrayList<>();
            List<String> lineA = findFile("methods.csv", url);
            lineA.remove(0);
            combined.addAll(lineA);
            List<String> lineB = findFile("fields.csv", url);
            lineB.remove(0);
            combined.addAll(lineB);
            return new CSVReader(combined).getMappings();
        }
        throw new RuntimeException("This isn't supposed to happen...");
    }

    private List<String> readLines(InputStream stream) {
        List<String> strings = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
        strings.addAll(bufferedReader.lines().collect(Collectors.toList()));
        return strings;
    }

    public Mappings getMappings() {
        return mappings;
    }

}
