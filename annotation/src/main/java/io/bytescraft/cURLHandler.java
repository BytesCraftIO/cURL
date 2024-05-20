package io.bytescraft;

import com.google.auto.service.AutoService;
import com.javaquery.util.collection.Collections;
import io.bytescraft.common.CURLProcessor;
import io.bytescraft.postman.PostmanSchema;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;
/**
 * cURL annotation handler
 * @author javaquery
 * @since 0.0.1
 */
@SupportedAnnotationTypes(
        {"io.bytescraft.cURL"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class cURLHandler extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(Collections.nonNullNonEmpty(annotations)){
            CURLProcessor CURLProcessor = new PostmanSchema(annotations, roundEnv, processingEnv);
            CURLProcessor.process();
        }
        return true;
    }
}
