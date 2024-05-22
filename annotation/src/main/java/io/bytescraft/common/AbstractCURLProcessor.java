package io.bytescraft.common;

import com.javaquery.util.Objects;
import com.javaquery.util.collection.Collections;
import com.javaquery.util.io.JFile;
import com.javaquery.util.string.Strings;
import io.bytescraft.cURL;
import io.bytescraft.model.QueryParam;
import io.bytescraft.spring.annotations.HttpRequestMapping;
import org.json.JSONObject;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import java.io.File;
import java.util.*;

import static io.bytescraft.common.Configuration.CURRENT_WORKING_DIR;

/**
 * Abstract annotation processor implementation
 * @author javaquery
 * @since 0.0.1
 */
public abstract class AbstractCURLProcessor implements CURLProcessor {

    protected static final String SPRING_REQUEST_PARAM_DEFAULT_VALUE = "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";
    protected static final String LOCALHOST = "localhost:8080";
    protected final Configuration cfg;
    protected final Set<? extends TypeElement> annotations;
    protected final RoundEnvironment roundEnv;
    protected final ProcessingEnvironment processingEnv;

    public AbstractCURLProcessor(Configuration cfg, Set<? extends TypeElement> annotations, RoundEnvironment roundEnv, ProcessingEnvironment processingEnvironment) {
        this.cfg = cfg;
        this.annotations = annotations;
        this.roundEnv = roundEnv;
        this.processingEnv = processingEnvironment;
    }

    protected List<? extends VariableElement> getVariableElements(Element element){
        ExecutableElement executableElement = (ExecutableElement) element;
        return executableElement.getParameters();
    }

    /**
     * Identify class level request path annotated by @RequestMapping.
     * @param element - method element
     * @return - class level request path
     */
    protected String getClassLevelRequestPath(Element element){
        List<Object> classLevelRequestMapping = getClassLevelRequestMapping(element);
        if(com.javaquery.util.collection.Collections.nonNullNonEmpty(classLevelRequestMapping)){
            String classLevelPath = Collections.nonNullNonEmpty(classLevelRequestMapping) ? classLevelRequestMapping.get(0).toString() : "";
            return classLevelPath.replace("\"", "");
        }
        return Strings.EMPTY_STRING;
    }

