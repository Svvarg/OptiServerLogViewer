package org.swarg.mc.optistats;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.DefaultDrawingSupplier;

/**
 * 16-11-21
 * @author Swarg
 */
public class ChartThemes {

    private static final Color C_BG0 = new Color(32, 34, 37);
    private static final Color C_BG1 = new Color(47,49,54);
    private static final Color C_BG2 = new Color(54,57,63);
    private static final Color C_W   = new Color(255, 243, 205);
    private static final Color C_T1  = new Color(79,84,92);

    public static org.jfree.chart.ChartTheme createDarknessTheme() {
        StandardChartTheme theme = new StandardChartTheme("Darkness");
        theme.setTitlePaint(C_W);
        theme.setSubtitlePaint(C_W);
        theme.setLegendBackgroundPaint(C_BG1);
        theme.setLegendItemPaint(C_W);
        theme.setChartBackgroundPaint(C_BG0);
        theme.setPlotBackgroundPaint(C_BG1);
        theme.setPlotOutlinePaint(C_T1);//Color.DARK_GRAY);//обводка вокруг площади графика yellow
        theme.setBaselinePaint(C_W);
        theme.setCrosshairPaint(Color.RED);
        theme.setLabelLinkPaint(Color.LIGHT_GRAY);//--
        theme.setTickLabelPaint(C_W);
        theme.setAxisLabelPaint(C_W);
        theme.setShadowPaint(Color.DARK_GRAY);
        theme.setItemLabelPaint(C_W);
        //отображение графика
        theme.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] {
                        Color.decode("0xA00000"),//Color.decode("0xFFFF00"),
                        Color.decode("0x0036CC"), Color.decode("0xFF0000"),
                        Color.decode("0xFFFF7F"), Color.decode("0x6681CC"),
                        Color.decode("0xFF7F7F"), Color.decode("0xFFFFBF"),
                        Color.decode("0x99A6CC"), Color.decode("0xFFBFBF"),
                        Color.decode("0xA9A938"), Color.decode("0x2D4587")},
                new Paint[] {Color.decode("0xFFFF00"),
                        Color.decode("0x0036CC")},
                new Stroke[] {new BasicStroke(2.0f)},
                new Stroke[] {new BasicStroke(0.5f)},
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE
               ));
        theme.setErrorIndicatorPaint(Color.LIGHT_GRAY);
        //theme.setGridBandPaint(new Color(255, 255, 255, 20));
        theme.setGridBandPaint(new Color(1, 1, 1, 20));
        theme.setGridBandAlternatePaint(new Color(255, 255, 255, 40));
        //theme.setShadowGenerator(null);
        return theme;
    }

}
