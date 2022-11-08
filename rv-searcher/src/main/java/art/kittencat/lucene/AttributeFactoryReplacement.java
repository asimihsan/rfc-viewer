package art.kittencat.lucene;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeFactory.StaticImplementationAttributeFactory;
import org.apache.lucene.util.AttributeImpl;

import java.lang.reflect.UndeclaredThrowableException;

@TargetClass(AttributeFactory.class)
public final class AttributeFactoryReplacement {

    @Substitute
    @SuppressWarnings("unchecked")
    public static <A extends AttributeImpl> AttributeFactory getStaticImplementation(AttributeFactory delegate,
                                                                                     Class<A> clazz) {
        return new StaticImplementationAttributeFactory<A>(delegate, clazz) {
            @Override
            protected A createInstance() {
                try {
                    return (A) AttributeCreator.create(clazz);
                } catch (Error | RuntimeException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new UndeclaredThrowableException(e);
                }
            }
        };
    }
}