    /**
     * Get class level request mapping.
     * @param element - method element
     * @return - class level request mapping
     */
    private List<Object> getClassLevelRequestMapping(Element element){
        List<Object> classLevelRequestMapping = null;
        AnnotationMirror requestMappingAnnotation = getAnnotationMirror(element.getEnclosingElement(), "org.springframework.web.bind.annotation.RequestMapping");
        if(Objects.nonNull(requestMappingAnnotation)){
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = processingEnv.getElementUtils().getElementValuesWithDefaults(requestMappingAnnotation);
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                String key = entry.getKey().getSimpleName().toString();
                if(StringPool.VALUE.equalsIgnoreCase(key)){
                    classLevelRequestMapping = (List<Object>) entry.getValue().getValue();
                    break;
                }
            }
        }
        return classLevelRequestMapping;
    }

    /**
     * Build HttpRequestMapping object.
     * @param element - method element
     * @return - HttpRequestMapping object
     */
    protected HttpRequestMapping getHttpRequestMapping(Element element){
        AnnotationMirror annotationMirror = getAnnotationMirror(element, "org.springframework.web.bind.annotation");
        if(Objects.nonNull(annotationMirror)){
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = processingEnv.getElementUtils().getElementValuesWithDefaults(annotationMirror);
            return new HttpRequestMapping(annotationMirror, values);
        }
        return null;
    }

    /**
     * Get query parameters.
     * @param parameters - method parameters with annotation "org.springframework.web.bind.annotation.RequestParam"
     * @return - list of query parameters
     */
    protected List<QueryParam> getQueryParameters(List<? extends VariableElement> parameters){
        List<QueryParam> result = new ArrayList<>();
        for (VariableElement parameter : parameters) {
            AnnotationMirror requestMappingAnnotation = getAnnotationMirror(parameter, "org.springframework.web.bind.annotation.RequestParam");
            if(Objects.nonNull(requestMappingAnnotation)){
                Map<? extends ExecutableElement, ? extends AnnotationValue> values = processingEnv.getElementUtils().getElementValuesWithDefaults(requestMappingAnnotation);
                QueryParam queryParam = new QueryParam();
                String name = null, value = null;
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                    String key = entry.getKey().getSimpleName().toString();

                    switch (key) {
                        case StringPool.VALUE:
                            value = entry.getValue().getValue().toString();
                            break;
                        case StringPool.NAME:
                            name = entry.getValue().getValue().toString();
                            break;
                        case StringPool.DEFAULT_VALUE:
                            String val = entry.getValue().getValue().toString();
                            if(!SPRING_REQUEST_PARAM_DEFAULT_VALUE.equals(val)){
                                queryParam.setDefaultValue(entry.getValue().getValue().toString());
                            }
                            break;
                        case StringPool.REQUIRED:
                            queryParam.setRequired(Boolean.parseBoolean(entry.getValue().getValue().toString()));
                            break;
                    }
                }
                if(Strings.nullOrEmpty(name) && Strings.nonNullNonEmpty(value)){
                    queryParam.setName(value);
                }else if(Strings.nullOrEmpty(value) && Strings.nonNullNonEmpty(name)){
                    queryParam.setName(name);
                }else if(Strings.nullOrEmpty(name)
                    && Strings.nullOrEmpty(value)){
                    /* use variable name when name and value not provided in annotation */
                    queryParam.setName(parameter.getSimpleName().toString());
                }
                result.add(queryParam);
            }
        }
        return result;
    }

    /**
     * Prepare request body.
     * @param executableElement - method element
     * @param parameters - method parameters
     * @return - request body
     */
    protected JSONObject getRequestBody(Element executableElement, List<? extends VariableElement> parameters) {
        Map<String, DeclaredType> declaredTypeMap = new HashMap<>();
        ExecutableType executableType = (ExecutableType) executableElement.asType();
        List<? extends TypeMirror> param = executableType.getParameterTypes();
        for (TypeMirror parameter : param) {
            try{
                DeclaredType declaredType = (DeclaredType) parameter;
                declaredTypeMap.put(parameter.toString(), declaredType);
            } catch (Exception ignored) {}
        }

        JSONObject requestBody = null;
        for (VariableElement parameter : parameters) {
            AnnotationMirror requestMappingAnnotation = getAnnotationMirror(parameter, "org.springframework.web.bind.annotation.RequestBody");
            if(Objects.nonNull(requestMappingAnnotation)){
                requestBody = new JSONObject();
                DeclaredType declaredType = declaredTypeMap.get(parameter.asType().toString());
                for(Element element : declaredType.asElement().getEnclosedElements()){
                    if(element.getKind().equals(ElementKind.FIELD)){
                        requestBody.put(element.getSimpleName().toString(), element.asType().toString());
                    }
                }
                break;
            }
        }
        return requestBody;
    }

    /**
     * Get annotation mirror of specified annotation path
     * @param element - element
     * @param name - annotation path
     * @return - annotation mirror
     */
    protected AnnotationMirror getAnnotationMirror(Element element, String name){
        return element.getAnnotationMirrors()
                .stream()
                .filter(am -> am.getAnnotationType().toString().contains(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get folder path.
     * @param classLevelRequestPath - class level request path
     * @param cURLObject - cURL annotation
     * @return - folder path
     */
    protected String getFolderPath(String classLevelRequestPath, cURL cURLObject) {
        String result = cURLObject.folder();
        if(Strings.nonNullNonEmpty(classLevelRequestPath)){
            if(!classLevelRequestPath.endsWith("/")
                    && !cURLObject.folder().startsWith("/")){
                classLevelRequestPath = classLevelRequestPath + "/";
            }
            result = classLevelRequestPath + cURLObject.folder();
            if(result.endsWith("/")){
                result = result.substring(0, result.length() - 1);
            }
        }
        return result;
    }

    /**
     * Write output to file
     * @param fileName - file name
     * @param content - content to write
     */
    public void writeToOutputFile(String fileName, String content) {
        if(Strings.nonNullNonEmpty(cfg.getCollectionName())){
            fileName = cfg.getCollectionName() + "_" + fileName;
            fileName = fileName.replace(" ", "_");
        }
        JFile outputFile = new JFile(CURRENT_WORKING_DIR + File.separatorChar + fileName);
        if(outputFile.exists()){
            boolean ignored = outputFile.delete();
        }
        outputFile.write(content);
    }
}
