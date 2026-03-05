package org.sofumar.portal.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for ADMIN or MANAGER access.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole(T(org.sofumar.portal.constants.Role).ADMIN.name()) or hasRole(T(org.sofumar.portal.constants.Role).MANAGER.name())")
public @interface IsAdminOrManager {
}