package org.swarg.mc.optistats;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.swarg.mc.optistats.jfreechart.LagStatsJFC;

/**
 * 15-11-21
 * @author Swarg
 */
public class LagStatsTest {

    /**
     * Test of genRndLagFile method, of class LagStats.
     */
    //@Test
    public void testGenRndLagFile() throws Exception {
        System.out.println("genRndLagFile");
        Path log = Paths.get("build/generated-lags.bin");
        boolean canRewrite = true;
        LagStats.genRndLagFile(96, log, canRewrite, System.out, false);
        //System.out.println(LagStats.getLagReadable(log, 0, 0));//+
        LagStatsJFC.createChartImg(log, Paths.get("lag.png"), 1280, 400, 0, 0);
    }

}
