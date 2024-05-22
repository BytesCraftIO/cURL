package io.bytescraft;

import com.google.auto.service.AutoService;
import com.javaquery.util.Objects;
import com.javaquery.util.collection.Collections;
import io.bytescraft.common.CURLProcessor;
import io.bytescraft.common.Client;
import io.bytescraft.common.Configuration;
import io.bytescraft.postman.PostmanSchema;
import io.bytescraft.thunderclient.ThunderClientSchema;

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
            Configuration configuration = new Configuration();
            for (String client : configuration.getCollectionClients()){
                CURLProcessor curlProcessor = null;
                switch (Client.valueOf(client)){
                    case POSTMAN:
                        curlProcessor = new PostmanSchema(configuration, annotations, roundEnv, processingEnv);
                        break;
                    case THUNDER_CLIENT:
                        curlProcessor = new ThunderClientSchema(configuration, annotations, roundEnv, processingEnv);
                        break;
                    default:
                        break;
                }

                if (Objects.nonNull(curlProcessor)){
                    curlProcessor.process();
                }
            }
        }
        return true;
    }
}
