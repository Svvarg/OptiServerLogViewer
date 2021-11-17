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
                    //определяем цвета самих линий графиков
                    Color.decode("0x32CD32"),//limegreen 	#32CD32 	rgb(50,205,50)
                    Color.decode("0xA00000"),//Red
                    Color.decode("0x00FFCC"),//Turquoise
                    Color.decode("0x87CEEB"),//skyblue 	#87CEEB
                    Color.decode("0xF4A460"),//sandybrown 	#F4A460 	rgb(244,164,96)
                    Color.decode("0x6A5ACD"),//slateblue 	#6A5ACD 	rgb(106,90,205)
                    Color.decode("0xFFFF7F"),
                    Color.decode("0x4D85A7"),//
                    Color.decode("0x6681CC"),
                    Color.decode("0xFF7F7F"),
                    Color.decode("0xFFFFBF"),
                    Color.decode("0x99A6CC"),
                    Color.decode("0xFFBFBF"),
                    Color.decode("0xA9A938"),
                    Color.decode("0xFF0000"),//
                    Color.decode("0x8B4513"),//saddlebrown 	#8B4513 	rgb(139,69,19)
                    //mediumseagreen 	#3CB371 	rgb(60,179,113)
                },
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
