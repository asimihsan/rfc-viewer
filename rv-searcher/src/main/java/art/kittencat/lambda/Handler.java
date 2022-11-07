package art.kittencat.lambda;

import art.kittencat.lucene.SearchResult;
import art.kittencat.lucene.Searcher;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.file.Path;
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
            searcher.search("congestion", 20, 3);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        logger.info("entry");

        Map<String, String> map = gson.fromJson(new StringReader(input.getBody()), mapType);
        String query = map.get("query");
        int hitsPerPage = 20;
        int maxNumHighlights = 3;
        List<SearchResult> results;
        try {
            results = searcher.search(query, hitsPerPage, maxNumHighlights);
        } catch (ParseException | IOException e) {
            throw new RuntimeException(e);
        }

        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(200)
                .withBody(gson.toJson(results))
                .withIsBase64Encoded(false)
                .build();
    }
}
