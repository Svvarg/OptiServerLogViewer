package org.swarg.mc.optistats;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import org.swarg.cmds.ArgsWrapper;

/**
 * 15-11-21
 * @author Swarg
 */
public class Viewer {

    protected ArgsWrapper w;
    protected Config config;

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        new Viewer(args)
                .init()
                .performRequest();
    }


    public Viewer(String[] args) {
        w = new ArgsWrapper(args);
    }

    /**
     * Инициация конфига. Для получения в дальнейшем дефолтных значений путей.
     * @return
     */
    public Viewer init() {
        getConfig();
        return this;
    }

    private static final String USAGE = "<help/config/lags> [--config path]";
    //если нужно указать конкретный путь к конфигу --config path/to/cnfg.properties

    /**
     * Выполнение заданной команды
     */
    public void performRequest() {
        Object ans = null;
        try {
            if (w.isHelpCmdOrNoArgs()) {
                ans = USAGE;
            }
            //показать полный путь к конфигу
            else if (w.isCmd("config", "c")) {
                ans = showConfigInfo(w);
            }
            //debug-gen binlog-file
            else if (w.isCmd("lags", "l")) {
                ans = cmdLags();
            }
            else {
                ans = "UNKNOWN cmd: "+ w.arg(w.ai) ;
            }
        }
        catch (Throwable t) {
            t.printStackTrace(getOut());//TODO log
        }

        getOut().println(ans);
    }


    private static final String CONFIG_USAGE =
            "[show-props]";
    /**
     * Данные о конфиге и текущей рабочей директории (для отладки)
     * @param w
     * @return
     */
    private Object showConfigInfo(ArgsWrapper w) {
        if (w.isHelpCmd()) {
            return CONFIG_USAGE;
        }
        StringBuilder sb = new StringBuilder();
        //текущий рабочий каталог в котором запущено приложение
        sb.append("pwd: ").append(Paths.get(".").toAbsolutePath()).append('\n');
        //откуда был взят или где должен лежать конфиг
        sb.append("cnf: ").append(config.configFile.toAbsolutePath()).append('\n');
        //опционально все свойства конфига
        if (w.isCmd("show-props", "sp")) {
            sb.append("# Config\n");
            for(Object k : config.props.keySet()) {
                sb.append(k).append('=').append(config.props.get(k)).append('\n');
            }
        }
        return sb;
    }


    private static final String LAGS_USAGE =
              "img  [-in (file-log.bin)] [-out (img.png)] [-w|--weight X] [-h|--height Y] --start-time L1 --end-time L2\n"
            + "view [-in (file-log.bin)] [--start-time L1] [--end-time L2] [--show-millis]\n"
            + "[NOTE]: Defaults for -in & -out take from config; Default [-s|--start-time] - StartOfCurrentDay, for [-e|--end-time] - CurrentTime\n"
            + "generate [-out ($Config.inLags)] [-w|--rewrite-exists] [--count N Def:96] [--verbose] - [DEBUG] Generate Random Binary LogFile \n";

    private Object cmdLags() throws IOException {
        if (w.isHelpCmdOrNoArgs()) {
            return LAGS_USAGE;
        }
        Object ans = "UNKNOWN";
        //где лежит бинарный лог с данными
        String defInLags = getProp("inLags", "lag.log.bin");
        String inname = w.optValueOrDef(defInLags, "-in");

        //временное ограничение (на данный момент все данные собираются в один файл)
        long s = w.optValueLongOrDef(Utils.getStartTimeOfCurentDay(), "-s", "--start-time"); //TODO начало текущего дня
        long e = w.optValueLongOrDef(System.currentTimeMillis(), "-e", "--end-time");


        //create-lag-image
        if (w.isCmd("img", "i")) {
            //куда ложить созданную картинку
            String defOutLagsImp = getProp("outLagsImg", "lag.png");
            String png = w.optValueOrDef(defOutLagsImp, "-out");

            int weight = (int) w.optValueLongOrDef(getPropI("lagChartWeight", 960), "-w", "--weight");
            int height = (int) w.optValueLongOrDef(getPropI("lagChartHeight", 400), "-h", "--height");

            ans = LagStats.createChartImg(inname, png, weight, height, s, e);
        }

        //bin-log to text
        else if (w.isCmd("view", "v")) {
            boolean showMillis = w.hasOpt("-sm", "--show-millis");
            ans = LagStats.getLagReadable(inname, s, e, showMillis);
        }

        //DEBUG Генерация случайного бинарного  лога для испытаний.
        else if (w.isCmd("generate", "g")) {
            final boolean verbose = w.hasOpt("-v", "--verbose");
            final int cnt = (int) w.optValueLongOrDef(96, "--count", "-c");
            //[DEBUG]Если нужно сразу заменить файл на который указывает конфиг
            //укажи -out $Config.inLags -w для перезаписи уже существующего
            String outName = w.optValue("-o", "-out");
            if ("$Config.inLags".equalsIgnoreCase(outName)) {
                outName = getProp("inLags", "lag.log.bin");
            }
            if (outName == null || outName.isEmpty()) {
                ans = "Not specified output file. use opt: -out (path)";
            } else {
                boolean canRewrite = w.hasOpt("-w", "--rewrite-exists");
                ans = LagStats.genRndLagFile(cnt, outName, canRewrite, getOut(), verbose);
            }
        }

        return ans;
    }


    // ------------------------------------------------------------------- \\

    public Config getConfig() {
        if (this.config == null) {
            String cnfgfile = w.optValue("--config", "-c");
            this.config = new Config(cnfgfile).reload();
        }
        return this.config;
    }

    public void setOut(PrintStream out) {
        this.getConfig().out = out;
    }

    public PrintStream getOut() {
        return getConfig().out;
    }


    public String getProp(String name, String def) {
        if (getConfig().props == null) {
            throw new IllegalStateException("No Props!");
        }
        String v = this.config.props.getProperty(name);
        return (v == null || v.isEmpty()) ? def : v;
    }

    public int getPropI(String name, int def) {
        if (getConfig().props == null) {
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


}
