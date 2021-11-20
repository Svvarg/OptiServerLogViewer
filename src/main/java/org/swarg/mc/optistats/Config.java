package org.swarg.mc.optistats;

import java.nio.file.Path;
import org.swarg.common.AbstractAppConfig;

/**
 * 16-11-21
 * @author Swarg
 */
public class Config extends AbstractAppConfig {

    public Config(String configFile) {
        super(configFile);
        this.defConfigDirInUserHome = "mcs-stats";
    }

    @Override
    protected boolean createDefaultConfig(Path path) {
        try {
            return AbstractAppConfig.copyFromResource(getDefaultConfigName(), path, false, this.out);
        }
        catch (Exception e) {
            return false;
        }
    }

}
