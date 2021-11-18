package org.swarg.mc.optistats;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.swarg.common.AbstractAppConfig;

/**
 * 16-11-21
 * @author Swarg
 */
public class Config extends AbstractAppConfig {

    public Config(String configFile) {
        super(configFile);
    }

    @Override
    protected boolean createDefaultConfig(Path path) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("DefaultConfig.properties")) {
            long sz = Files.copy(is, configFile);
            is.close();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }






}
