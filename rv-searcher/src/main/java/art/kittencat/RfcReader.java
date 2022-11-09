package art.kittencat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class RfcReader implements AutoCloseable {
    private static final Gson gson = new Gson();
    private static final Type mapType = new TypeToken<Map<String, Object>>() {
    }.getType();
    private static final Type listType = new TypeToken<List<String>>() {
    }.getType();

    private final BufferedReader br;
    private boolean isClosed = false;

    public RfcReader(Path preprocessedRfcs) throws IOException {
        this.br = Files.newBufferedReader(preprocessedRfcs);
    }

    public Rfc getNextRfc() throws IOException {
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

    private String jsonDecompress(String base64Encoded) throws IOException {
        if (base64Encoded == null) {
            return null;
        }
        byte[] bytes = Base64.getDecoder().decode(base64Encoded);
        return zlibDecompress(bytes);
    }

    private String zlibDecompress(byte[] compressed) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        InflaterInputStream iis = new InflaterInputStream(bais);

        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[4 * 1024];
        int rlen;
        while ((rlen = iis.read(buf)) != -1) {
            sb.append(new String(Arrays.copyOf(buf, rlen), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private byte[] zlibCompress(String uncompressed) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos);
        dos.write(uncompressed.getBytes());
        dos.flush();
        dos.close();
        return baos.toByteArray();
    }

    @Nonnull
    private byte[] maybeGetBase64EncodedBytes(String key, Map<String, Object> map) {
        if (!map.containsKey(key)) {
            return new byte[]{};
        }
        return Base64.getDecoder().decode((String) map.get(key));
    }

    private Rfc stringToRfc(String currentLine) throws IOException {
        Map<String, Object> map = gson.fromJson(new StringReader(currentLine), mapType);

        String id = (String) map.get("doc_id");
        String title = ((String) map.get("title")).trim();
        String abstractText = (String) map.get("abstract");

        byte[] htmlCompressed = maybeGetBase64EncodedBytes("$html", map);
        byte[] txtCompressed = maybeGetBase64EncodedBytes("$txt", map);

        String wordsJson = jsonDecompress((String) map.get("words"));
        List<String> wordsList = gson.fromJson(new StringReader(wordsJson), listType);
        String words = String.join(" ", wordsList);

        byte[] wordsCompressed = zlibCompress(words);

        return new Rfc(id, title, abstractText, htmlCompressed, txtCompressed, words, wordsCompressed);
    }

    @Override
    public void close() throws Exception {
        isClosed = true;
        br.close();
    }
}
