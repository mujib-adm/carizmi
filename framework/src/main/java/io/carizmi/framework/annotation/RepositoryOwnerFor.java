package io.carizmi.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the class as the owner of the specified repository.
 *
 * <p>Enforced by ArchUnit to guarantee the 1-to-1 repository ownership pattern:
 * only the annotated class (and its sealed hierarchy) may access the declared repository.</p>
 *
 * @see DomainLogicFor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepositoryOwnerFor {

    /** The repository interface that this class exclusively owns. */
    Class<?> value();
}