package org.swarg.mc.optistats;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.swarg.common.Binary;
import org.swarg.mcforge.statistic.StatEntry;

/**
 * Создание графика и просмотр содержимого бинарного лога timingStat Лога
 * модель и как сериализуется описано в mcfbackendlib
 * org.swarg.mcforge.statistic.StatEntry
 *
 * The normal way of generating HTML for a web response is to use a JSP or a
 * Java templating engine like Velocity or FreeMarker.
 * 17-11-21
 * @author Swarg
 */
public class TimingStats {

    public static List<StatEntry> parseFromBin(String inname, long startStampTime, long endStampTime) {
        try {
            List<StatEntry> list = new ArrayList<>();
            byte[] ba = Files.readAllBytes(Paths.get(inname));
            final int oesz = StatEntry.getSerializeSize();
            int cnt = ba.length / oesz; //27
            int off = 0;
            for (int i = 0; i < cnt; i++) {
                final long time = Binary.readLong(ba, off);
                if (!Utils.isTimeInRange(time, startStampTime, endStampTime )) {
                    off += oesz;//просто пропускаю если время не подходит
                    continue;
                }
                list.add(new StatEntry(ba, off));
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
     * Для просмотра бинарного лога в простом текстовом виде в заданной временной
     * области
     * @param inname файл бинарного лога
     * @param startStampTime начало от когото нужно выводить (0 - с самого начала)
     * @param endStampTime значение timeMillis до которого делать выборку значений
     * @return
     */
    public static Object getReadable(String inname, long startStampTime, long endStampTime) {
        List<StatEntry> list = parseFromBin(inname, startStampTime, endStampTime);
        if (list == null || list.isEmpty()) {
            return "Emtpty for " + inname;
        } else {
            final int sz = list.size();
            StringBuilder sb = new StringBuilder(sz * 116);

            for (int i = 0; i < sz; i++) {
                StatEntry se = list.get(i);
                se.appendTo(sb).append('\n');
            }
            return sb;
        }
    }

    /**
     *
     * @param inname
     * @param outname
     * @param w
     * @param h
     * @param s startStampTime
     * @param e endStampTime
     * @param ts дополнительный график добавляется если не null
     * @return
     * @throws IOException
     */
    public static File createChartImg(String inname, String outname, int w, int h, long s, long e, TimeSeries ts) throws IOException {
        Path in = Paths.get(inname);
        if (!Files.exists(in)) {
            throw new IllegalStateException("No input file "+in);
        }

        ChartFactory.setChartTheme(ChartThemes.createDarknessTheme());
        
        List<StatEntry> list = parseFromBin(inname, s, e);
        if (list.size() > 1) {
            XYDataset dataset = createDataset(list);
            if (ts != null) {
                ((TimeSeriesCollection)dataset).addSeries(ts);
            }

            final long start = list.get(0).time;
            final long end = list.get(list.size()-1).time;
            final String date = Utils.getFormatedTimeInterval(start, end);

            JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Server Stats",
                date,       //X-Axis Label
                "Values",   //Y-Axis Label
                dataset);

            File out = new File(outname);
            File dir = out.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            ChartUtils.saveChartAsPNG(out, chart, w, h);
            return out.getAbsoluteFile();
        }
        return null;
    }


    private static XYDataset createDataset(List<StatEntry> list) {
        try {
            TimeSeriesCollection dataset = new TimeSeriesCollection();
            TimeSeries tsTps     = new TimeSeries("tps(1k=20)");
            TimeSeries tsMemUsed = new TimeSeries("UsedMem(Mb)");
            TimeSeries tsOnline  = new TimeSeries("Online(100=1)");
            TimeSeries tsChunks  = new TimeSeries("Chunks");
            TimeSeries tsEntities= new TimeSeries("Entities");
            TimeSeries tsTiles   = new TimeSeries("Tiles");

            for (int i = 0; i < list.size(); i++) {
                StatEntry se = list.get(i);

                Second sec = Utils.getSecondOfMillis(se.time);
                tsMemUsed.add(sec, se.memUsed);
                //"Масштабирую" Значения для лучшей визуалиазции значений на графике
                tsTps.    add(sec, (se.tps & 0xFF) * 5 ); //200 -> 20.0  1000 - 20.0
                tsOnline. add(sec, se.online * 100);   // 1 -> 100,  10 -> 1000

                tsChunks. add(sec, se.chunks);
                tsTiles.  add(sec, se.tiles);
                tsEntities.add(sec,se.entities);
            }
            
            dataset.addSeries(tsTps);//Green
            dataset.addSeries(tsOnline);
            dataset.addSeries(tsChunks);
            dataset.addSeries(tsEntities);
            dataset.addSeries(tsTiles);
            dataset.addSeries(tsMemUsed);
            return dataset;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
