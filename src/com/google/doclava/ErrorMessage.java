package com.google.doclava;

import com.google.doclava.ErrorCode;

public final class ErrorMessage implements Comparable {
    private ErrorCode error;
    private SourcePositionInfo pos;
    private String msg;

    public ErrorMessage(ErrorCode e, SourcePositionInfo p, String m) {
      error = e;
      pos = p;
      msg = m;
    }

    public int compareTo(Object o) {
      ErrorMessage that = (ErrorMessage) o;
      int r = this.pos.compareTo(that.pos);
      if (r != 0) return r;
      return this.msg.compareTo(that.msg);
    }

    @Override
    public String toString() {
      String whereText = this.pos == null ? "unknown: " : this.pos.toString() + ':';
      return whereText + this.msg;
    }
    
    public ErrorCode getError() {
      return error;
    }
  }