package org.swarg.mc.optistats;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 15-11-21
 * @author Swarg
 */
public class LagStatsTest
{

    public LagStatsTest() {
    }


    /**
     * Test of genRndLagFile method, of class LagStats.
     */
    //@Test
    public void testGenRndLagFile() throws Exception {
        System.out.println("genRndLagFile");
        String log = "lag.log.bin";
        boolean canRewrite = true;
        LagStats.genRndLagFile(96, log, canRewrite, System.out, false);
        //System.out.println(LagStats.getLagReadable(log, 0, 0));//+
        LagStats.createChartImg(log, "lag.png", 1280, 400, 0, 0);
    }

}
