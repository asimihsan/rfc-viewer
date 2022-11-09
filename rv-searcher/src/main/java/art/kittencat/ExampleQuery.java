package art.kittencat;

import org.apache.lucene.analysis.TokenStream;
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
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.MultiSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

public class ExampleQuery {

    public static void main(String[] args) throws ParseException, IOException {
//        CustomAnalyzer analyzer = new CustomAnalyzer();
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory index = FSDirectory.open(Path.of("lucene-index"));
//        Directory suggesterDir = FSDirectory.open(Path.of("lucene-suggester"));
//        AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(suggesterDir, analyzer);

        // 2 . Query
        String querystr = args.length > 0 ? args[0] : "cookie";
        QueryParser qp = new QueryParser("doc", analyzer);
        Query q = qp.parse(querystr);
        QueryScorer queryScorer = new QueryScorer(q);
        Highlighter highlighter = new Highlighter(queryScorer);
        highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);

        // 3. Search
        int hitsPerPage = 20;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new MultiSimilarity(new Similarity[]{
//                new BM25Similarity(),
//                new AxiomaticF2EXP(),
                new LMDirichletSimilarity(), new BM25Similarity()
//                new DFISimilarity(new IndependenceChiSquared()),
        }));

        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        // 4. Display
//        int numSuggestions = 3;
//        List<Lookup.LookupResult> lookupResults = suggester.lookup(querystr, true /*onlyMorePopular*/, numSuggestions);
//        for (Lookup.LookupResult lookupResult : lookupResults) {
//            String display;
//            if (lookupResult.highlightKey instanceof String) {
//                display = (String)lookupResult.highlightKey;
//            } else {
//                display = lookupResult.key.toString();
//            }
//            System.out.printf("Auto suggest:\t%s\n", display);
//        }

        System.out.printf("Found %d hits.\n", hits.length);
        for (int i = 0; i < hits.length; i++) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            System.out.printf("%d. %s\t%s\n", (i + 1), d.get("id"), d.get("title"));
            try {
                String words = getWords(d);
                TokenStream wordsTokenStream = analyzer.tokenStream("doc", words);
                String[] highlights = highlighter.getBestFragments(wordsTokenStream, words, 3);
                for (String highlight : highlights) {
                    System.out.println(highlight);
                }
            } catch (InvalidTokenOffsetsException e) {
                throw new RuntimeException(e);
            }
//            System.out.printf("%s\n", searcher.explain(q, docId));
            System.out.println("---");
        }

//        System.out.println(getDoc(searcher.doc(hits[0].doc)));
    }

    private static String zlibDecompress(byte[] compressed) throws IOException {
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

    private static String getDoc(Document d) throws IOException {
        byte[] html = d.getBinaryValue("htmlCompressed").bytes;
        byte[] text = d.getBinaryValue("textCompressed").bytes;

        if (html.length > 0) {
            return zlibDecompress(html);
        } else if (text.length > 0) {
            return zlibDecompress(text);
        } else {
            return "";
        }
    }

    private static String getWords(Document d) throws IOException {
        byte[] wordsBytes = d.getBinaryValue("wordsCompressed").bytes;
        return zlibDecompress(wordsBytes);
    }
}
