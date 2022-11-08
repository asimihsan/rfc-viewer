package art.kittencat.lambda;

import art.kittencat.lucene.GetDocResult;
import art.kittencat.lucene.SearchResult;
import art.kittencat.lucene.Searcher;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class Handler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private static final Logger logger = LogManager.getLogger(Handler.class);
    private static final Gson gson = new Gson();
    private static final Type mapType = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final Type resultType = new TypeToken<List<SearchResult>>() {
    }.getType();
    private static final Searcher searcher;

    static {
        try {
            searcher = new Searcher(Path.of("lucene-index"));
            for (String word : Arrays.asList("congestion", "loss", "mesh loss")) {
                searcher.search(word, 20, 3);
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Map<String, String> headers = new ImmutableMap.Builder<String, String>()
            .put("Access-Control-Allow-Origin", "*")
            .put("Access-Control-Allow-Credentials", "true")
            .put("Access-Control-Allow-Headers", "Content-Type")
            .put("Access-Control-Allow-Methods", "OPTIONS,POST")
            .put("Content-Type", "application/json")
            .build();

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        logger.info("entry");
        if (input.getRequestContext().getHttp().getMethod().equals("OPTIONS")) {
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(200)
                    .withBody("{}")
                    .withIsBase64Encoded(false)
                    .withHeaders(headers)
                    .build();
        }

        String body;
        if (input.getIsBase64Encoded()) {
            body = new String(Base64.getDecoder().decode(input.getBody()), StandardCharsets.UTF_8);
        } else {
            body = input.getBody();
        }
        logger.info(body);
        Map<String, String> map = gson.fromJson(new StringReader(body), mapType);

        try {
            if (map.containsKey("query")) {
                return handleQuery(map.get("query"));
            } else if (map.containsKey("getDocById")) {
                return handleGetDocById(map.get("getDocById"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(400)
                .withHeaders(headers)
                .build();
    }

    private APIGatewayV2HTTPResponse handleGetDocById(String id) throws IOException {
        String doc = searcher.getDocById(id);
        GetDocResult result = new GetDocResult(doc);
        String resultBody = Base64.getEncoder().encodeToString(gson.toJson(result).getBytes(StandardCharsets.UTF_8));
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(200)
                .withBody(resultBody)
                .withIsBase64Encoded(true)
                .withHeaders(headers)
                .build();

    }

    private APIGatewayV2HTTPResponse handleQuery(String query) {
        int hitsPerPage = 20;
        int maxNumHighlights = 3;
        List<SearchResult> results;
        try {
            results = searcher.search(query, hitsPerPage, maxNumHighlights);
        } catch (ParseException | IOException e) {
            throw new RuntimeException(e);
        }
        String resultBody = Base64.getEncoder().encodeToString(gson.toJson(results).getBytes(StandardCharsets.UTF_8));
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(200)
                .withBody(resultBody)
                .withIsBase64Encoded(true)
                .withHeaders(headers)
                .build();
    }
}
