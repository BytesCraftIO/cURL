package io.bytescraft;

import com.javaquery.util.io.JFile;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * @author javaquery
 * @since 0.0.1
 */
public abstract class AbstractCURLCURLProcessor implements CURLProcessor {

    private static final String CURRENT_WORKING_DIR = System.getProperty("user.dir");
    protected static final String SPRING_REQUEST_PARAM_DEFAULT_VALUE = "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";
    protected final Set<? extends TypeElement> annotations;
    protected final RoundEnvironment roundEnv;
    protected final ProcessingEnvironment processingEnv;

    public AbstractCURLCURLProcessor(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv, ProcessingEnvironment processingEnvironment) {
        this.annotations = annotations;
        this.roundEnv = roundEnv;
        this.processingEnv = processingEnvironment;
    }

    public void writeToOutputFile(String content) {
        JFile outputFile = new JFile(CURRENT_WORKING_DIR + "/postman_collection.json");
        if(outputFile.exists()){
            boolean ignored = outputFile.delete();
        }
        outputFile.write(content);
    }
}
