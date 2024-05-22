package io.bytescraft.thunderclient;

import com.javaquery.util.Objects;
import com.javaquery.util.string.Strings;
import com.javaquery.util.time.DatePattern;
import com.javaquery.util.time.Dates;
import io.bytescraft.cURL;
import io.bytescraft.common.AbstractCURLProcessor;
import io.bytescraft.common.Commons;
import io.bytescraft.common.Configuration;
import io.bytescraft.common.StringPool;
import io.bytescraft.model.QueryParam;
import io.bytescraft.spring.annotations.HttpRequestMapping;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.*;

/**
 * In progress : Thunder Client schema generator.
 * @author javaquery
 * @since 2024-05-19
 */
public class ThunderClientSchema extends AbstractCURLProcessor {

    public ThunderClientSchema(Configuration cfg, Set<? extends TypeElement> annotations, RoundEnvironment roundEnv, ProcessingEnvironment processingEnvironment) {
        super(cfg, annotations, roundEnv, processingEnvironment);
    }

    @Override
    public void process() {
        JSONObject ROOT = new JSONObject();
        JSONArray folders = new JSONArray();

        UUID collectionId = UUID.randomUUID();
        for(TypeElement annotation : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                List<? extends VariableElement> parameters = getVariableElements(element);
                List<QueryParam> queryParams = getQueryParameters(parameters);
                JSONObject requestBody = getRequestBody(element, parameters);

                JSONArray items = ROOT.optJSONArray(StringPool.REQUESTS, new JSONArray());
                ROOT.put(StringPool.REQUESTS, items);

                cURL cURLObject = element.getAnnotation(cURL.class);
                String classLevelRequestMapping = getClassLevelRequestPath(element);
                String classMethodName = element.getSimpleName().toString();
                String requestName = Strings.nonNullNonEmpty(cURLObject.name()) ? cURLObject.name() : classMethodName;
                HttpRequestMapping httpRequestMapping = getHttpRequestMapping(element);
                String folderName = getFolderPath(classLevelRequestMapping, cURLObject);
                String containerId = findFolderRecursive(folders, folderName, "");

                JSONObject item = buildHttpRequestItem(collectionId, containerId, classLevelRequestMapping, requestName, httpRequestMapping, queryParams, requestBody);
                if(Objects.nonNull(item)){
                    items.put(item);
                }
            }
        }

