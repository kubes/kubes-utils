package com.denniskubes.webasset;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingErrorReporter
  implements ErrorReporter {

  private final static Logger LOG = LoggerFactory
    .getLogger(LoggingErrorReporter.class);

  public void warning(String message, String sourceName, int line,
    String lineSource, int lineOffset) {
    if (line < 0) {
      LOG.warn(message);
    }
    else {
      LOG.warn(line + ':' + lineOffset + ':' + message);
    }
  }

  public void error(String message, String sourceName, int line,
    String lineSource, int lineOffset) {
    if (line < 0) {
      LOG.error(message);
    }
    else {
      LOG.error(line + ':' + lineOffset + ':' + message);
    }
  }

  public EvaluatorException runtimeError(String message, String sourceName,
    int line, String lineSource, int lineOffset) {
    error(message, sourceName, line, lineSource, lineOffset);
    return new EvaluatorException(message);
  }
}
