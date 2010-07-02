
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

// Copyright 2010 Google Inc. All Rights Reserved.


public class JarUtils {
    /**
     * Returns the .jar for this context.
     */
    private static File thisJar() {
        String path = JarUtils.class.getName().replace('.', '/') + ".class";
        URL jarUrl = JarUtils.class.getResource(path);
        if (jarUrl == null) {
            throw new IllegalStateException("Cannot find context's own .jar");
        }

        String url = jarUrl.toString();
        int bang = url.indexOf("!");
        String JAR_URI_PREFIX = "jar:file:";
        if (url.startsWith(JAR_URI_PREFIX) && bang != -1) {
            return new File(url.substring(JAR_URI_PREFIX.length(), bang));
        } else {
            throw new IllegalStateException("Cannot find this context's .jar file in " + jarUrl);
        }
    }
    
    /**
     * Copies a directory within this context's jar file to an external directory
     * @param sourceDir
     * @param destDir
     */
    public static void resourcesToDirectory(String sourceDir, String destDir)
        throws IOException
    {
        File file = thisJar();
        JarFile jar = new JarFile(file);
        
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements())
        {
            JarEntry entry = entries.nextElement();
            if (entry.getName().startsWith(sourceDir+"/") && !entry.isDirectory())
            {
                File dest = new File(destDir + "/" + entry.getName().substring(sourceDir.length()+1));
                File parent = dest.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                
                FileOutputStream out = new FileOutputStream(dest);
                InputStream in = jar.getInputStream(entry);
                
                try {
                    byte[] buffer = new byte[8*1024];
                    
                    int s = 0;
                    while ((s = in.read(buffer)) > 0)
                    {
                        out.write(buffer, 0, s);
                    }
                } catch (IOException e) {
                    try {
                        dest.delete();
                    } catch (Exception ignored) {}
                    throw new IOException("Could not copy asset from jar file", e);
                } finally {
                    try {
                        in.close();
                    } catch (IOException ignored) {}
                    try {
                        out.close();
                    } catch (IOException ignored) {}
                }
            }
        }
        
    }

}
