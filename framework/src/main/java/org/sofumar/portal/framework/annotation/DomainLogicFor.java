package org.sofumar.portal.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a Business Logic class as the owner of a specific Value Object (VO) class.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainLogicFor {
    Class<?> value(); // the VO class this BL owns
}