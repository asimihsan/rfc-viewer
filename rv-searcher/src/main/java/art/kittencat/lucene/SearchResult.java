package art.kittencat.lucene;

import java.util.List;

public class SearchResult {
    private final String id;
    private final String title;
    private final List<String> highlights;

    public SearchResult(String id, String title, List<String> highlights) {
        this.id = id;
        this.title = title;
        this.highlights = highlights;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getHighlights() {
        return highlights;
    }
}
