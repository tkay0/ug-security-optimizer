package org.ugoptimizer.frontend;

/** Runtime boundary exception used when backend persistence fails during a frontend action. */
public final class FrontendServiceException extends RuntimeException {
  public FrontendServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
