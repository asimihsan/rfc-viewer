package art.kittencat;

import com.github.luben.zstd.Zstd;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class RfcReader implements AutoCloseable {
    private static final Gson gson = new Gson();
    private static final Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
    private static final Type listType = new TypeToken<List<String>>() {}.getType();

    private final BufferedReader br;
    private boolean isClosed = false;

    public RfcReader(Path preprocessedRfcs) throws IOException {
        this.br = Files.newBufferedReader(preprocessedRfcs);
    }

    public Rfc getNextRfc() {
        if (isClosed) {
            return null;
        }
        String nextLine = getNextLine();
        if (nextLine == null) {
            isClosed = true;
            return null;
        }
        return stringToRfc(nextLine);
    }

    private String getNextLine() {
        try {
            return br.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String jsonDecompress(String base64Encoded) {
        if (base64Encoded == null) {
            return null;
        }
        byte[] bytes = Base64.getDecoder().decode(base64Encoded);
        return new String(
                Zstd.decompress(bytes, (int) Zstd.decompressedSize(bytes)),
                StandardCharsets.UTF_8);
    }

    private Rfc stringToRfc(String currentLine) {
        Map<String, Object> map = gson.fromJson(new StringReader(currentLine), mapType);

        String id = (String)map.get("doc_id");
        String title = ((String)map.get("title")).trim();
        String abstractText = (String)map.get("abstract");

        String html = jsonDecompress((String) map.get("$html"));

        String wordsJson = jsonDecompress((String) map.get("words"));
        List<String> wordsList = gson.fromJson(new StringReader(wordsJson), listType);
        String words = String.join(" ", wordsList);

        return new Rfc(id, title, abstractText, html, words);
    }

    @Override
    public void close() throws Exception {
        isClosed = true;
        br.close();
    }
}
