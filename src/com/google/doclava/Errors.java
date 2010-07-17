/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.doclava;

import java.util.TreeSet;

public class Errors {
  public static boolean hadError = false;
  private static boolean warningsAreErrors = false;
  private static TreeSet<Message> allErrors = new TreeSet<Message>();

  private static class Message implements Comparable {
    SourcePositionInfo pos;
    int level;
    String msg;

    Message(SourcePositionInfo p, int l, String m) {
      pos = p;
      level = l;
      msg = m;
    }

    public int compareTo(Object o) {
      Message that = (Message) o;
      int r = this.pos.compareTo(that.pos);
      if (r != 0) return r;
      return this.msg.compareTo(that.msg);
    }

    @Override
    public String toString() {
      String whereText = this.pos == null ? "unknown: " : this.pos.toString() + ':';
      return whereText + this.msg;
    }
  }

  public static void error(Error error, SourcePositionInfo where, String text) {
    if (error.level == HIDDEN) {
      return;
    }

    int level = (!warningsAreErrors && error.level == WARNING) ? WARNING : ERROR;
    String which = level == WARNING ? " warning " : " error ";
    String message = which + error.code + ": " + text;

    if (where == null) {
      where = new SourcePositionInfo("unknown", 0, 0);
    }

    allErrors.add(new Message(where, level, message));

    if (error.level == ERROR || (warningsAreErrors && error.level == WARNING)) {
      hadError = true;
    }
  }

  public static void printErrors() {
    for (Message m : allErrors) {
      if (m.level == WARNING) {
        System.err.println(m.toString());
      }
    }
    for (Message m : allErrors) {
      if (m.level == ERROR) {
        System.err.println(m.toString());
      }
    }
  }

  public static int HIDDEN = 0;
  public static int WARNING = 1;
  public static int ERROR = 2;

  public static void setWarningsAreErrors(boolean val) {
    warningsAreErrors = val;
  }

  public static class Error {
    public int code;
    public int level;

    public Error(int code, int level) {
      this.code = code;
      this.level = level;
    }
  }

  public static Error UNRESOLVED_LINK = new Error(1, WARNING);
  public static Error BAD_INCLUDE_TAG = new Error(2, WARNING);
  public static Error UNKNOWN_TAG = new Error(3, WARNING);
  public static Error UNKNOWN_PARAM_TAG_NAME = new Error(4, WARNING);
  public static Error UNDOCUMENTED_PARAMETER = new Error(5, HIDDEN);
  public static Error BAD_ATTR_TAG = new Error(6, ERROR);
  public static Error BAD_INHERITDOC = new Error(7, HIDDEN);
  public static Error HIDDEN_LINK = new Error(8, WARNING);
  public static Error HIDDEN_CONSTRUCTOR = new Error(9, WARNING);
  public static Error UNAVAILABLE_SYMBOL = new Error(10, ERROR);
  public static Error HIDDEN_SUPERCLASS = new Error(11, WARNING);
  public static Error DEPRECATED = new Error(12, HIDDEN);
  public static Error DEPRECATION_MISMATCH = new Error(13, WARNING);
  public static Error MISSING_COMMENT = new Error(14, WARNING);
  public static Error IO_ERROR = new Error(15, HIDDEN);
  public static Error NO_SINCE_DATA = new Error(16, HIDDEN);
  public static Error NO_FEDERATION_DATA = new Error(17, WARNING);

  public static Error PARSE_ERROR = new Error(18, ERROR);
  public static Error ADDED_PACKAGE = new Error(19, WARNING);
  public static Error ADDED_CLASS = new Error(20, WARNING);
  public static Error ADDED_METHOD = new Error(21, WARNING);
  public static Error ADDED_FIELD = new Error(22, WARNING);
  public static Error ADDED_INTERFACE = new Error(23, WARNING);
  public static Error REMOVED_PACKAGE = new Error(24, WARNING);
  public static Error REMOVED_CLASS = new Error(25, WARNING);
  public static Error REMOVED_METHOD = new Error(26, WARNING);
  public static Error REMOVED_FIELD = new Error(27, WARNING);
  public static Error REMOVED_INTERFACE = new Error(28, WARNING);
  public static Error CHANGED_STATIC = new Error(29, WARNING);
  public static Error CHANGED_FINAL = new Error(30, WARNING);
  public static Error CHANGED_TRANSIENT = new Error(31, WARNING);
  public static Error CHANGED_VOLATILE = new Error(32, WARNING);
  public static Error CHANGED_TYPE = new Error(33, WARNING);
  public static Error CHANGED_VALUE = new Error(34, WARNING);
  public static Error CHANGED_SUPERCLASS = new Error(35, WARNING);
  public static Error CHANGED_SCOPE = new Error(36, WARNING);
  public static Error CHANGED_ABSTRACT = new Error(37, WARNING);
  public static Error CHANGED_THROWS = new Error(38, WARNING);
  public static Error CHANGED_NATIVE = new Error(39, HIDDEN);
  public static Error CHANGED_CLASS = new Error(40, WARNING);
  public static Error CHANGED_DEPRECATED = new Error(41, WARNING);
  public static Error CHANGED_SYNCHRONIZED = new Error(42, ERROR);
  
  public static Error[] ERRORS =
      {UNRESOLVED_LINK, BAD_INCLUDE_TAG, UNKNOWN_TAG, UNKNOWN_PARAM_TAG_NAME,
          UNDOCUMENTED_PARAMETER, BAD_ATTR_TAG, BAD_INHERITDOC, HIDDEN_LINK, HIDDEN_CONSTRUCTOR,
          UNAVAILABLE_SYMBOL, HIDDEN_SUPERCLASS, DEPRECATED, IO_ERROR, NO_SINCE_DATA,
          NO_FEDERATION_DATA, PARSE_ERROR, ADDED_PACKAGE, ADDED_CLASS, ADDED_METHOD, ADDED_FIELD,
          ADDED_INTERFACE, REMOVED_PACKAGE, REMOVED_CLASS, REMOVED_METHOD, REMOVED_FIELD,
          REMOVED_INTERFACE, CHANGED_STATIC, CHANGED_FINAL, CHANGED_TRANSIENT, CHANGED_VOLATILE,
          CHANGED_TYPE, CHANGED_VALUE, CHANGED_SUPERCLASS, CHANGED_SCOPE, CHANGED_ABSTRACT,
          CHANGED_THROWS, CHANGED_NATIVE, CHANGED_CLASS, CHANGED_DEPRECATED, CHANGED_SYNCHRONIZED};

  public static boolean setErrorLevel(int code, int level) {
    for (Error e : ERRORS) {
      if (e.code == code) {
        e.level = level;
        return true;
      }
    }
    return false;
  }
}