        if(ROOT.has(StringPool.REQUESTS)){
            addCollectionDetails(ROOT, collectionId);
            ROOT.put(StringPool.FOLDERS, folders);
            writeToOutputFile("thunder_collection.json", ROOT.toString(4));
        }
    }

    /**
     * Build http request
     * @param collectionId - collection id
     * @param containerId - container id
     * @param classLevelRequestMapping - class level request mapping
     * @param httpRequestMapping - http request mapping
     * @param queryParams - query parameters
     * @param convertedClassToJSON - converted class to JSON
     * @return - http request item
     */
    private JSONObject buildHttpRequestItem(UUID collectionId, String containerId, String classLevelRequestMapping, String requestName, HttpRequestMapping httpRequestMapping, List<QueryParam> queryParams, JSONObject convertedClassToJSON) {
        JSONObject result = null;
        if(Objects.nonNull(httpRequestMapping)){
            String path = LOCALHOST + classLevelRequestMapping + httpRequestMapping.getRequestPath();

            result = new JSONObject();
            result.put(StringPool._ID, UUID.randomUUID());
            result.put(StringPool.COLID, collectionId);
            result.put(StringPool.CONTAINERID, containerId);
            result.put(StringPool.NAME, requestName);
            result.put(StringPool.URL, path);
            result.put(StringPool.METHOD, httpRequestMapping.getMethod());
            result.put(StringPool.CREATED, Dates.format(Dates.current(), DatePattern.Y_M_D_T_HMSSSSZ));
            result.put(StringPool.MODIFIED, Dates.format(Dates.current(), DatePattern.Y_M_D_T_HMSSSSZ));
            result.put(StringPool.HEADERS, headers(httpRequestMapping.extractHeaders()));
            result.put(StringPool.PARAMS, params(queryParams));
            if(Objects.nonNull(convertedClassToJSON)){
                result.put(StringPool.BODY, prepareRequestBody(convertedClassToJSON));
            }
        }
        return result;
    }

    /**
     * Add collection details
     * @param root JSONObject
     * @param collectionId UUID
     */
    private void addCollectionDetails(JSONObject root, UUID collectionId) {
        root.put(StringPool.CLIENTNAME, "Thunder Client");
        root.put(StringPool.COLLECTIONNAME, Strings.nonNullNonEmpty(cfg.getCollectionName()) ? cfg.getCollectionName() : "Thunder Client Collection");
        root.put(StringPool.COLLECTIONID, collectionId);
        root.put(StringPool.DATEEXPORTED, Dates.format(Dates.current(), DatePattern.Y_M_D_T_HMSSSSZ));
        root.put(StringPool.VERSION, "1.2");
    }

    /**
     * Prepare headers
     * @param strings - variable names
     * @return - variables
     */
    private JSONArray headers(List<String> strings) {
        JSONArray headers = new JSONArray();
        for(String header : strings){
            JSONObject headerObject = new JSONObject();
            String[] headerArray = header.split("=");
            headerObject.put(StringPool.NAME, headerArray[0]);
            headerObject.put(StringPool.VALUE, headerArray[1]);
            headers.put(headerObject);
        }
        return headers;
    }

    /**
     * Prepare params
     * @param queryParams - query parameters
     * @return - params
     */
    private JSONArray params(List<QueryParam> queryParams) {
        JSONArray params = new JSONArray();
        for(QueryParam queryParam : queryParams){
            JSONObject param = new JSONObject();
            param.put(StringPool.NAME, queryParam.getName());
            param.put(StringPool.VALUE, queryParam.getDefaultValue());
            param.put(StringPool.IS_PATH, false);
            params.put(param);
        }
        return params;
    }

    /**
     * Prepare request body
     * @param convertedClassToJSON - converted class to JSON
     * @return - request body
     */
    private JSONObject prepareRequestBody(JSONObject convertedClassToJSON) {
        JSONObject body = new JSONObject();
        body.put(StringPool.TYPE, StringPool.JSON);
        body.put(StringPool.RAW, convertedClassToJSON.toString());
        return body;
    }

    /**
     * Get container id
     * @param foldersArray - folders array
     * @param folderName - folder name
     * @param containerId - container id
     * @return - container id
     */
    private String findFolderRecursive(JSONArray foldersArray, String folderName, String containerId){
        folderName = folderName.startsWith("/") ? folderName.substring(1) : folderName;
        int fwdSlashIndex = folderName.indexOf("/");
        if(fwdSlashIndex > 0){
            String folder = folderName.substring(0, fwdSlashIndex);
            JSONObject folderObject = findFolder(foldersArray, folder, containerId);
            if(Objects.isNull(folderObject)) {
                folderObject = createFolder(folder, containerId);
                foldersArray.put(folderObject);
            }
            return findFolderRecursive(foldersArray, folderName.substring(fwdSlashIndex + 1), folderObject.optString(StringPool._ID));
        }else{
            JSONObject folderObject = findFolder(foldersArray, folderName, containerId);
            if(Objects.isNull(folderObject)){
                JSONObject newFolder = createFolder(folderName, containerId);
                foldersArray.put(newFolder);
                return newFolder.optString(StringPool._ID);
            }else{
                return folderObject.optString(StringPool._ID);
            }
        }
    }

    /**
     * Find folder
     * @param folders - folders
     * @param name - folder name
     * @param containerId - container id
     * @return - folder object
     */
    private JSONObject findFolder(JSONArray folders, String name, String containerId){
        name = Commons.convertCamelCaseToName(name);
        JSONObject result = null;
        for(int i = 0; i < folders.length(); i++){
            JSONObject folder = folders.getJSONObject(i);
            if(folder.optString(StringPool.NAME).equalsIgnoreCase(name)
                && folder.optString(StringPool.CONTAINERID).equals(containerId)){
                result = folder;
                break;
            }
        }
        return result;
    }

    /**
     * Create folder
     * @param name folder name
     * @param containerId container id
     * @return folder object
     */
    private JSONObject createFolder(String name, String containerId){
        JSONObject newFolder = new JSONObject();
        newFolder.put(StringPool._ID, UUID.randomUUID().toString());
        newFolder.put(StringPool.NAME, Commons.convertCamelCaseToName(name));
        newFolder.put(StringPool.CONTAINERID, containerId);
        newFolder.put(StringPool.CREATED, Dates.format(Dates.current(), DatePattern.Y_M_D_T_HMSSSSZ));
        return newFolder;
    }
}
