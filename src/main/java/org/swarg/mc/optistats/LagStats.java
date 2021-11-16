package org.swarg.mc.optistats;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.swarg.common.Binary;
import org.swarg.common.Strings;

/**
 * 15-11-21
 * @author Swarg
 */
public class LagStats {
    private static final DateTimeFormatter DF_DATE = DateTimeFormatter.ofPattern("dd.MM.yy");

    /**
     *
     * @param inname  lag.log.bin
     * @param outname img.png
     * @param w  размер картинки
     * @param h
     * @param s startTime для возможности создавать график части данных
     * @param e endTime  в указанной границе времени от до
     * @throws IOException
     */
    public static File createChartImg(String inname, String outname, int w, int h, long s, long e) throws IOException {
        Path in = Paths.get(inname);
        if (!Files.exists(in)) {
            throw new IllegalStateException("No input file "+in);
        }
        String date = "";
        if (s > 0) {
            date = Instant.ofEpochMilli(s).atZone(ZoneOffset.systemDefault()).format(DF_DATE);
        }

        XYDataset dataset = parseLagFile2Dataset(in, s, e);
        ChartTheme theme = ChartThemes.createDarknessTheme();
        ChartFactory.setChartTheme(theme);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Server Tick Lags " + date,
            "DateTime", // X-Axis Label
            "Lag (ms)", // Y-Axis Label
            dataset);

        File out = new File(outname);
        File dir = out.getParentFile();
        if (dir != null) {
            dir.mkdirs();
        }

        //chart.getPlot().setsetBackgroundPaint( Color.BLACK);

        ChartUtils.saveChartAsPNG(out, chart, w, h);
        return out.getAbsoluteFile();
    }

    /**
     * Преобразовать бинарный лог в читаемое представление
     * @param inname
     * @param startStampTime
     * @param endStampTime
     * @return
     */
    public static Object getLagReadable(String inname, long startStampTime, long endStampTime) {
        try {
            byte[] ba = Files.readAllBytes(Paths.get(inname));
            int cnt = ba.length / 10; //8 time 2 lagvalue
            int off = 0;
            boolean hasTimeRange = startStampTime > 0 && startStampTime < endStampTime;
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < cnt; i++) {
                final long time = Binary.readLong(ba, off); off += 8;
                final int lag = Binary.readUnsignedShort(ba, off); off += 2;
                if (hasTimeRange && (time < startStampTime || time > endStampTime )) {
                    continue;
                }
                sb.append(Strings.formatDateTime(time)).append("  ").append(lag).append('\n');
            }
            return sb;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Читает весь файл и на основе временных рамок от startStampTime до
     * endStampTime создаёт набор данных для отображения на графике
     *
     * TODO научить читать с конфа файла порциями, для эффективной работы с
     * большими файлами (чтобы не помещать в память весь файл) Utils.readBinBytes
     * @param src
     * @param startStampTime
     * @param endStampTime
     * @return
     */
    private static XYDataset parseLagFile2Dataset(Path src, long startStampTime, long endStampTime) {
        if (src != null && Files.exists(src)) {
            try {
                //прочесть весь файл целеком TODO часть через AsyncRandomAccess
                byte[] ba = Files.readAllBytes(src);
                int cnt = ba.length / 10; //8 time 2 lagvalue
                int off = 0;
                boolean hasTimeRange = startStampTime > 0 && startStampTime < endStampTime;

                TimeSeriesCollection dataset = new TimeSeriesCollection();
                TimeSeries ts = new TimeSeries("Lag");
                //черта отображающая норму для наглядности.
                TimeSeries tsbaseline = new TimeSeries("Baseline");
                boolean first = true;
                Second sec = null;

                for (int i = 0; i < cnt; i++) {
                    //data
                    final long time = Binary.readLong(ba, off); off += 8;
                    final int lag = Binary.readUnsignedShort(ba, off); off += 2;

                    if (hasTimeRange && (time < startStampTime || time > endStampTime )) {
                        continue;
                    }
                    LocalDateTime t = Instant.ofEpochMilli(time).atZone(ZoneOffset.systemDefault()).toLocalDateTime();
                    int s = t.getSecond();
                    int m = t.getMinute();
                    int h = t.getHour();
                    int d = t.getDayOfMonth();
                    int mm= t.getMonthValue();
                    int y = t.getYear();
                    sec = new Second(s, m, h, d, mm, y);
                    ts.add(sec, lag);
                    /*Данные лагов имеют природу резких всплесков, а не графика
                    где точки между разными данными соеденены. поэтому добавляю
                    до и после значения привязку к baseline - 50 мс на один тик */
                    ts.add(sec.next(), 50);
                    ts.add(sec.previous(), 50);

                    //draw baseline 2 point
                    if (first) {
                        first = false;
                        tsbaseline.add(sec, 50);
                    }
                }
                //последняя точка для baseline - черта нормы
                tsbaseline.add(sec, 50);

                //первый граффик будет иметь зелёный цвет задаётся в ChartThemes
                dataset.addSeries(tsbaseline);
                dataset.addSeries(ts);
                return dataset;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }



    // ============================== DEBUG ===============================  \\

    /**
     * [DEBUG]
     * Для отладки - создать файл со случайными данными
     * @param filename
     * @return
     * @throws IOException
     */
    public static Path genRndLagFile(String filename) throws IOException {
        Path in = Paths.get(filename);
        Path dir = in.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
        
        int cap = 128;
        byte[] a = new byte[cap*10];
        long now = System.currentTimeMillis();
        //long nextDay = now + 60000*60*24;
        long t = now - 60000*60*48;// -TwoDays from Now
        int off = 0;
        Random rnd = new Random();
        int i = 0;
        int totalPoints = 0;//всего добавленых случайных временных точек логов
        int todayPoints = 0;// "сегодняшние лаги"
        //всего лагов и сегодняшние для проверки механики рисования графика на сегодня

        while ( totalPoints < cap) {
            i++;
            //t += i * (60_000 + rnd.nextInt(60_000)) * (1 + rnd.nextInt(2));
            t += i * (60_000 + rnd.nextInt(60_000));
            if (rnd.nextInt() % 2 == 0) {
                Binary.writeLong(a, off, t ); off+=8;
                int lag = ((i+totalPoints) % 5 != 0) ? 50 + rnd.nextInt(2000) : 1000 + rnd.nextInt(10000);
                Binary.writeUnsignedShort(a, off, lag); off += 2;
                ++totalPoints;
                if (t > now ) {
                    todayPoints++;
                }
            }
        }
        //DEBUG
        System.out.println("TotalLagPoints: "+totalPoints+"  ToDayLagTimePoints: " + todayPoints);
        return Files.write(in, a);
    }


}
