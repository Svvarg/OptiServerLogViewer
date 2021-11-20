package org.swarg.mc.optistats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 20-11-21
 * @author Swarg
 */
public class ConfigTest {

    @Test//+
    public void testCreateDefaultConfig() throws IOException {
        Config c = new Config(null);
        Path def = Paths.get("def.tmp").toAbsolutePath();
        if (Files.exists(def)){
            Files.delete(def);
        }
        c.createDefaultConfig(def);
        assertTrue(Files.exists(def));
        Files.delete(def);
    }

}
