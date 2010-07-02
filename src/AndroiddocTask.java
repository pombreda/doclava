

import com.sun.javadoc.LanguageVersion;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.util.ArrayList;
import java.util.List;

public class AndroiddocTask extends Task {
    
    Arguments args = null;
    
    @Override
    public void execute() {
        if (args == null) {
            throw new BuildException("No command specified.");
        }
        
        /*
        String[] argz = args.getArguments();
        for (int i=0;i<argz.length;i++) {
            System.out.println(argz[i]);
        }
        */
        try {
          
          // TODO: fork a process to run java on DroidDoc.main
          //DroidDoc.main(args.getArguments());
          //com.sun.tools.javadoc.Main.execute(args.getArguments());
          
            
        } catch (Exception e) {
            throw new BuildException("Error generating documentation.",e);
        }
    }

    // Consumes <arguments/> build.xml element
    public Arguments createArguments() {
        args = new Arguments();
        return args;
    }
    
    public class Arguments {
        private String text;
        public Arguments() {}
        
        public void addText(String text) {
           this.text=getProject().replaceProperties(text);
        }
        
        public String[] getArguments() {
            if (text == null) return null;
            List<String>parts = new ArrayList<String>();
            int cur = 0, length = text.length();
            while (cur < length) {
                
                int open_quote_pos = text.indexOf('"', cur);
                int close_quote_pos;
                if (open_quote_pos == -1) {
                    open_quote_pos = close_quote_pos = length;
                } else {
                    close_quote_pos = text.indexOf('"', open_quote_pos+1);
                    if (close_quote_pos == -1) {
                        throw new BuildException("Unterminated quote in command.");
                    }
                }
                
                String[] cmds = text.substring(cur, open_quote_pos).split("\\s+");
                for (int i = 0; i < cmds.length; i++) {
                    if (cmds[i].length() > 0) {
                        parts.add(cmds[i]);
                    }
                }
                if (open_quote_pos < close_quote_pos) {
                    parts.add(text.substring(open_quote_pos+1,close_quote_pos));
                }
                
                cur = close_quote_pos+1;
            }
            
            return parts.toArray(new String[]{});
        } 
    }
}
