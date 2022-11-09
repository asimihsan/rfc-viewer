package art.kittencat;

import org.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SegmentReader;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

public class ReflectionRegistration implements Feature {
    public void beforeAnalysis(Feature.BeforeAnalysisAccess access) {
        try {
            RuntimeReflection.register(PackedTokenAttributeImpl.class.getDeclaredConstructor());
            RuntimeReflection.register(ConcurrentMergeScheduler.class);
            RuntimeReflection.register(IndexWriter.class);
            RuntimeReflection.register(SegmentReader.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
