package org.swarg.mc.optistats;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.swarg.cmds.ArgsWrapper;
import org.swarg.mc.optistats.jfreechart.LagStatsJFC;
import org.swarg.mc.optistats.jfreechart.TimingStatsJFC;

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
                .performRequests();
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

    public static final String USAGE = "<help/version/config/lags/stats> [--config path]";
    //если нужно указать конкретный путь к конфигу --config path/to/cnfg.properties

    /**
     *
     */
    public void performRequests() {
        String[] args = w.getArgs();
        int b = 0;
        //String look = java.util.Arrays.asList(args).toString();
        int i;
        for (i = 0; i <= args.length; i++) {            
            if (i == args.length || ":".equals(args[i])) {
                int len = i - b;
                String[] args0;
                //если нет разделителя команд : - ничего не изменять
                if (!(b == 0 && i == args.length)) {
                    args0 = new String[len];
                    System.arraycopy(args, b, args0, 0, len);
                    //String look0 = java.util.Arrays.asList(args0).toString();
                    this.w.setArgs(args0);
                }
                performRequest();
                b += len + 1;
            }
        }
    }

    /**
     * Выполнение заданной команды
     */
    public void performRequest() {
        Object ans = null;
        try {
            if (w.isHelpCmdOrNoArgs()) {
                ans = USAGE;
            }
            else if (w.isCmd("version", "v")) {
                ans = getConfig().getVersionInfo();
            }
            //показать полный путь к конфигу
            else if (w.isCmd("config", "c")) {
                ans = showConfigInfo(w);
            }
            //debug-gen binlog-file
            else if (w.isCmd("lags", "l")) {
                ans = cmdLags();
            }
            else if (w.isCmd("stats", "s")) {
                ans = cmdStats();
            }
            //-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=9009
            else if (w.isCmd("sleep")) {
                try {
                    Thread.sleep(w.argI(w.ai++, 5000));
                    ans = "Done.";
                }catch (Exception e) {}
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
            "<path/dirs/prop/show-all-props>";
    /**
     * Данные о конфиге и текущей рабочей директории (для отладки)
     * @param w
     * @return
     */
    private Object showConfigInfo(ArgsWrapper w) {
        if (w.isHelpCmd()) {
            return CONFIG_USAGE;
        }

        //просто вывести полный путь к конфигу
        if (w.isCmd("path")) {
            return config.configFile.toAbsolutePath();
        }
        
        StringBuilder sb = new StringBuilder();
        if (w.noArgs()|| w.isCmd("dirs")) {
            //текущий рабочий каталог в котором запущено приложение
            sb.append("pwd: ").append(Paths.get(".").toAbsolutePath()).append('\n');
            sb.append(" wd: ").append(getConfig().getWorkDir()).append('\n');
            //откуда был взят или где должен лежать конфиг
            sb.append("cnf: ").append(config.configFile.toAbsolutePath()).append('\n');
        }

        //получить значение для одного или несколько указанных свойств
        else if (w.isCmd("prop", "p")) {
            while (w.argsRemain() > 0) {
                String name = w.arg(w.ai++);
                sb.append(name).append('=').append(config.props.get(name)).append('\n');
            }
        }
        //опционально все свойства конфига
        else if (w.isCmd("show-all-props", "sap")) {
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
        Path in = getPathByOptOfDef("-in", "inLags", "lag.log.bin");

        //временное ограничение (на данный момент все данные собираются в один файл)
        final long now = System.currentTimeMillis();
        final long before24h = now - 24*60*60000;
        long s = w.optValueLongOrDef(before24h, "-s", "--start-time"); //за 24ч до сейчас; old начало текущего дня Utils.getStartTimeOfCurentDay()
        long e = w.optValueLongOrDef(now, "-e", "--end-time");


        //create-lag-image
        if (w.isCmd("img", "i")) {
            checkJFreeChartInClassPath();
            //куда ложить созданную картинку
            Path png = getPathByOptOfDef("-out", "outLagsImg", "lag.png");

            int weight = (int) w.optValueLongOrDef(getPropI("lagChartWeight", 960), "-w", "--weight");
            int height = (int) w.optValueLongOrDef(getPropI("lagChartHeight", 400), "-h", "--height");

            ans = LagStatsJFC.createChartImg(in, png, weight, height, s, e);
        }

        //bin-log to text
        else if (w.isCmd("view", "v")) {
            boolean showMillis = w.hasOpt("-sm", "--show-millis");
            ans = LagStats.getReadable(in, s, e, showMillis);
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
                Path out = getConfig().getFullPath(outName);
                ans = LagStats.genRndLagFile(cnt, out, canRewrite, getOut(), verbose);
            }
        }

        return ans;
    }

    /**
     * Получить путь из опции optName либо дефолтное значение из конфига, либо
     * если в конфиге ключа с cnfgName нет - def
     * Пример использования указание входного файлы бинарного лог
     * -in path 
     * если опция не указывается в аргументах запуска будет взято значение из 
     * конфига. Если конфига нет - просанное дефолтное def
     * @param optName имя опции 
     * @param cnfgName имя ключа в конфиге для поиска дефолтного значения
     * @param def дефолтное значение для случая когда данное знение не указано в конфиге
     * @return 
     */
    public Path getPathByOptOfDef(String optName, String cnfgName, String def) {
        String defInStats = getProp(cnfgName, def);
        String spath = w.optValueOrDef(defInStats, optName);
        return getConfig().getFullPath(spath);
    }

    /* -------------------------------------------------------------------
                                  Stats
       ------------------------------------------------------------------- */
    private static final String STATS_USAGE =
              "img  [-in (file-log.bin)] [-out (img.png)] [-w|--weight X] [-h|--height Y] --start-time L1 --end-time L2\n"
            + "html [-in (file-log.bin)] [-out (img.png)] [-w|--weight X] [-h|--height Y] --start-time L1 --end-time L2\n"
            + "view [-in (file-log.bin)] [--start-time L1] [--end-time L2]\n"
            + "[NOTE]: Defaults for -in & -out take from config; Default [-s|--start-time] - StartOfCurrentDay, for [-e|--end-time] - CurrentTime\n";
    //stats
    private Object cmdStats() throws IOException {
        if (w.isHelpCmdOrNoArgs()) {
            return STATS_USAGE;
        }
        Object ans = "UNKNOWN";
        //где лежит бинарный лог с данными
        Path in = getPathByOptOfDef("-in", "inStats", "stats.log.bin");//path to binarylog

        //временное ограничение (на данный момент все данные собираются в один файл)
        final long now = System.currentTimeMillis();
        final long before24h = now - 24*60*60000;
        long s = w.optValueLongOrDef(before24h, "-s", "--start-time"); //за 24ч до сейчас; old начало текущего дня Utils.getStartTimeOfCurentDay()
        long e = w.optValueLongOrDef(now, "-e", "--end-time");

        if (false) {
        }
        // stats view  bin-log to text
        else if (w.isCmd("view", "v")) {
            ans = TimingStats.getReadable(in, s, e);
        }
        else if (w.isCmd("img", "i")) {
            checkJFreeChartInClassPath();

            Path png = getPathByOptOfDef("-out", "outStatsImg", "stats.png");

            int weight = (int) w.optValueLongOrDef(getPropI("statsChartWeight", 1280), "-w", "--weight");
            int height = (int) w.optValueLongOrDef(getPropI("statsChartHeight",  600), "-h", "--height");
            Object/*TimeSeries*/ ts = null;
            //для создания графика на котором будет добавлен график лагов
            if (w.hasOpt("--lags")) {
                Path blLags  = getPathByOptOfDef("--lags", "inLags", "lag.log.bin");//path to binarylog of lags
                //height = weight;
                //weight *=2;
                ts = LagStatsJFC.createTimeSeries(LagStats.parseFromBin(blLags, s, e), "Lags", 0);
            }
            ans = TimingStatsJFC.createChartImg(in, png, weight, height, s, e, ts);
        }

        //stats html
        //Создание html-страницы с графиком на основе данных о лагах и производительности сервера
        else if (w.isCmd("html", "h")) {
            Path html    = getPathByOptOfDef("-out", "outStatsHtml", "stats.html");//куда сохранять Html
            Path blLags  = getPathByOptOfDef("--lags", "inLags", "lag.log.bin");//path to binarylog of lags
            Path blStats = in;
            ans = TimingStatsHtmlGen.createHtmlChart(blStats, blLags, html, s, e);
        }

        return ans;
    }


    // ------------------------------------------------------------------- \\

    public Config getConfig() {
        if (this.config == null) {
            String cnfgfile = w.optValue("--config", "-c");
            this.config = new Config(cnfgfile);
            this.config.reload();
        }
        return this.config;
    }

    public Viewer setOut(PrintStream out) {
        this.getConfig().out = out;
        return this;
    }

    public PrintStream getOut() {
        return getConfig().getOut();
    }


    public String getProp(String name, String def) {
        return getConfig().getProp(name, def);
    }

    public int getPropI(String name, int def) {
        return getConfig().getPropI(name, def);
    }

    /**
     * Проверка наличия зависимости в класспафе
     * И если разрешено через конфиг - динамическая её загрузка
     */
    private void checkJFreeChartInClassPath() {
         if (config != null) {
             if (config.checkAndLoadDependency("org.jfree.chart.ChartFactory")) {
                 return;//Ok
             }
         }
         System.exit(-1);
    }



}
