package art.kittencat;

import com.google.common.io.Closer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.MultiSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;

// https://www.lucenetutorial.com/lucene-in-5-minutes.html

public class CreateIndex {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello world!");

        // 1. Index
        try (StandardAnalyzer analyzer = new StandardAnalyzer();
//             CustomAnalyzer analyzer = new CustomAnalyzer();
             Directory index = FSDirectory.open(Path.of("lucene-index"));
             Closer closer = Closer.create();
             RfcReader rfcReader = new RfcReader(Path.of(args[0]))) {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            config.setSimilarity(new MultiSimilarity(new Similarity[]{
//                    new BM25Similarity(),
//                    new AxiomaticF2EXP(),
                    new LMDirichletSimilarity(),
                    new BM25Similarity(),
//                    new DFISimilarity(new IndependenceChiSquared()),
            }));
            IndexWriter w = closer.register(new IndexWriter(index, config));

            System.out.println("adding...");
            Rfc rfc;
            while ((rfc = rfcReader.getNextRfc()) != null) {
                addDoc(w, rfc);
            }
            System.out.println("added");
            w.forceMerge(1);
            System.out.println("merged");
        }

//        System.out.println("creating auto suggester lookup...");
//        try (StandardAnalyzer analyzer = new StandardAnalyzer();
//             Directory index = FSDirectory.open(Path.of("lucene-index"));
//             IndexReader reader = DirectoryReader.open(index);
//             Directory suggesterDir = FSDirectory.open(Path.of("lucene-suggester"))) {
//            AnalyzingSuggester suggester = new AnalyzingSuggester(suggesterDir, "temp", analyzer);
//            for (String field : new String[]{"title", "abstractText"}) {
//                DocumentDictionary documentDictionary = new DocumentDictionary(
//                        reader, field, null /*weightField*/);
//                suggester.build(documentDictionary);
//            }
//        }
//        System.out.println("done creating auto suggester lookup.");
    }

    private static void addDoc(IndexWriter w,
                                   Rfc rfc) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("id", rfc.id(), Field.Store.YES));
        doc.add(new StringField("title", rfc.title(), Field.Store.YES));
        if (rfc.abstractText() != null) {
            doc.add(new StringField("abstractText", rfc.abstractText(), Field.Store.YES));
        } else {
            doc.add(new StringField("abstractText", "", Field.Store.YES));
        }
        doc.add(new TextField("doc", rfc.words(), Field.Store.YES));
        doc.add(new StoredField("htmlCompressed", rfc.htmlCompressed()));
        doc.add(new StoredField("textCompressed", rfc.textCompressed()));
        w.addDocument(doc);
    }
}