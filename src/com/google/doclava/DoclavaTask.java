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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.util.ArrayList;
import java.util.List;

public final class DoclavaTask extends Task {

  Arguments args;

  @Override
  public void execute() {
    if (args == null) {
      throw new BuildException("No arguments specified.");
    }

    try {
      com.sun.tools.javadoc.Main.execute(args.getArgumentsList().toArray(new String[] {}));
    } catch (Exception e) {
      throw new BuildException("Error generating documentation.", e);
    }
  }

  /**
   * Consumes <arguments/> build.xml element.
   * 
   */
  public Arguments createArguments() {
    if (args != null) {
      throw new BuildException("Arguments parameter set multiple times.");
    }
    args = new Arguments();
    return args;
  }

  public class Arguments {
    private String text;

    public Arguments() {}

    public void addText(String text) {
      this.text = getProject().replaceProperties(text);
    }

    /**
     * Returns a list of arguments. Arguments are whitespace-separated character sequences. If an
     * argument starts with a double quotation mark, then the argument consists of all characters
     * (including whitespace) preceding the closing quotation mark.
     */
    public List<String> getArgumentsList() {
      if (text == null) {
        return null;
      }

      List<String> parts = new ArrayList<String>();
      int currentPosition = 0, length = text.length();

      while (currentPosition < length) {
        int openQuotePosition = text.indexOf('"', currentPosition);
        int closeQuotePosition;
        if (openQuotePosition == -1) {
          openQuotePosition = closeQuotePosition = length;
        } else {
          closeQuotePosition = text.indexOf('"', openQuotePosition + 1);
          if (closeQuotePosition == -1) {
            throw new BuildException("Unterminated quote in arguments.");
          }
        }

        String[] cmds = text.substring(currentPosition, openQuotePosition).split("\\s+");
        for (int i = 0; i < cmds.length; i++) {
          if (cmds[i].length() > 0) {
            parts.add(cmds[i]);
          }
        }
        if (openQuotePosition < closeQuotePosition) {
          parts.add(text.substring(openQuotePosition + 1, closeQuotePosition));
        }

        currentPosition = closeQuotePosition + 1;
      }

      return parts;
    }
  }
}
