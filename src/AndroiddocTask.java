
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.util.ArrayList;
import java.util.List;

public final class AndroiddocTask extends Task {

  Arguments args;

  @Override
  public void execute() {
    if (args == null) {
      throw new BuildException("No arguments specified.");
    }

    try {

      // TODO: fork a process to run java on DroidDoc.main
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

    public Arguments() {
    }

    public void addText(String text) {
      this.text = getProject().replaceProperties(text);
    }

    /**
     * Returns a list of arguments. Arguments are whitespace-separated character
     * sequences. If an argument starts with a double quotation mark, then the
     * argument consists of all characters (including whitespace) preceding the
     * closing quotation mark.
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
