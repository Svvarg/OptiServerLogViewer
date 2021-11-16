package org.swarg.mc.optistats;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 16-11-21
 * @author Swarg
 */
public class Config {
    public Properties props;
    //public String homeDir = System.getProperty("user.home");// + File.separator + "tmp-agent";
    public Path configFile;
    
    //DEBUG tools
    public boolean verbose;
    public PrintStream out;


    public Config(String cnfgfile) {
        verbose = true;
        props = new Properties();
        out = System.out;
        if (cnfgfile == null || cnfgfile.isEmpty()) {
            cnfgfile = "logviewer.properties";
        }
        this.configFile = Paths.get(cnfgfile);
    }


    public Config reload() {
        InputStream is = null;
        try {
            props.clear();

            if (!Files.exists(configFile)) {
                if (this.verbose) {
                    out.println("Create new Property file at: " + configFile.toAbsolutePath().toString());
                }
                //Cоздать дефолтный конфиг тело конфига взять из ресурсов jar-ника
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream("DefaultConfig.properties");
                long sz = Files.copy(is, configFile);
                try {
                    is.close();
                }
                catch (Exception e) {
                }
            }

            is = Files.newInputStream(configFile);
            if (is != null) {
                props.load(is);
                this.verbose = getBool("verbose", true);
            }
        }
        catch (Exception e) {
            e.printStackTrace(out);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (Exception e) {
                    e.printStackTrace(out);
                }
            }
        }
        return this;
    }


    public boolean getBool(String key, boolean _default) {
        return this.props == null ? _default : (key != null && "true".equalsIgnoreCase(props.getProperty(key, String.valueOf(_default))));
    }

}
