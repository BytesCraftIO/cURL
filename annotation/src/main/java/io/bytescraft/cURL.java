package io.bytescraft;

import java.lang.annotation.*;

/**
 * cURL annotation to generate postman collection.
 * @author javaquery
 * @since 0.0.1
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface cURL {
    /**
     * Folder name where postman collection will be saved.
     * @return folder name
     */
    String folder() default "";
    /**
     * Request name to be used otherwise method name will be used.
     * @return request name
     */
    String name() default "";
}
