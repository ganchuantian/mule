/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Referenced error types do exist in the context of the artifact
 */
public class ErrorHandlerOnErrorTypeExists implements Validation {

  private static final String ON_ERROR = "on-error";
  private static final String ON_ERROR_PROPAGATE = "on-error-propagate";
  private static final String ON_ERROR_CONTINUE = "on-error-continue";

  private static final ComponentIdentifier ON_ERROR_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR).build();
  private static final ComponentIdentifier ON_ERROR_PROPAGATE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR_PROPAGATE).build();
  private static final ComponentIdentifier ON_ERROR_CONTINUE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR_CONTINUE).build();

  @Override
  public String getName() {
    return "Error Type references exist";
  }

  @Override
  public String getDescription() {
    return "Referenced error types do exist in the context of the artifact.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(comp -> (comp.getIdentifier().equals(ON_ERROR_IDENTIFIER)
        || comp.getIdentifier().equals(ON_ERROR_PROPAGATE_IDENTIFIER)
        || comp.getIdentifier().equals(ON_ERROR_CONTINUE_IDENTIFIER))
        && comp.getParameter("type") != null
        && comp.getParameter("type").getResolvedRawValue() != null);
  }

  @Override
  public Optional<String> validate(ComponentAst onErrorModel, ArtifactAst artifact) {
    final Optional<ErrorType> errorType = artifact.getErrorTypeRepository()
        .lookupErrorType(parserErrorType(onErrorModel.getParameter("type").getResolvedRawValue()));
    if (!errorType.isPresent()) {
      return of(format("Could not find error '%s' used in %s", errorType, compToLoc(onErrorModel)));
    }

    return empty();
  }

  private static ComponentIdentifier parserErrorType(String representation) {
    int separator = representation.indexOf(':');
    String namespace;
    String identifier;
    if (separator > 0) {
      namespace = representation.substring(0, separator).toUpperCase();
      identifier = representation.substring(separator + 1).toUpperCase();
    } else {
      namespace = CORE_PREFIX.toUpperCase();
      identifier = representation.toUpperCase();
    }

    return builder().name(identifier).namespace(namespace).build();
  }

  private String compToLoc(ComponentAst component) {
    return "[" + component.getMetadata().getFileName().orElse("unknown") + ":"
        + component.getMetadata().getStartLine().orElse(-1) + "]";
  }

}
