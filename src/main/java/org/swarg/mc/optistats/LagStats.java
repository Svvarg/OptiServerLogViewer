package org.swarg.mc.optistats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private static final DateTimeFormatter DF_DATETIME = DateTimeFormatter.ofPattern("dd.MM.yy  HH:mm:ss");
    private static final DateTimeFormatter DF_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

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

        long[] setime = new long[2];
        XYDataset dataset = parseLagFile2Dataset(in, s, e, setime);
        ChartTheme theme = ChartThemes.createDarknessTheme();
        ChartFactory.setChartTheme(theme);

        String date;
        final long start = setime[0];
        final long end = setime[1];
        ZoneId zid = ZoneOffset.systemDefault();
        ZonedDateTime zts = Instant.ofEpochMilli(start).atZone(zid);
        ZonedDateTime zte = Instant.ofEpochMilli(end).atZone(zid);
        date = zts.format(DF_DATETIME) + " - " +
                zte.format((zts.getDayOfYear() == zte.getDayOfYear() ? DF_TIME : DF_DATETIME));

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Server Tick Lags",
            date,       // X-Axis Label
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
     * @param showMillis показывать и timeMillis
     * @return
     */
    public static Object getLagReadable(String inname, long startStampTime, long endStampTime, boolean showMillis) {
        try {
            byte[] ba = Files.readAllBytes(Paths.get(inname));
            int cnt = ba.length / 10; //8 time 2 lagvalue
            int off = 0;
            StringBuilder sb = new StringBuilder();
            int j = 0;
            for (int i = 0; i < cnt; i++) {
                final long time = Binary.readLong(ba, off); off += 8;
                if (!Utils.isTimeInRange(time, startStampTime, endStampTime )) {
                    off += 2;//просто пропускаю не читая значение лага
                    continue;
                }
                final int lag = Binary.readUnsignedShort(ba, off); off += 2;
                String line;
                String fdt = Strings.formatDateTime(time);
                if (showMillis) {
                    line = String.format("#% ,3d  %s (%d)   % ,6d", j, fdt, time, lag);
                } else {
                    line = String.format("#% ,3d  %s   % ,6d", j, fdt, lag);
                }
                sb.append(line).append('\n');
                j++;
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
     * @param out 0 время первой точки 1 - последней
     * @return
     */
    private static XYDataset parseLagFile2Dataset(Path src, long startStampTime, long endStampTime, long[] out) {
        if (src != null && Files.exists(src)) {
            try {
                //прочесть весь файл целеком TODO часть через AsyncRandomAccess
                byte[] ba = Files.readAllBytes(src);
                int cnt = ba.length / 10; //8 time 2 lagvalue
                int off = 0;

                TimeSeriesCollection dataset = new TimeSeriesCollection();
                TimeSeries ts = new TimeSeries("Lag");
                //черта отображающая норму для наглядности.
                TimeSeries tsbaseline = new TimeSeries("Normal");//Baseline
                boolean first = true;
                Second sec = null;
                long lastPointTime = 0;
                ZoneId zid = ZoneOffset.systemDefault();

                for (int i = 0; i < cnt; i++) {
                    //data
                    final long time = Binary.readLong(ba, off); off += 8;

                    if (!Utils.isTimeInRange(time, startStampTime, endStampTime)) {
                        off += 2;//просто пропускаю не читая значение лага
                        continue;
                    }
                    final int lag = Binary.readUnsignedShort(ba, off); off += 2;

                    lastPointTime = time;
                    //можно ли как-то создавать инстанс секунды без Instant?
                    ZonedDateTime zt = Instant.ofEpochMilli(time).atZone(zid);//.toLocalDateTime()
                    int s = zt.getSecond();
                    int m = zt.getMinute();
                    int h = zt.getHour();
                    int d = zt.getDayOfMonth();
                    int mm= zt.getMonthValue();
                    int y = zt.getYear();
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
                        tsbaseline.add(sec.previous(), 50);
                        if (out != null && out.length > 0) {
                            out[0] = time;
                        }
                    }
                }
                //последняя точка для baseline - черта нормы
                tsbaseline.add(sec.next(), 50);
                //время последне
                if (out != null && out.length > 1) {
                    out[1] = lastPointTime;
                }

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
     * Генерация бинарного лога со случайными данными в обратном порядке - от
     * конца текущего дня и на случайное количество шагов в прошлое.
     * Генерация идёт в обратном порядке с конца выделенного массива к его началу
     * это сделано для того, чтобы эмулировать бинарный лог имеющий в себе записи
     * нескольких дней. Из которого в дальнейшем нужно будет взять только значения
     * для текущего дня.
     * @param eCount количество генерируемых записей если меньше 0 возьмёт 96
     * @param filename куда записать
     * @param canRewrite переписывать для существующего файла (чтобы не затереть
     * случайно реальный лог)
     * @param out
     * @param verbose
     * @return
     * @throws IOException
     */
    public static Path genRndLagFile(int eCount, String filename, boolean canRewrite, PrintStream out, boolean verbose) throws IOException {
        Path in = Paths.get(filename);
        Path dir = in.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
        if (Files.exists(in)) {
            out.print("Already Exists file: " + in);
            if (!canRewrite) {
                out.println(" Use opt [-w|--rewrite-exists]");
                return null;
            } else {
                out.println(" Will be changed");
            }
        }
        
        eCount = eCount <= 0 ? 96 : eCount;
        byte[] a = new byte[eCount*10];
        //для подсчёта количество записей сгенерированых для текущего дня
        long tStart = Utils.getStartTimeOfCurentDay();
        
        //точка начала генерации значений в обратном порядке - начало следующего дня
        long tEnd = Utils.getStartTimeOfNextDay();
        long t = tEnd;

        if (verbose) {
            out.println("Latest TimePoint: " + Strings.formatDateTime(t)+" ("+t+")" );
        }
        int off = eCount;
        Random rnd = new Random();
        int k = 0;
        int i = 0;//totalPointsвсего добавленых случайных временных точек логов
        int todayPoints = 0;// "сегодняшние лаги"
        //всего лагов и сегодняшние для проверки механики рисования графика на сегодня
        List<String> list = verbose ? new ArrayList<>() : null;

        while ( i < eCount) {
            k++;
            t -= k * (20_000 + rnd.nextInt(10_000));
            if (rnd.nextInt() % 2 == 0) {
                int lag = ((k+i) % 5 != 0) ? 50 + rnd.nextInt(2000) : 1000 + rnd.nextInt(10000);
                //Для добавления значений с конца массива. Начиная от конца текущего дня и вглубь прошлого
                off = (((eCount - i)-1) * 10);
                //Serialize to binlog
                Binary.writeLong(a, off, t );           off += 8;
                Binary.writeUnsignedShort(a, off, lag); off += 2;
                if (t > tStart ) {
                    todayPoints++;
                }
                if (verbose) {
                    //index : time
                    list.add(String.format("#% ,3d  %s (%d)   % ,6d",
                            eCount-i, Strings.formatDateTime(t), t, lag));
                }
                ++i;
            }
        }
        if (verbose) {
            Collections.reverse(list);
            for (String l : list) {
                out.println(l);
            }
        }
        //DEBUG
        out.println("TotalLagPoints: " + i + "  ToDayLagTimePoints: " + todayPoints);
        return Files.write(in, a);
    }


}
