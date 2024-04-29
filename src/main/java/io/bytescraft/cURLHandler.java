package io.bytescraft;

import com.google.auto.service.AutoService;
import com.javaquery.util.io.JFile;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.util.Map;
import java.util.Set;

/**
 * @author javaquery
 * @since 2024-04-26
 */
@SupportedAnnotationTypes(
        {"io.bytescraft.cURL", "org.springframework.web.bind.annotation.*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class cURLHandler extends AbstractProcessor {

    private static final String CURRENT_WORKING_DIR = System.getProperty("user.dir");

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        JFile jFile = new JFile( CURRENT_WORKING_DIR + "/curl.txt");
        for(TypeElement annotation : annotations){
            String annotationName = annotation.getQualifiedName().toString();
            jFile.append(annotationName, true);

            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for(Element element : elements){
                jFile.append("\t" + element.getSimpleName().toString(), true);
                jFile.append("\t" + element.getAnnotationMirrors().size(), true);
                for(AnnotationMirror annotationMirror : element.getAnnotationMirrors()){
                    jFile.append("\t" +annotationMirror.toString(), true);
                    Map<? extends ExecutableElement, ? extends AnnotationValue> values = processingEnv.getElementUtils().getElementValuesWithDefaults(annotationMirror);
                    for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()){
                        jFile.append("\t\t" + entry.getKey().toString() + " = " + entry.getValue().toString(), true);
                    }
                }
            }
        }
        return true;
    }
}
