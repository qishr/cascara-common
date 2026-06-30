package io.github.qishr.cascara.common.lang.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
        ElementType.CONSTRUCTOR, ElementType.LOCAL_VARIABLE, ElementType.PACKAGE,
        ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE, ElementType.TYPE_PARAMETER
})
public @interface Example {}