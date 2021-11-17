package org.swarg.mc.optistats;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 17-11-21
 * @author Swarg
 */
public class TimingStatsHtmlGenTest {

    /**
     * Test of createHtmlChart method, of class TimingStatsHtmlGen.
     */
    @Test
    public void testCreateHtmlChart() throws IOException {
        System.out.println("createHtmlChart");
        String blStats = "";
        String blLags = "";
        String html = "";
        long s = 0L;
        long e = 0L;
        TimingStatsHtmlGen.createHtmlChart(blStats, blLags, html, s, e);
    }

}
