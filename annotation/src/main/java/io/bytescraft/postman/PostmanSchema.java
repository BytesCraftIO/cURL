package io.bytescraft.postman;

import com.javaquery.util.Objects;
import com.javaquery.util.collection.Collections;
import com.javaquery.util.string.Strings;
import io.bytescraft.AbstractCURLCURLProcessor;
import io.bytescraft.Commons;
import io.bytescraft.StringPool;
import io.bytescraft.cURL;
import io.bytescraft.model.QueryParam;
import io.bytescraft.spring.annotations.HttpRequestMapping;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import java.util.*;

/**
 * @author javaquery
 * @since 0.0.1
 */
public class PostmanSchema extends AbstractCURLCURLProcessor {

    public PostmanSchema(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv, ProcessingEnvironment processingEnvironment) {
        super(annotations, roundEnv, processingEnvironment);
        process();
    }

    public void process(){
        JSONObject ROOT = new JSONObject();
        for(TypeElement annotation : annotations){
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for(Element element : elements){
                ExecutableElement executableElement = (ExecutableElement) element;
                List<? extends VariableElement> parameters = executableElement.getParameters();
                List<QueryParam> queryParams = getQueryParameters(parameters);
                JSONObject requestBody = getRequestBody(element, parameters);

                JSONArray items = ROOT.optJSONArray(StringPool.ITEM, new JSONArray());
                ROOT.put(StringPool.ITEM, items);

                cURL cURLObject = element.getAnnotation(cURL.class);
                String classLevelRequestMapping = getClassLevelRequestPath(element);
                String classMethodName = element.getSimpleName().toString();
                HttpRequestMapping httpRequestMapping = getHttpRequestMapping(element);
                JSONObject item = buildHttpRequestItem(classLevelRequestMapping, classMethodName, cURLObject, httpRequestMapping, queryParams, requestBody);

                String folderPath = getFolderPath(classLevelRequestMapping, cURLObject);
                if(Strings.nullOrEmpty(folderPath)){
                    items.put(item);
                }else{
                    recursiveAddToCollection(folderPath, items, item);
                }
            }
        }
        if(ROOT.has(StringPool.ITEM)){
            ROOT.put(StringPool.INFO, info());
            ROOT.put(StringPool.VARIABLE, variables());
            writeToOutputFile(ROOT.toString(4));
        }
    }

    /**
     * Identify class level request path annotated by @RequestMapping.
     * @param element - method element
     * @return - class level request path
     */
    private String getClassLevelRequestPath(Element element){
        List<Object> classLevelRequestMapping = getClassLevelRequestMapping(element);
        if(Collections.nonNullNonEmpty(classLevelRequestMapping)){
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
    private HttpRequestMapping getHttpRequestMapping(Element element){
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
    private List<QueryParam> getQueryParameters(List<? extends VariableElement> parameters){
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
                }
                result.add(queryParam);
            }
        }
        return result;
    }

