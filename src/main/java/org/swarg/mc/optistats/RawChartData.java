package org.swarg.mc.optistats;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import org.swarg.mc.optistats.model.LagEntry;
import org.swarg.mcforge.statistic.StatEntry;

/**
 * Создать из бинарного лога выборку данных для построение графика для последних
 * 24 часов. Через запрос созданных текстовых файлов js`ом
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
public class RawChartData {

    /**
     * Копирование ресурсов для отображения html страницы с графиками
     * из внутренностей джарника в каталог указанный в конфиге
     * chartStats.hmlt
     * favicon.ico
     * styles.css todo
     * js/chart.js
     * @param indexHtml можно указать как каталог так и конкретный файл под html
     * @param canReplace если файлы уже существуют и true - заменит их, иначе не тронет
     * @param out
     * @return 
     */
    public static boolean deployHtmlFromResources(Path indexHtml, boolean canReplace, PrintStream out) throws IOException {
        Objects.requireNonNull(indexHtml, "html index or directory");
        Path dir;
        if (Files.isDirectory(indexHtml)) {
            dir = indexHtml;
            indexHtml = dir.resolve("index.html");
        } else {
            dir = indexHtml.getParent();
            if (dir == null) {
                dir = Paths.get(".");
            }
        }
        //если не указан полный путь а только имя файла в текущем каталоге для indexHtml - выдаст null
        Path jsDir = dir.resolve("js");
        if (!Files.exists(jsDir)) {
            Files.createDirectories(jsDir);
            if (out != null) {
                out.println("[NEW] " + jsDir);
            }
        }

        Utils.copyFromResource("chartStats.html", indexHtml, canReplace, out);
        Utils.copyFromResource("js/chart.js", jsDir.resolve("chart.js"), canReplace, out);
        Utils.copyFromResource("favicon.ico", dir.resolve("favicon.ico"), canReplace, out);
        return true;
    }

    /**
     * создание stats.bin lags.bin для js-скрипта подтягивающего данные для
     * рендеринга графиков
     * @param blStats где лежат бинарные логи
     * @param blLags
     * @param indexHtml корень html куда ложить создаваемые для запроса через js файлы
     * @param s
     * @param e
     * @param out
     * @return
     */
    public static boolean createRawDataForJSChart(Path blStats, Path blLags, Path indexHtml, long s, long e, PrintStream out) throws IOException {
        List<StatEntry> selist = TimingStats.parseFromBin(blStats, s, e);
        List<LagEntry> lelist  = LagStats.parseFromBin(blLags, s, e);
        Path dir;
        if (Files.isDirectory(indexHtml)) {
            dir = indexHtml;
        } else {
            dir = indexHtml.getParent();
            if (dir == null) {
                dir = Paths.get(".");
            }
        }

        StringBuilder sb = new StringBuilder();
        Path p1 = dir.resolve("stats.bin");
        Path p2 = dir.resolve("lags.bin");

        p1 = Files.write(p1, getRawStatsData(selist, sb).toString().getBytes(StandardCharsets.UTF_8));
        p2 = Files.write(p2, getRawLagsData(lelist, sb).toString().getBytes(StandardCharsets.UTF_8));
        if (out != null) {
            out.println("[WRITE]: " + p1);
            out.println("[WRITE]: " + p2);
        }
        return true;
    }

    /**
     * Создание набора данных для JS Google Charts
     * @param selist
     * @param lelist
     * @param sb
     */
    //private static void binaryToRawStatsData(List<StatEntry> selist, List<LagEntry> lelist, StringBuilder sb) {
    private static StringBuilder getRawStatsData(List<StatEntry> selist, StringBuilder sb) {
        sb.setLength(0);
        sb.append("datetime:DateTime number:Tps number:UsedMem(Mb) number:Online number:Chunks number:Entities number:Tiles\n");
        if (selist != null) {
            for (int i = 0; i < selist.size(); i++) {
                if (i > 0) {
                    sb.append('\n');
                }
                StatEntry se = selist.get(i);
                sb.append(se.time).append(' ');
                sb.append((se.tps & 0xFF)).append(' ');
                sb.append(se.memUsed).append(' ');
                sb.append(se.online).append(' ');
                sb.append(se.chunks).append(' ');
                sb.append(se.entities).append(' ');
                sb.append(se.tiles);
            }
        }
        return sb;
    }

    private static StringBuilder getRawLagsData(List<LagEntry> lelist, StringBuilder sb) {
        sb.setLength(0);
        sb.append("datetime:DateTime number:Lags\n");
        if (lelist != null) {
            for (int i = 0; i < lelist.size(); i++) {
                if (i > 0) {
                    sb.append('\n');
                }
                LagEntry e = lelist.get(i);
                sb.append(e.time).append(' ');
                sb.append(e.lag);
            }
        }
        return sb;
    }


