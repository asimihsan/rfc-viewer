package art.kittencat;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;

public class ExampleQuery {
    public static void main(String[] args) throws ParseException, IOException {
//        CustomAnalyzer analyzer = new CustomAnalyzer();
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory index = FSDirectory.open(Path.of("lucene-index"));

        // 2 . Query
        String querystr = args.length > 0 ? args[0] : "cookie";
        Query q = new QueryParser("doc", analyzer).parse(querystr);

        // 3. Search
        int hitsPerPage = 20;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        // 4. Display
        System.out.printf("Found %d hits.\n", hits.length);
        for (int i = 0; i < hits.length; i++) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            System.out.printf("%d. %s\t%s\n", (i + 1), d.get("id"), d.get("title"));
        }
    }
}
