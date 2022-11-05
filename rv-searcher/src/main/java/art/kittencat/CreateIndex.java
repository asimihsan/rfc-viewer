package art.kittencat;

import com.google.common.io.Closer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.*;
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
                    new AxiomaticF2EXP(),
                    new DFISimilarity(new IndependenceStandardized()),
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
    }

    private static void addDoc(IndexWriter w,
                               Rfc rfc) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("id", rfc.id(), Field.Store.YES));
        doc.add(new StringField("title", rfc.title(), Field.Store.YES));
        doc.add(new TextField("doc", rfc.words(), Field.Store.NO));
        doc.add(new StoredField("htmlCompressed", rfc.htmlCompressed()));
        doc.add(new StoredField("textCompressed", rfc.textCompressed()));
        w.addDocument(doc);
    }
}