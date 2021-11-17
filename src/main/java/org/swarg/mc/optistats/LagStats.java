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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import org.swarg.common.Binary;
import org.swarg.common.Strings;
import org.swarg.mc.optistats.model.LagEntry;

/**
 * 15-11-21
 * @author Swarg
 */
public class LagStats {

    /**
     * Построить лист значений входящих в требуемые временные рамки
     * из бинарного файла лога
     * @param inname бинарный лог файл
     * @param startStampTime временная начальная точка выборки
     * @param endStampTime
     * @return
     */
    public static List<LagEntry> parseFromBin(String inname, long startStampTime, long endStampTime) {
        try {
            List<LagEntry> list = new ArrayList<>();
            byte[] ba = Files.readAllBytes(Paths.get(inname));
            final int oesz = LagEntry.getSerializeSize();
            int cnt = ba.length / oesz; //8
            int off = 0;
            for (int i = 0; i < cnt; i++) {
                final long time = Binary.readLong(ba, off);
                if (!Utils.isTimeInRange(time, startStampTime, endStampTime )) {
                    off += oesz;//просто пропускаю если время не подходит
                    continue;
                }
                list.add(new LagEntry(ba, off));
                off += oesz;
            }
            return list;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Преобразовать бинарный лог в читаемое представление
     * @param inname
     * @param startStampTime
     * @param endStampTime
     * @param showMillis показывать и timeMillis
     * @return
     */
    public static Object getReadable(String inname, long startStampTime, long endStampTime, boolean showMillis) {
        List<LagEntry> list = parseFromBin(inname, startStampTime, endStampTime);
        if (list == null || list.isEmpty()) {
            return "Emtpty for " + inname;
        }
        else {
            final int sz = list.size();
            StringBuilder sb = new StringBuilder(sz * 116);

            for (int i = 0; i < sz; i++) {
                LagEntry le = list.get(i);
                le.appendTo(sb, i, showMillis).append('\n');
            }
            return sb;
        }
    }


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

        ChartFactory.setChartTheme(ChartThemes.createDarknessTheme());

        List<LagEntry> list = parseFromBin(inname, s, e);
        if (list.size() > 1) {
            XYDataset dataset = createDataset(list);

            final long start = list.get(0).time;
            final long end   = list.get(list.size()-1).time;
            final String date = Utils.getFormatedTimeInterval(start, end);

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
        return null;
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
    private static XYDataset createDataset(List<LagEntry> list) {
        if (list != null && list.size() > 1) {
            try {

                TimeSeriesCollection dataset = new TimeSeriesCollection();
                TimeSeries ts = createTimeSeries(list, "Lag", -1);//Lag
                
                //черта отображающая норму для наглядности.
                TimeSeries tsbaseline = new TimeSeries("Normal");//Baseline

                final int noLag = 50;
                tsbaseline.add(Utils.getSecondOfMillis(list.get(0).time), noLag);
                tsbaseline.add(Utils.getSecondOfMillis(list.get(list.size()-1).time), noLag);

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

    /**
     * Создать набор точек для наложения на некий другой график
     * max - 100%
     * lag -   x
     * @param max - максимальное значение к которому нужно смаштабировать значения
     * лагов (Для того, чтобы 
     * @return 
     */
    public static TimeSeries createTimeSeries(List<LagEntry> list, String name, int max) {
        if (list != null && list.size() > 0) {
            try {
               TimeSeries ts = new TimeSeries(name);//"Lag");
                final int sz = list.size();

                for (int i = 0; i < sz; i++) {
                    LagEntry le = list.get(i);
                    final long time = le.time;
                    //можно ли как-то создавать инстанс секунды без Instant?
                    Second sec = Utils.getSecondOfMillis(time);
                    ts.add(sec, le.lag);
                    /*Данные лагов имеют природу резких всплесков, а не графика
                    где точки между разными данными соеденены. поэтому добавляю
                    до и после значения привязку к baseline - 50 мс на один тик
                    Возможно это правильнее делать каким-то иным способом - но пока так*/
                    ts.add(sec.next(), 50);
                    ts.add(sec.previous(), 50);
                }
                //первый граффик будет иметь зелёный цвет задаётся в ChartThemes
                return ts;
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
