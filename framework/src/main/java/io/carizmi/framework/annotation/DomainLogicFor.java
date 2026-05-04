package io.carizmi.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the class as the business logic owner for the specified Value Object.
 *
 * <p>Used by the framework's runtime singleton enforcement in {@code AbstractBusinessLogic}
 * to guarantee that each Value Object has exactly one business logic implementation.</p>
 *
 * @see RepositoryOwnerFor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainLogicFor {

    /** The Value Object class that this business logic exclusively owns. */
    Class<?> value();
}