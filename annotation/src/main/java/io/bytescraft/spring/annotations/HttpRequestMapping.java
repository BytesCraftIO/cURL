package io.bytescraft.spring.annotations;

import com.javaquery.util.collection.Collections;
import com.javaquery.util.string.Strings;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author javaquery
 * @since 0.0.1
 */
public class HttpRequestMapping {

    public enum RequestMethod {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE
    }

    private RequestMethod method;
    private String name;
    private List<Object> value;
    private List<Object> path;
    private List<Object> params;
    private List<Object> headers;
    private List<Object> consumes;
    private List<Object> produces;

    public HttpRequestMapping(AnnotationMirror annotationMirror, Map<? extends ExecutableElement, ? extends AnnotationValue> values) {
        switch (annotationMirror.getAnnotationType().toString()){
            case "org.springframework.web.bind.annotation.GetMapping":
                this.method = RequestMethod.GET;
                break;
            case "org.springframework.web.bind.annotation.PostMapping":
                this.method = RequestMethod.POST;
                break;
            case "org.springframework.web.bind.annotation.PutMapping":
                this.method = RequestMethod.PUT;
                break;
            case "org.springframework.web.bind.annotation.DeleteMapping":
                this.method = RequestMethod.DELETE;
                break;
            case "org.springframework.web.bind.annotation.PatchMapping":
                this.method = RequestMethod.PATCH;
                break;
        }

        for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()){
            switch (entry.getKey().getSimpleName().toString()){
                case "name":
                    this.name = entry.getValue().getValue().toString();
                    break;
                case "value":
                    this.value = (List<Object>) entry.getValue().getValue();
                    break;
                case "path":
                    this.path = (List<Object>) entry.getValue().getValue();
                    break;
                case "params":
                    this.params = (List<Object>) entry.getValue().getValue();
                    break;
                case "headers":
                    this.headers = (List<Object>) entry.getValue().getValue();
                    break;
                case "consumes":
                    this.consumes = (List<Object>) entry.getValue().getValue();
                    break;
                case "produces":
                    this.produces = (List<Object>) entry.getValue().getValue();
                    break;
            }
        }
    }

    public RequestMethod getMethod() {
        return method;
    }

    public List<String> extractHeaders(){
        List<String> result = new ArrayList<>();
        for (Object header : this.headers) {
            result.add(header.toString().replace("\"", ""));
        }
        return result;
    }

    public String getRequestPath(){
        String result = "";
        if(Collections.nonNullNonEmpty(this.value)){
            result = this.value.get(0).toString();
        }else if(Collections.nonNullNonEmpty(this.path)){
            result = this.path.get(0).toString();
        }
        if(Strings.nonNullNonEmpty(result)){
            result = result.replace("\"", "");
        }
        return result;
    }
}
