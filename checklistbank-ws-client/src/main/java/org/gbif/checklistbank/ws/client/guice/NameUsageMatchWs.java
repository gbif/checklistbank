package org.gbif.checklistbank.ws.client.guice;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Guice binding annotation intended to be applied to injected parameters specifying the URL of the nub lookup service.
 * For example http://api.gbif.org/species/match
 */
@BindingAnnotation
@Retention(RUNTIME)
@Target({FIELD, PARAMETER, METHOD})
public @interface NameUsageMatchWs {

}
