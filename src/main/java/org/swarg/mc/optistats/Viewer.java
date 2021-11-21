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

    //временной отрезок для которого делать выборку данных из лога
    protected long startTime;
    protected long endTime;


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
            else if (w.isCmd("cleanups", "cl")) {
                ans = cmdCleanups();
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
            return config.getConfigFile().toAbsolutePath();
        }
        
        StringBuilder sb = new StringBuilder();
        if (w.noArgs()|| w.isCmd("dirs")) {
            //текущий рабочий каталог в котором запущено приложение
            sb.append("pwd: ").append(Paths.get(".").toAbsolutePath()).append('\n');
            sb.append(" wd: ").append(getConfig().getWorkDir()).append('\n');
            //откуда был взят или где должен лежать конфиг
            sb.append("cnf: ").append(config.getConfigFile().toAbsolutePath()).append('\n');
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

    private static final String DEFINE_DATETIME_RANGE_USAGE =
              "[DATATIME]: to Specify DateTime Range of Data Use:\n"
            + "[-s|--start-time] (Default: Now - 24hours), [-e|--end-time] - (Default:CurrentTime)\n"
            + "[--last-hours (Default:24)]\n";
    /**
     * [-s|--start-time] - StartOfCurrentDay, for [-e|--end-time] - CurrentTime --last-hours N \n"
     * @param maxDaysAgo
     */
    public void defineDateTimeRange(int maxDaysAgo) {
        final long now = System.currentTimeMillis();
        long lasthours = w.optValueLongOrDef(24, "--last-hours", "-lh");
        if (maxDaysAgo <= 0) {
            maxDaysAgo = 7;//default
        }
        if (lasthours > 24 * maxDaysAgo) {
            lasthours = 24 * maxDaysAgo ;//return "limit 7 days";//TODO добавить поддержку указания даты в читабельном виде (например для такого-то там дня)
        }
        final long before24h = now - lasthours *60*60000;
        this.startTime = w.optValueLongOrDef(before24h, "-s", "--start-time"); //за 24ч до сейчас; old начало текущего дня Utils.getStartTimeOfCurentDay()
        this.endTime = w.optValueLongOrDef(now, "-e", "--end-time");
    }


    private static final String LAGS_USAGE =
              "img  [-in (file-log.bin)] [-out (img.png)] [-w|--weight X] [-h|--height Y]\n"
            + "view [-in (file-log.bin)] [--no-millis]\n"
            + "[NOTE]: Defaults for -in & -out take from Config.inLags;\n"
            +  DEFINE_DATETIME_RANGE_USAGE
            + "generate [-out ($Config.inLags)] [-w|--rewrite-exists] [--count N Def:96] [--verbose] - [DEBUG] Generate Random Binary LogFile \n";

    private Object cmdLags() throws IOException {
        if (w.isHelpCmdOrNoArgs()) {
            return LAGS_USAGE;
        }
        Object ans = "UNKNOWN";
        //где лежит бинарный лог с данными
        Path in = getPathByOptOfDef("-in", "inLags", "lag.log.bin");

        //временное ограничение (на данный момент все данные собираются в один файл)
        defineDateTimeRange(14);

        //create-lag-image
        if (w.isCmd("img", "i")) {
            checkJFreeChartInClassPath();
            //куда ложить созданную картинку
            Path png = getPathByOptOfDef("-out", "outLagsImg", "lag.png");

            int weight = (int) w.optValueLongOrDef(getPropI("lagChartWeight", 960), "-w", "--weight");
            int height = (int) w.optValueLongOrDef(getPropI("lagChartHeight", 400), "-h", "--height");

            ans = LagStatsJFC.createChartImg(in, png, weight, height, this.startTime, this.endTime);
        }

        //bin-log to text
        else if (w.isCmd("view", "v")) {
            boolean showMillis = !w.hasOpt("-nm", "--no-millis");
            ans = LagStats.getReadable(in, this.startTime, this.endTime, showMillis);
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
    private static final String STATS_UPDATE_USAGE =
              "update [-in (stats-log.bin)] [--lags (lags-log.bin)] [--cleanups (Def:inCleanups)] [-out (html)]\n"
            + " - Update Data for Charts  [-d|--deploy-html - flag for (Re)Deploy html-files]\n";
    private static final String STATS_USAGE =
              STATS_UPDATE_USAGE
            + "img    [-in (stats-log.bin)] [-out (img.png)] [-w|--weight X] [-h|--height Y]\n"
            + "view   [-in (stats-log.bin)] \n"
            +  DEFINE_DATETIME_RANGE_USAGE //--start-time --end-time
            + "[NOTE]: Defaults for -in/-out take from Config.inStats/outStatsImg/outStatsHtml;\n";
    //stats
    private Object cmdStats() throws IOException {
        if (w.isHelpCmdOrNoArgs()) {
            return STATS_USAGE;
        }
        Object ans = "UNKNOWN";
        //где лежит бинарный лог с данными
        Path in = getPathByOptOfDef("-in", "inStats", "stats.log.bin");//path to binarylog

        //временное ограничение (на данный момент все данные собираются в один файл)
        this.defineDateTimeRange(7);

        // stats view  bin-log to text
        if (w.isCmd("view", "v")) {
            ans = TimingStats.getReadableTable(in, this.startTime, this.endTime);
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
                ts = LagStatsJFC.createTimeSeries(LagStats.parseFromBin(blLags, this.startTime, this.endTime), "Lags", 0);
            }
            ans = TimingStatsJFC.createChartImg(in, png, weight, height, this.startTime, this.endTime, ts);
        }

        //stats html
        //Создание html-страницы с графиком на основе данных о лагах и производительности сервера
        else if (w.isCmd("update", "u")) {
            if (w.isHelpCmd()) {
                return STATS_UPDATE_USAGE;
            }
            Path blStats = in;//getPathByOptOfDef("-in", "inStats", "stats.log.bin");//path to binarylog
            Path html    = getPathByOptOfDef("-out", "outStatsHtml", "stats.html");//куда сохранять Html
            Path blLags  = getPathByOptOfDef("--lags", "inLags", "lag.log.bin");//path to binarylog of lags
            Path blClnps = getPathByOptOfDef("--cleanups", "inCleanups", "cleanups.log.bin");
            /*если ключ не указан и нужных файлов нет - скопирует из ресурсов jar`ника
            если файлы уже существуют и опция не задана - не тронет*/
            boolean replace = w.hasOpt("--deploy-html","-d");
            RawChartData.deployHtmlFromResources(html, replace, getOut());
            
            ans = RawChartData.createRawDataForJSChart(blStats, blLags, blClnps, html, this.startTime, this.endTime, getOut());
        }

        return ans;
    }

    
    private static final String CLEANUPS_USAGE = 
            "view [-in (Def:Config.inCleanups)]\n" +
            DEFINE_DATETIME_RANGE_USAGE;

    private Object cmdCleanups() {
        if (w.isHelpCmdOrNoArgs()) {
            return CLEANUPS_USAGE;
        }
        Object ans = "UNKNOWN";
        //где лежит бинарный лог с данными
        Path in = getPathByOptOfDef("-in", "inCleanups", "cleanup-log.bin");

        defineDateTimeRange(14);

        //Текстовая Таблица срабатываний очисток
        if (w.isCmd("view", "v")) {
            ans = CleanupStats.getReadableTable(in, this.startTime, this.endTime);
        }
        return ans;
    }



    // ------------------------------------------------------------------- \\

    public Config getConfig() {
        if (this.config == null) {
            String cnfgfile = w.optValue("--config", "-c");
            this.config = new Config(cnfgfile);
            this.config.verbose = w.hasOpt("--verbose-startup");
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
