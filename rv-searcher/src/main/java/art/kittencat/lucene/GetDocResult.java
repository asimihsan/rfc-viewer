package art.kittencat.lucene;

public class GetDocResult {
    public String getBody() {
        return body;
    }

    private final String body;

    public GetDocResult(String body) {
        this.body = body;
    }
}
