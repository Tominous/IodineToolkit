package iodine.obf;

import net.techcable.srglib.format.MappingsFormat;
import net.techcable.srglib.mappings.Mappings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class SRGReader {

    private final Mappings mappings;

    public SRGReader(InputStream inputStream) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        mappings = MappingsFormat.SEARGE_FORMAT.parseLines(bufferedReader.lines().collect
                (Collectors.toList()));
    }

    public SRGReader(List<String> strings) {
        mappings = MappingsFormat.SEARGE_FORMAT.parseLines(strings);
    }

    public static boolean isValidUrl(String url) throws Exception {
        return isValidUrl(new URL(url));
    }

    public static boolean isValidUrl(URL url) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
        httpURLConnection.setConnectTimeout((200 * 1000));
        httpURLConnection.setReadTimeout((200 * 1000));
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode == 404 || responseCode == 403) {
            return false;
        }
        return true;
    }

    public static void main(String... args) throws Exception {
        System.out.println(getSrgFor("1.7.10").toString());
    }

    public static Mappings getSrgFor(String version) throws Exception {
        URL url = new URL("http://mcpbot.bspk.rs/mcp/" + version + "/mcp-" + version + "-srg.zip");
        if (isValidUrl(url)) {
            List<String> srgFiles = CSVReader.findFile("joined.srg", url);
            return new SRGReader(srgFiles).getMappings();
        }
        throw new RuntimeException("This isn't supposed to happen...");
    }

    public Mappings getMappings() {
        return mappings;
    }

}
