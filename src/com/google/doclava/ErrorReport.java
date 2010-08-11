package com.google.doclava;

import java.util.Set;

public final class ErrorReport {
    private final int code;
    private final Set<ErrorMessage> errors;
    
    public ErrorReport(int code, Set<ErrorMessage> errors) {
      this.code = code;
      this.errors = errors;
    }
    
    public int getCode() {
      return code;
    }
    
    public Set<ErrorMessage> getErrors() {
      return errors;
    }
  }