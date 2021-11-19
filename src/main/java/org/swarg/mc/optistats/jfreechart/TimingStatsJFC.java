package org.swarg.mc.optistats.jfreechart;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.Second;
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
     * Создание графика в виде png изображения. С разделением на две части
     * online&tps отдельно от счётчиков (Для лучшей визуализации и маштабирования)
     * @param in путь к бинарному логу, хранящему значения 
     * @param out куда сохранять картинку
     * @param w ширина изображения
     * @param h высота
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
            XYDataset[] datas = createDataset(list);
            if (datas == null) {
                throw new IllegalStateException("Empty datasets for " + in);
            }
            
            final long start = list.get(0).time;
            final long end = list.get(list.size()-1).time;
            final String date = Utils.getFormatedTimeInterval(start, end);

            JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Server Stats",
                date,          //X-Axis Label
                "Online & TPS",//Y-Axis Label
                datas[0]);

            //Second vAxis for all data exclude Online And TPS
            addDataSetToNewAxis(chart, datas[1], "Values", 1, 1);
            boolean hasLagsSeries = false;
            if (ts instanceof TimeSeries) {
                final TimeSeriesCollection dataset = new TimeSeriesCollection();
                dataset.addSeries((TimeSeries)ts);
                String name = ((String)((TimeSeries)ts).getKey());
                addDataSetToNewAxis(chart, dataset, name, 2, 2);
                hasLagsSeries = true;
                //((TimeSeriesCollection)datas[0]).addSeries((TimeSeries)ts);
            }

            LegendTitle legend = chart.getLegend();
            legend.setPosition(RectangleEdge.TOP);
            //apply colors to Second vAxis
            ChartUtils.applyCurrentTheme(chart);
            
            //Fix Colors for multiple seriesGraths
            //если не сделать этого, то цвета для каждого из отдельных графиков
            //будут браться с 0го индекса! (избежание перемешивания цветов)
            final XYPlot plot = (XYPlot)chart.getPlot();

            final XYLineAndShapeRenderer r1 = new XYLineAndShapeRenderer();
            r1.setSeriesPaint(0, ChartThemes.COLORS[2]);//memused
            r1.setSeriesPaint(1, ChartThemes.COLORS[3]);//chuncks
            r1.setSeriesPaint(2, ChartThemes.COLORS[4]);//entities
            r1.setSeriesPaint(3, ChartThemes.COLORS[5]);//tiles
            r1.setDefaultShapesVisible(false);
            plot.setRenderer(1, r1);

            //Исправление цвета Для доп графика(Lags)
            if (hasLagsSeries) {
                final XYLineAndShapeRenderer r2 = new XYLineAndShapeRenderer();
                r2.setSeriesPaint(0, ChartThemes.COLORS[6]);//RED lags
                r2.setDefaultShapesVisible(false);
                plot.setRenderer(2, r2);
            }

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

    /**
     * Создать на основе данных два Dataset для отдельного маштабирования
     * tps + online от счётчиков
     * @param list
     * @return
     */
    private static XYDataset[] createDataset(List<StatEntry> list) {
        try {
            TimeSeriesCollection dataset = new TimeSeriesCollection();
            TimeSeriesCollection dataset2 = new TimeSeriesCollection();
            TimeSeries tsTps     = new TimeSeries("tps");
            TimeSeries tsOnline  = new TimeSeries("Online");
            TimeSeries tsMemUsed = new TimeSeries("UsedMem(Mb)");
            TimeSeries tsChunks  = new TimeSeries("Chunks");
            TimeSeries tsEntities= new TimeSeries("Entities");
            TimeSeries tsTiles   = new TimeSeries("Tiles");

            for (int i = 0; i < list.size(); i++) {
                StatEntry se = list.get(i);

                Second sec = LagStatsJFC.getSecondOfMillis(se.time);
                tsMemUsed.add(sec, se.memUsed);
                tsTps.    add(sec, (se.tps & 0xFF) / 10.0 );
                tsOnline. add(sec, se.online);

                tsChunks. add(sec, se.chunks);
                tsTiles.  add(sec, se.tiles);
                tsEntities.add(sec,se.entities);
            }

            dataset2.addSeries(tsTps);//Green
            dataset2.addSeries(tsOnline);
            dataset.addSeries(tsMemUsed);
            dataset.addSeries(tsChunks);
            dataset.addSeries(tsEntities);
            dataset.addSeries(tsTiles);
            return new XYDataset[]{dataset2, dataset};
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Создание дополнительной оси измерений по шкале y
     * Для отдельного маштабирования разного сорта значений
     * @param chart
     * @param dataset набор значений
     * @param label названия оси
     * @param index 1+
     * @param axisindex
     */
    public static void addDataSetToNewAxis(JFreeChart chart, XYDataset dataset, String label, int index, int axisindex) {
        final XYPlot plot = (XYPlot)chart.getPlot();
        final NumberAxis axis = new NumberAxis(label);
        axis.setAutoRangeIncludesZero(false);//?
        plot.setRangeAxis(index, axis);
        plot.setDataset(index, dataset);
        plot.mapDatasetToRangeAxis(index, axisindex);
    }

}
