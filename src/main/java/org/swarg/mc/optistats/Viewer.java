package org.swarg.mc.optistats;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import org.swarg.cmds.ArgsWrapper;

/**
 * 15-11-21
 * @author Swarg
 */
public class Viewer {

    Config config;

    public Viewer(String conffile) {
        this.config = new Config(conffile).reload();
    }

    private static final String USAGE = "<help/config/lags> [--config path]";
    //если нужно указать конкретный путь к конфигу --config path/to/cnfg.properties

    public static void main(String[] args) throws IOException {

        ArgsWrapper w = new ArgsWrapper(args);
        Viewer v = new Viewer(w.optValue("--config", "-c"));
        Object ans = null;
        //help
        if (w.isHelpCmdOrNoArgs()) {
            System.out.println(USAGE);
            System.exit(0);
        }

        //показать полный путь к конфигу
        if (w.isCmd("config")) {
            ans = v.config.configFile.toAbsolutePath();
        }
        //debug-gen binlog-file
        else if (w.isCmd("lags","l")) {
            ans = cmdLags(w, ans, v);
        }
        else {
            ans = "UNKNOWN cmd: "+ w.arg(w.ai) ;
        }

        System.out.println(ans);
    }

    private static final String LAGS_USAGE =
              "img [-in (file-log.bin)] [-out (img.png)] [--weight X --height Y] [--start-time L1] [--end-time L2 Def-Now]\n"
            + "view [-in (file-log.bin)] [--start-time L1] [--end-time L2 Def-Now]\n"
            + "generate  - debug tool\n";

    private static Object cmdLags(ArgsWrapper w, Object ans, Viewer v) throws IOException {
        if (w.isHelpCmdOrNoArgs()) {
            return LAGS_USAGE;
        }
        //где лежит бинарный лог с данными
        String defInLags = v.getProp("inLags", "lag.log.bin");
        String inname = w.optValueOrDef(defInLags, "-in");

        //временное ограничение (на данный момент все данные собираются в один файл)
        long s = w.optValueLongOrDef(getStartTimeOfCurentDay(), "-s", "--start-time"); //TODO начало текущего дня
        long e = w.optValueLongOrDef(System.currentTimeMillis(), "-e", "--end-time");


        if (w.isCmd("generate", "g")) {
            ans = LagStats.genRndLagFile(inname);
        }
        //bin-log to text
        else if (w.isCmd("view", "v")) {
            /*DEBUG*/Path p = Paths.get(".");
            /*DEBUG*/System.out.println(inname+"\n"+p.toAbsolutePath());
            ans = LagStats.getLagReadable(inname, s, e);
        }
        //create-lag-image
        else if (w.isCmd("img", "i")) {
            //куда ложить созданную картинку
            String defOutLagsImp = v.getProp("outLagsImg", "lag.png");
            String png = w.optValueOrDef(defOutLagsImp, "-out");

            int weight = (int) w.optValueLongOrDef(v.getPropI("lagChartWeight", 960), "-w", "--weight");
            int height = (int) w.optValueLongOrDef(v.getPropI("lagChartHeight", 400), "-h", "--height");

            ans = LagStats.createChartImg(inname, png, weight, height, s, e);
        }
        return ans;
    }


    // ------------------  Config Util ----------------------------------- \\


    public String getProp(String name, String def) {
        if (this.config.props == null) {
            throw new IllegalStateException("No Props!");
        }
        String v = this.config.props.getProperty(name);
        return (v == null || v.isEmpty()) ? def : v;
    }

    public int getPropI(String name, int def) {
        if (this.config.props == null) {
            throw new IllegalStateException("No Props!");
        }
        String v = this.config.props.getProperty(name);

        try {
            return Integer.parseInt(v);
        }
        catch (Exception e) {
            return def;
        }
    }


    public static long getStartTimeOfCurentDay() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
