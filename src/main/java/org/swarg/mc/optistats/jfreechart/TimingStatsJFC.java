package org.swarg.mc.optistats.jfreechart;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.swarg.mc.optistats.Utils;
import org.swarg.mc.optistats.ChartThemes;
import org.swarg.mcforge.statistic.StatEntry;
import static org.swarg.mc.optistats.TimingStats.parseFromBin;

/**
 * 18-11-21
 * @author Swarg
 */
public class TimingStatsJFC {

    /**
     *
     * @param in путь к бинарному логу, хранящему значения 
     * @param out куда сохранять картинку
     * @param w
     * @param h
     * @param s startStampTime
     * @param e endStampTime
     * @param ts (TimeSeries) дополнительный график добавляется если инстанс TimeSeries
     * @return
     * @throws IOException
     */
    public static File createChartImg(Path in, Path out, int w, int h, long s, long e, Object ts) throws IOException {
        if (!Files.exists(in)) {
            throw new FileNotFoundException("No input file " + in.toAbsolutePath());
        }

        ChartFactory.setChartTheme(ChartThemes.createDarknessTheme());

        List<StatEntry> list = parseFromBin(in, s, e);
        if (list.size() > 1) {
            XYDataset dataset = createDataset(list);
            if (ts instanceof TimeSeries) {
                ((TimeSeriesCollection)dataset).addSeries((TimeSeries)ts);
            }

            final long start = list.get(0).time;
            final long end = list.get(list.size()-1).time;
            final String date = Utils.getFormatedTimeInterval(start, end);

            JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Server Stats",
                date,       //X-Axis Label
                "Values",   //Y-Axis Label
                dataset);

            File out0 = out.toFile();
            File dir = out0.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            ChartUtils.saveChartAsPNG(out0, chart, w, h);
            return out0.getAbsoluteFile();
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

                Second sec = LagStatsJFC.getSecondOfMillis(se.time);
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
