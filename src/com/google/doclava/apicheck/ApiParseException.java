// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.doclava.apicheck;

public final class ApiParseException extends Exception {
  public ApiParseException(String message, Exception cause) {
    super(message, cause);
  }
  
  public ApiParseException() {
    
  }
  
  ApiParseException(String message) {
    super(message);
  }
}
