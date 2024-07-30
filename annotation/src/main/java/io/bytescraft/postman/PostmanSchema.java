package io.bytescraft.postman;

import com.javaquery.util.Objects;
import com.javaquery.util.string.Strings;
import io.bytescraft.cURL;
import io.bytescraft.common.*;
import io.bytescraft.model.QueryParam;
import io.bytescraft.spring.annotations.HttpRequestMapping;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Generate postman collection from annotations
 * @author javaquery
 * @since 0.0.1
 */
public class PostmanSchema extends AbstractCURLProcessor {

    private static final String OUTPUT_FILE = "postman_collection.json";
    private static final String SCHEMA = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json";
    private static final String DEFAULT_COL_NAME = "Postman Collection";

    public PostmanSchema(Configuration cfg, Set<? extends TypeElement> annotations, RoundEnvironment roundEnv, ProcessingEnvironment processingEnvironment) {
        super(cfg, annotations, roundEnv, processingEnvironment);
        process();
    }

    public void process(){
        JSONObject ROOT = new JSONObject();
        for(TypeElement annotation : annotations){
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for(Element element : elements){
                List<? extends VariableElement> parameters = getVariableElements(element);
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
            ROOT.put(StringPool.EVENTS, events());
            writeToOutputFile(OUTPUT_FILE, ROOT.toString(4));
        }
    }

    /**
     * Info object for postman collection.
     * @return - info object
     */
    private JSONObject info(){
        JSONObject info = new JSONObject();
        info.put(StringPool.NAME, Strings.nonNullNonEmpty(cfg.getCollectionName()) ? cfg.getCollectionName() : DEFAULT_COL_NAME);
        info.put(StringPool.SCHEMA, SCHEMA);
        info.put(StringPool.DESCRIPTION, cfg.getCollectionDescription());
        return info;
    }

    /**
     * Default variables.
     * @return - default variables
     */
    private JSONArray variables() {
        JSONArray variables = new JSONArray();
        Map<String, Object> vars = cfg.getVariables();
        vars.putIfAbsent(StringPool.HOST_CAPS, LOCALHOST);

        for(Map.Entry<String, Object> entry : vars.entrySet()){
            JSONObject variable = new JSONObject();
            variable.put(StringPool.KEY, entry.getKey());
            variable.put(StringPool.VALUE, entry.getValue());

            if(entry.getValue() instanceof String){
                variable.put(StringPool.TYPE, StringPool.STRING);
            }else if(entry.getValue() instanceof Number){
                variable.put(StringPool.TYPE, StringPool.NUMBER);
            }
            variables.put(variable);
        }
        return variables;
    }

    private JSONArray events(){
        String collectionPreRequestScript = cfg.optString(ConfigKeys.POSTMAN_COL_PRE_REQUEST);
        String collectionPostResponseScript = cfg.optString(ConfigKeys.POSTMAN_COL_POST_RESPONSE);

        JSONArray events = new JSONArray();
        if(Strings.nonNullNonEmpty(collectionPreRequestScript)){
            JSONObject script = new JSONObject();
            script.put(StringPool.TYPE, StringPool.TEST_JAVASCRIPT);
            script.put(StringPool.PACKAGE, new JSONObject());
            script.put(StringPool.EXEC, new JSONArray(collectionPreRequestScript.split("\n")));

            JSONObject preRequestScript = new JSONObject();
            preRequestScript.put(StringPool.LISTEN, StringPool.PREREQUEST);
            preRequestScript.put(StringPool.SCRIPT, script);
            events.put(preRequestScript);
        }

        if(Strings.nonNullNonEmpty(collectionPostResponseScript)){
            JSONObject script = new JSONObject();
            script.put(StringPool.TYPE, StringPool.TEST_JAVASCRIPT);
            script.put(StringPool.PACKAGE, new JSONObject());
            script.put(StringPool.EXEC, new JSONArray(collectionPostResponseScript.split("\n")));

            JSONObject postResponseScript = new JSONObject();
            postResponseScript.put(StringPool.LISTEN, StringPool.TEST);
            postResponseScript.put(StringPool.SCRIPT, script);
            events.put(postResponseScript);
        }
        return events;
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
            folder.put(StringPool.NAME, Commons.convertCamelCaseToName(folders[0]));
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
        folderName = Commons.convertCamelCaseToName(folderName);
        int itemsLength = items.length();
        for (int i = 0; i < itemsLength; i++) {
            JSONObject jsonObject = items.optJSONObject(i);
            if(Objects.nonNull(jsonObject)
                    && jsonObject.optString(StringPool.NAME).equalsIgnoreCase(folderName)){
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
            headerObject.put(StringPool.TYPE, StringPool.TEXT);
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
