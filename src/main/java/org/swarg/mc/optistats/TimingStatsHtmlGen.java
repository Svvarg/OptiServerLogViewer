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
 * 
 *https://developers.google.com/chart/interactive/docs/reference#google_visualization_data_join
 * //google.visualization.data.join                 (dt1, dt2, joinMethod, keys, dt1Columns, dt2Columns);
 * var joinedData = google.visualization.data.join(data1, data2, 'full', [[0, 0]], [1,2], [1]);
 * google.visualization.data.join
 * //Две таблицы соединяются в одну. на подобии SQL join т.е столбцы как бы
 * комбинируются из двух таблиц в одну поэтому в настройках для соединение точек
 * из как бы разных таблиц нужно в options обязательно указать:(иначе будут точки)
 * interpolateNulls: true
 *
 * explorer - позволяет "двигать" таблицу в интерактивном режиме и изменять маштаб
 *
 * https://developers.google.com/chart/interactive/docs/gallery/barchart
 * 
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
    public static Path createHtmlChart(Path blStats, Path blLags, Path html, long s, long e) throws IOException {
        List<StatEntry> selist = TimingStats.parseFromBin(blStats, s, e);
        List<LagEntry> lelist  = LagStats.parseFromBin(blLags, s, e);
        String pattern = Config.getResourceAsString("chartStats.html", StandardCharsets.UTF_8);
        //System.out.println(pattern);
        if (pattern != null) {
            int bi = pattern.indexOf("//#DATA_BEGIN");
            int ei = pattern.indexOf("//#DATA_END");
            Path dir = html.getParent();
            if (dir != null && !Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            StringBuilder data = new StringBuilder();
            data.append(pattern, 0, bi);
            final String replacedt = "$RDateTime";
            int i = pattern.indexOf(replacedt);
            String date = Utils.getFormatedTimeInterval(s, e);
            data.replace(i, i + replacedt.length(), date);

            //create chart-data
            createJSGoogleChartData(selist, lelist, data);

            data.append(pattern, ei, pattern.length());

            return Files.write(html, data.toString().getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    /**
     * Создание набора данных для JS Google Charts
     * @param selist
     * @param lelist
     * @param sb
     */
    private static void createJSGoogleChartData(List<StatEntry> selist, List<LagEntry> lelist, StringBuilder sb) {
        final String dac = "data.addColumn('";
        sb.append("function fillDataStats(data){\n");
       
        sb.append(dac).append("datetime','DateTime');\n");
//        sb.append(dac).append("number',  'Tps(x100)');\n");
        sb.append(dac).append("number',  'Tps');\n");
        sb.append(dac).append("number',  'UsedMem(Mb)');\n");
        sb.append(dac).append("number',  'Online');\n");
        sb.append(dac).append("number',  'Chunks');\n");
        sb.append(dac).append("number',  'Entities');\n");
        sb.append(dac).append("number',  'Tiles');\n");

        sb.append("data.addRows([");
        if (selist != null) {
            for (int i = 0; i < selist.size(); i++) {
                StatEntry se = selist.get(i);
                sb.append("[new Date(").append(se.time).append("),");
                 sb.append((se.tps & 0xFF) / 10.0 ).append(","); //200 -> 20.0 2000 -> 20.00
                 sb.append(se.memUsed).append(",");
                 sb.append(se.online ).append(",");//"маштабирование"
                 sb.append(se.chunks).append(",");
                 sb.append(se.entities).append(",");
                 sb.append(se.tiles).append("],");
            }
        }
        sb.append("]);\n");
        sb.append("}\n");

        //---- lags ------
        sb.append("function fillDataLags(data){\n");
        sb.append(dac).append("datetime', 'DateTime');\n");
        sb.append(dac).append("number',   'lags(ms)');\n");

        sb.append("data.addRows([");
        if (lelist != null) {
            for (int i = 0; i < lelist.size(); i++) {
                LagEntry e = lelist.get(i);
                sb.append("[new Date(").append(e.time).append("),");
                sb.append(e.lag).append("],");
            }
        }
        sb.append("]);\n");
        sb.append("}");
    }


}
