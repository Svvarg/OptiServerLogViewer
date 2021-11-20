package org.swarg.mc.optistats.jfreechart;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import org.swarg.mc.optistats.ChartThemes;
import org.swarg.mc.optistats.Utils;
import org.swarg.mcforge.statistic.LagEntry;
import static org.swarg.mc.optistats.LagStats.parseFromBin;

/**
 * 18-11-21
 * @author Swarg
 */
public class LagStatsJFC {

    /**
     * Для JFreeChart из указанного времени получить инстанс Second
     * @param time
     * @return
     */
    public static Second getSecondOfMillis(long time) {
        //можно ли как-то создавать инстанс секунды без Instant?
        ZonedDateTime zt = Instant.ofEpochMilli(time).atZone(ZoneOffset.systemDefault());
        int s = zt.getSecond();
        int m = zt.getMinute();
        int h = zt.getHour();
        int d = zt.getDayOfMonth();
        int mm= zt.getMonthValue();
        int y = zt.getYear();
        return new Second(s, m, h, d, mm, y);
    }

    /**
     *
     * @param in  path to lag.log.bin
     * @param out img.png
     * @param w  размер картинки
     * @param h
     * @param s startTime для возможности создавать график части данных
     * @param e endTime  в указанной границе времени от до
     * @throws IOException
     */
    public static File createChartImg(Path in, Path out, int w, int h, long s, long e) throws IOException {
        if (!Files.exists(in)) {
            throw new FileNotFoundException("No input file " + in.toAbsolutePath());
        }

        ChartFactory.setChartTheme(ChartThemes.createDarknessTheme());

        List<LagEntry> list = parseFromBin(in, s, e);
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

            File out0 = out.toFile();
            File dir = out0.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }

            //chart.getPlot().setsetBackgroundPaint( Color.BLACK);

            ChartUtils.saveChartAsPNG(out0, chart, w, h);
            return out0.getAbsoluteFile();
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
                tsbaseline.add(getSecondOfMillis(list.get(0).time), noLag);
                tsbaseline.add(getSecondOfMillis(list.get(list.size()-1).time), noLag);

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
                    Second sec = getSecondOfMillis(time);
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

}
