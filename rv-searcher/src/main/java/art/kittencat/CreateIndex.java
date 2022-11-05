package art.kittencat;

import com.google.common.io.Closer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Text;

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
        if (rfc.abstractText() != null) {
            doc.add(new StringField("abstract", rfc.abstractText(), Field.Store.YES));
        } else {
            doc.add(new StringField("abstract", "", Field.Store.YES));
        }
        doc.add(new TextField("doc", rfc.words(), Field.Store.NO));
        w.addDocument(doc);
    }
}