    /**
     * Get annotation mirror of specified annotation path
     * @param element - element
     * @param name - annotation path
     * @return - annotation mirror
     */
    private AnnotationMirror getAnnotationMirror(Element element, String name){
        return element.getAnnotationMirrors()
                .stream()
                .filter(am -> am.getAnnotationType().toString().contains(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Prepare request body.
     * @param executableElement - method element
     * @param parameters - method parameters
     * @return - request body
     */
    private JSONObject getRequestBody(Element executableElement, List<? extends VariableElement> parameters) {
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
     * Info object for postman collection.
     * @return - info object
     */
    private JSONObject info(){
        JSONObject info = new JSONObject();
        info.put(StringPool.NAME, "Postman Collection");
        info.put(StringPool.SCHEMA, "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
        info.put(StringPool.DESCRIPTION, "Postman collection auto generated by cURL(bytescraft.io) annotation processor.");
        return info;
    }

    /**
     * Default variables.
     * @return - default variables
     */
    private JSONArray variables() {
        JSONObject host = new JSONObject();
        host.put(StringPool.KEY, "HOST");
        host.put(StringPool.VALUE, "localhost:8080");
        host.put(StringPool.TYPE, "string");

        JSONArray variables = new JSONArray();
        variables.put(host);
        return variables;
    }

    /**
     * Get folder path.
     * @param classLevelRequestPath - class level request path
     * @param cURLObject - cURL annotation
     * @return - folder path
     */
    private String getFolderPath(String classLevelRequestPath, cURL cURLObject) {
        String result = cURLObject.folder();
        if(Strings.nonNullNonEmpty(classLevelRequestPath)){
            if(!classLevelRequestPath.endsWith("/")
                && !cURLObject.folder().startsWith("/")){
                classLevelRequestPath = classLevelRequestPath + "/";
            }
            classLevelRequestPath = Commons.convertCamelCaseToName(classLevelRequestPath.replace("/", ""));
            result = classLevelRequestPath + cURLObject.folder();
        }
        return result;
    }

    /**
     * When user want folder structure in postman collection. (i.e. folder = "/folder1/folder2")
     * @param folderName - folder name
     * @param items - list of items
     * @param item - item to add in folder
     */
    private void recursiveAddToCollection(String folderName, JSONArray items, JSONObject item){
        folderName = folderName.startsWith("/") ? folderName.substring(1) : folderName;
        String[] folders = folderName.split("/");

        JSONObject folder = findFolder(items, folders[0]);
        if(Objects.isNull(folder)){
            folder = new JSONObject();
            folder.put(StringPool.NAME, folders[0]);
            folder.put(StringPool.ITEM, new JSONArray());
            items.put(folder);
        }

        JSONArray folderItems = folder.optJSONArray(StringPool.ITEM);
        if(folders.length == 1){
            folderItems.put(item);
        }else{
            StringBuilder sbCollectionPath = new StringBuilder();
            for(int i = 1; i < folders.length; i++){
                sbCollectionPath.append("/").append(folders[i]);
            }
            recursiveAddToCollection(sbCollectionPath.toString(), folderItems, item);
        }
    }

    /**
     * Find folder in items.
     * @param items - list of items and folders
     * @param folderName - folder name to look for
     * @return - folder object if found else null
     */
    private JSONObject findFolder(JSONArray items, String folderName){
        int itemsLength = items.length();
        for (int i = 0; i < itemsLength; i++) {
            JSONObject jsonObject = items.optJSONObject(i);
            if(Objects.nonNull(jsonObject)
                    && jsonObject.optString(StringPool.NAME).equals(folderName)){
                return jsonObject;
            }
        }
        return null;
    }

    /**
     * Build request item.
     *
     * @param classLevelRequestMapping - class level request mapping
     * @param classMethodName          - method name
     * @param cURL                     - cURL annotation
     * @param httpRequestMapping       - HttpRequestMapping annotation
     * @param queryParams              - query parameters
     * @param convertedClassToJSON - converted class to JSON
     * @return - request item object
     */
    private JSONObject buildHttpRequestItem(String classLevelRequestMapping, String classMethodName, cURL cURL, HttpRequestMapping httpRequestMapping, List<QueryParam> queryParams, JSONObject convertedClassToJSON){
        if(Objects.nonNull(httpRequestMapping)){
            String name = Strings.nonNullNonEmpty(cURL.name()) ? cURL.name() : classMethodName;
            JSONObject requestItem = new JSONObject();
            requestItem.put(StringPool.NAME, name);
            requestItem.put(StringPool.REQUEST, request(classLevelRequestMapping, httpRequestMapping, queryParams, convertedClassToJSON));
            return requestItem;
        }
        return null;
    }

    /**
     * Build request object.
     *
     * @param classLevelRequestMapping - class level request mapping
     * @param httpRequestMapping       - HttpRequestMapping annotation
     * @param queryParams              - query parameters
     * @param convertedClassToJSON - converted class to JSON
     * @return - request object
     */
    private JSONObject request(String classLevelRequestMapping, HttpRequestMapping httpRequestMapping, List<QueryParam> queryParams, JSONObject convertedClassToJSON){
        JSONObject request = new JSONObject();
        request.put(StringPool.METHOD, httpRequestMapping.getMethod());
        request.put(StringPool.HEADER, headers(httpRequestMapping.extractHeaders()));
        request.put(StringPool.URL, url(classLevelRequestMapping, httpRequestMapping.getRequestPath(), queryParams));
        if(Objects.nonNull(convertedClassToJSON)){
            request.put(StringPool.BODY, prepareRequestBody(convertedClassToJSON));
        }
        return request;
    }

    private JSONArray headers(List<String> strings) {
        JSONArray headers = new JSONArray();
        for(String header : strings){
            JSONObject headerObject = new JSONObject();
            String[] headerArray = header.split("=");
            headerObject.put(StringPool.KEY, headerArray[0]);
            headerObject.put(StringPool.VALUE, headerArray[1]);
            headerObject.put(StringPool.TYPE, "text");
            headers.put(headerObject);
        }
        return headers;
    }

    /**
     * Build URL object.
     *
     * @param classLevelRequestMapping  - class level request mapping
     * @param methodLevelRequestMapping - method level request mapping
     * @param queryParams - query parameters
     * @return - URL object
     */
    private JSONObject url(String classLevelRequestMapping, String methodLevelRequestMapping, List<QueryParam> queryParams){
        JSONObject url = new JSONObject();
        String path = classLevelRequestMapping + methodLevelRequestMapping;
        url.put(StringPool.HOST, "{{HOST}}");
        url.put(StringPool.RAW, "{{HOST}}" + path);

        JSONArray pathArray = new JSONArray();
        pathArray.put(path);
        url.put(StringPool.PATH, pathArray);

        JSONArray query = new JSONArray();
        StringJoiner queryJoiner = new StringJoiner("&");
        for(QueryParam queryParam : queryParams){
            JSONObject queryParamObject = new JSONObject();
            queryParamObject.put(StringPool.VALUE, queryParam.getDefaultValue());
            queryParamObject.put(StringPool.KEY, queryParam.getName());
            queryParamObject.put(StringPool.DISABLED, !queryParam.isRequired());
            query.put(queryParamObject);

            if(queryParam.isRequired()){
                queryJoiner.add(queryParam.getName() + "=" + queryParam.getDefaultValue());
            }
        }
        url.put(StringPool.QUERY, query);
        if(Strings.nonNullNonEmpty(queryJoiner.toString())){
            url.put(StringPool.RAW, url.getString(StringPool.RAW) + "?" + queryJoiner);
        }
        return url;
    }

    /**
     * Prepare request body.
     * @param convertedClassToJSON - converted class to JSON
     * @return - request body
     */
    private JSONObject prepareRequestBody(JSONObject convertedClassToJSON){
        JSONObject body = new JSONObject();
        body.put(StringPool.MODE, StringPool.RAW);
        body.put(StringPool.RAW, convertedClassToJSON.toString());

        JSONObject raw = new JSONObject();
        raw.put(StringPool.LANGUAGE, StringPool.JSON);

        JSONObject options = new JSONObject();
        options.put(StringPool.RAW, raw);

        body.put(StringPool.OPTIONS, options);
        return body;
    }
}
