package org.swarg.mc.optistats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.swarg.mc.optistats.model.LagEntry;
import org.swarg.mcforge.statistic.StatEntry;

/**
 * 17-11-21
 * @author Swarg
 */
class TimingStatsHtmlGen {

    /**
     *
     * @param blStats
     * @param blLags
     * @param html
     * @param s startStampTime
     * @param e endStampTime
     */
    public static Path createHtmlChart(String blStats, String blLags, String html, long s, long e) throws IOException {
        List<StatEntry> selist = TimingStats.parseFromBin(blStats, s, e);
        List<LagEntry> lelist  = LagStats.parseFromBin(blLags, s, e);
        String pattern = Utils.getResourceAsString("chartStats.html");
        //System.out.println(pattern);
        if (pattern != null) {
            int bi = pattern.indexOf("//#DATA_BEGIN");
            int ei = pattern.indexOf("//#DATA_END");
            Path htmlp = Paths.get(html);
            Path dir = htmlp.getParent();
            if (dir != null && !Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            StringBuilder data = new StringBuilder();
            data.append(pattern, 0, bi);
            //create chart-data
            createJSGoogleChartData(selist, lelist, data);

            data.append(pattern, ei, pattern.length());

            return Files.write(htmlp, data.toString().getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    private static void createJSGoogleChartData(List<StatEntry> selist, List<LagEntry> lelist, StringBuilder sb) {
        final String dac = "data.addColumn('";
        sb.append("function fillData(data){\n");
        sb.append(dac).append("datetime', 'DateTime');\n");
        //StatEntry
        sb.append(dac).append("number', 'memUsed');\n");
        sb.append(dac).append("number', 'tps');\n");
        sb.append(dac).append("number', 'online');\n");
        sb.append(dac).append("number', 'chunks');\n");
        sb.append(dac).append("number', 'entities');\n");
        sb.append(dac).append("number', 'tiles');\n");

        //LagsEntry
        //sb.append(dac).append("number', 'lags');\n");

        sb.append("data.addRows([\n");
            for (int i = 0; i < selist.size(); i++) {
                StatEntry se = selist.get(i);
                sb.append("[new Date(").append(se.time).append("), ");
                 sb.append(se.memUsed).append(", ");
                 sb.append((se.tps & 0xFF)).append(", ");
                 sb.append(se.online).append(", ");
                 sb.append(se.chunks).append(", ");
                 sb.append(se.entities).append(", ");
                 sb.append(se.tiles).append("],\n");
            }
        sb.append("]);\n");
        sb.append("}");
    }


}
