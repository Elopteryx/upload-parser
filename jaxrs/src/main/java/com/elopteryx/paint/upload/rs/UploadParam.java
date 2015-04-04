package com.elopteryx.paint.upload.rs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation type which can be used to set a part item as a
 * parameter in the endpoint. It only works on simple {@link Part}
 * parameters, it is ignored on the others.
 */
@Target(value = { ElementType.PARAMETER, ElementType.FIELD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface UploadParam {

    /**
     * The name of the form param in the multipart request.
     * @return The form name to match
     */
    String value() default "";
}
