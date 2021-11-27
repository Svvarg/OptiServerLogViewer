package org.swarg.mc.optistats;

import java.util.List;
import java.util.Objects;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import org.swarg.mcforge.statistic.CleanupEntry;
import org.swarg.mcforge.statistic.TShEntry;
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

        Config.copyFromResource("chartStats.html", indexHtml, canReplace, out);
        Config.copyFromResource("js/chart.js", jsDir.resolve("chart.js"), canReplace, out);
        Config.copyFromResource("favicon.ico", dir.resolve("favicon.ico"), canReplace, out);
        return true;
    }

    /**
     * создание stats.bin lags.bin для js-скрипта подтягивающего данные для
     * рендеринга графиков
     * @param blStats где лежат бинарные логи собираемой статистики
     * @param blLags  выявленных лагов
     * @param blCleanups прошедших очисток
     * @param indexHtml корень html куда ложить создаваемые для запроса через js файлы
     * @param s
     * @param e
     * @param out
     * @return
     */
    public static boolean createRawDataForJSChart(Path blStats, Path blLags, Path blCleanups, Path indexHtml, long s, long e, PrintStream out) throws IOException {
        List<StatEntry> selist = TimingStats.parseFromBin(blStats, s, e);
        List<TShEntry> lelist  = LagStats.parseFromBin(blLags, s, e);
        List<CleanupEntry> celist  = CleanupStats.parseFromBin(blCleanups, s, e);
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
        Path p1 = dir.resolve("stats.txt");
        Path p2 = dir.resolve("lags.txt");
        Path p3 = dir.resolve("cleanups.txt");

        p1 = Files.write(p1, getRawStatsData(selist, sb).toString().getBytes(StandardCharsets.UTF_8));
        p2 = Files.write(p2, getRawLagsData(lelist, sb).toString().getBytes(StandardCharsets.UTF_8));
        p3 = Files.write(p3, getRawCleanupsData(celist, sb).toString().getBytes(StandardCharsets.UTF_8));
        if (out != null) {
            out.println("[WRITE]: " + p1 + " " + getListSize(selist));
            out.println("[WRITE]: " + p2 + " " + getListSize(lelist));
            out.println("[WRITE]: " + p3 + " " + getListSize(celist));
        }
        return true;
    }

    public static int getListSize(List l) {
        return l == null ? -1 : l.size();
    }

    /**
     * Создание набора данных для JS Google Charts
     * @param selist
     * @param lelist
     * @param sb
     */
    //private static void binaryToRawStatsData(List<StatEntry> selist, List<TShEntry> lelist, StringBuilder sb) {
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

    private static StringBuilder getRawLagsData(List<TShEntry> lelist, StringBuilder sb) {
        sb.setLength(0);
        sb.append("datetime:DateTime number:Lags\n");
        if (lelist != null) {
            for (int i = 0; i < lelist.size(); i++) {
                TShEntry e = lelist.get(i);
                sb.append(e.time).append(' ');
                sb.append(e.value).append('\n');//lag
            }
        }
        return sb;
    }

    /**
     * Для очисток в графиках пока будет использоваться только время инициализации
     * и сколько длилась сама очистка.
     * Для простого отображения когда были очистки (Для Сверки с лагами)
     * @param celist
     * @param sb
     * @return
     */
    private static Object getRawCleanupsData(List<CleanupEntry> celist, StringBuilder sb) {
        sb.setLength(0);
        sb.append("datetime:DateTime number:Cleanup\n");
        if (celist != null) {
            for (int i = 0; i < celist.size(); i++) {
                CleanupEntry e = celist.get(i);
                sb.append(e.time).append(' ').append(e.tookMs).append('\n');
            }
        }
        return sb;
    }

}
