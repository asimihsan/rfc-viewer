package art.kittencat.lucene;

import com.google.common.io.Closer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.InflaterInputStream;

public class Searcher {
    private static final Logger logger = LogManager.getLogger(Searcher.class);

    private final Analyzer analyzer;
    private final IndexSearcher searcher;
    private final Closer closer;

    public Searcher(Path indexPath) throws IOException {
        analyzer = new StandardAnalyzer();
        Directory index = FSDirectory.open(indexPath);
        IndexReader reader = DirectoryReader.open(index);
        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new MultiSimilarity(new Similarity[]{
                new LMDirichletSimilarity(),
                new BM25Similarity()
        }));
        this.closer = Closer.create();
        this.closer.register(reader);
        this.closer.register(index);
    }

    public List<SearchResult> search(String query, int hitsPerPage, int maxNumHighlightFragments) throws ParseException, IOException {
        QueryParser qp = new QueryParser("doc", analyzer);
        Query q = qp.parse(query);
        QueryScorer queryScorer = new QueryScorer(q);
        Highlighter highlighter = new Highlighter(
                new SimpleHTMLFormatter("<mark>", "</mark>"), queryScorer);
        highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);

        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        List<SearchResult> results = new ArrayList<>(hits.length);
        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document d = searcher.doc(docId);
            try {
                String words = getWords(d);
                TokenStream wordsTokenStream = analyzer.tokenStream("doc", words);
                String[] highlights = highlighter.getBestFragments(wordsTokenStream, words, maxNumHighlightFragments);
                results.add(new SearchResult(d.get("id"), d.get("title"), Arrays.asList(highlights)));
            } catch (InvalidTokenOffsetsException e) {
                throw new RuntimeException(e);
            }
        }
        return results;
    }

    public String getDocById(String docId) throws IOException {
        TopDocs docs = searcher.search(new TermQuery(new Term("id", docId)), 1);
        ScoreDoc[] hits = docs.scoreDocs;
        if (hits.length > 0) {
            Document d = searcher.doc(hits[0].doc);
            return getDoc(d);
        } else {
            return null;
        }
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

    public void close() throws IOException {
        closer.close();
    }

}