//    /**
//     * Генерация
//     * @param blStats
//     * @param blLags
//     * @param indexHtml
//     * @param s startStampTime
//     * @param e endStampTime
//     */
//    public static Path createHtmlChart(Path blStats, Path blLags, Path indexHtml, long s, long e) throws IOException {
//        Objects.requireNonNull(indexHtml, "html index or directory");
//
//        List<StatEntry> selist = TimingStats.parseFromBin(blStats, s, e);
//        List<LagEntry> lelist  = LagStats.parseFromBin(blLags, s, e);
//        String html = Config.getResourceAsString("chartStats.html", StandardCharsets.UTF_8);
//        Path dir;
//        if (Files.isDirectory(indexHtml)) {
//            dir = indexHtml;
//            indexHtml = dir.resolve("index.html");
//        } else {
//            dir = indexHtml.getParent();
//        }
//        //если не указан полный путь а только имя файла в текущем каталоге для indexHtml - выдаст null
//        Path jsDir = dir == null ? Paths.get("js") : dir.resolve("js");
//        if (!Files.exists(jsDir)) {
//            Files.createDirectories(jsDir);
//        }
//
//        //копирование индекс-файла из ресурсов в указанный каталог
//        if (!Files.exists(indexHtml) || Files.size(indexHtml) < html.length()) {
//            Files.write(indexHtml, html.getBytes(StandardCharsets.UTF_8));
//            try {
//                Files.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("facivon.ico"),dir.resolve("facivon.ico"));
//            }
//            catch (Exception ex0) {
//            }
//        }
//
//        String js = Config.getResourceAsString("js/chart.js", StandardCharsets.UTF_8);
//        if (js != null) {
//            int bi = js.indexOf("//#DATA_BEGIN");
//            int ei = js.indexOf("//#DATA_END");
//            StringBuilder data = new StringBuilder();
//            data.append(js, 0, bi);
//            final String replacedt = "$RDateTime";
//            int i = js.indexOf(replacedt);
//            String date = Utils.getFormatedTimeInterval(s, e);
//            data.replace(i, i + replacedt.length(), date);
//
//            //create chart-data
//            createJSGoogleChartData(selist, lelist, data);
//
//            data.append(js, ei, js.length());
//
//            return Files.write(jsDir.resolve("chart.js"), data.toString().getBytes(StandardCharsets.UTF_8));
//        }
//        return null;
//    }
//
//    /**
//     * Создание набора данных для JS Google Charts
//     * @param selist
//     * @param lelist
//     * @param sb
//     */
//    private static void createJSGoogleChartData(List<StatEntry> selist, List<LagEntry> lelist, StringBuilder sb) {
//        final String dac = "data.addColumn('";
//        sb.append("function fillDataStats(data){\n");
//
//        sb.append(dac).append("datetime','DateTime');\n");
////        sb.append(dac).append("number',  'Tps(x100)');\n");
//        sb.append(dac).append("number',  'Tps');\n");
//        sb.append(dac).append("number',  'UsedMem(Mb)');\n");
//        sb.append(dac).append("number',  'Online');\n");
//        sb.append(dac).append("number',  'Chunks');\n");
//        sb.append(dac).append("number',  'Entities');\n");
//        sb.append(dac).append("number',  'Tiles');\n");
//
//        sb.append("data.addRows([");
//        if (selist != null) {
//            for (int i = 0; i < selist.size(); i++) {
//                StatEntry se = selist.get(i);
//                sb.append("[new Date(").append(se.time).append("),");
//                 //sb.append((se.tps & 0xFF) / 10.0 ).append(","); //200 -> 20.0 2000 -> 20.00
//                 sb.append((se.tps & 0xFF)).append(","); //200 -> 20.0 2000 -> 20.00
//                 sb.append(se.memUsed).append(",");
//                 sb.append(se.online ).append(",");//"маштабирование"
//                 sb.append(se.chunks).append(",");
//                 sb.append(se.entities).append(",");
//                 sb.append(se.tiles).append("],");
//            }
//        }
//        sb.append("]);\n");
//        sb.append("}\n");
//
//        //---- lags ------
//        sb.append("function fillDataLags(data){\n");
//        sb.append(dac).append("datetime', 'DateTime');\n");
//        sb.append(dac).append("number',   'lags(ms)');\n");
//
//        sb.append("data.addRows([");
//        if (lelist != null) {
//            for (int i = 0; i < lelist.size(); i++) {
//                LagEntry e = lelist.get(i);
//                sb.append("[new Date(").append(e.time).append("),");
//                sb.append(e.lag).append("],");
//            }
//        }
//        sb.append("]);\n");
//        sb.append("}");
//    }
//

}
