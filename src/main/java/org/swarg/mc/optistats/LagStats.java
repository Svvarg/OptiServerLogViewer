package org.swarg.mc.optistats;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Random;
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
        XYDataset dataset = parseLagFile(in, s, e);
        ChartTheme theme = ChartThemes.createDarknessTheme();
        ChartFactory.setChartTheme(theme);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Server Tick Lags",
            "DateTime", // X-Axis Label
            "Lag (ms)",   // Y-Axis Label
            dataset);
        File out = new File(outname);

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


    private static XYDataset parseLagFile(Path src, long startStampTime, long endStampTime) {
        if (src != null && Files.exists(src)) {
            try {
                byte[] ba = Files.readAllBytes(src);
                int cnt = ba.length / 10; //8 time 2 lagvalue
                int off = 0;
                boolean hasTimeRange = startStampTime > 0 && startStampTime < endStampTime;

                TimeSeriesCollection dataset = new TimeSeriesCollection();
                TimeSeries ts = new TimeSeries("Lag");//day?

                for (int i = 0; i < cnt; i++) {
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
                    ts.add(new Second(s, m, h, d, mm, y), lag);
                }
                //return cnt;//сколько записей было загружено
                dataset.addSeries(ts);
                return dataset;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * [DEBUG]
     * Для отладки - создать файл со случайными данными
     * @param filename
     * @return
     * @throws IOException
     */
    public static Path genRndLagFile(String filename) throws IOException {
        Path in = Paths.get(filename);
        int cap = 128;
        byte[] a = new byte[cap*10];
        long t = System.currentTimeMillis();
        int off = 0;
        Random rnd = new Random();
        int i = 0, j = 0;
        //for (int i = 0; i < cap; i++) {
        while ( j < cap) {
            i++;
            long time = t + i * 60_000 + rnd.nextInt(60_000);
            if (rnd.nextInt() % 8 == 0) {
                Binary.writeLong(a, off, time ); off+=8;
                int lag = ((i+j) % 5 != 0) ? rnd.nextInt(2000) : rnd.nextInt(32000);
                Binary.writeUnsignedShort(a, off, lag); off += 2;
                ++j;
            }
        }
        return Files.write(in, a);
    }


}
