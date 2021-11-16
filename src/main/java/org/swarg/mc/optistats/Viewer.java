package org.swarg.mc.optistats;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.swarg.cmds.ArgsWrapper;

/**
 * 15-11-21
 * @author Swarg
 */
public class Main {


    private static final String USAGE =
              "help\n "
            + "lag-img [-in (file-log.bin)] [-out (img.png)] [--weight X --height Y] [--start-time L1] [--end-time L2 Def-Now]\n"
            + "lag-view [-in (file-log.bin)] [--start-time L1] [--end-time L2 Def-Now]\n"
            + "gen-lag debug tool\n";

    public static void main(String[] args) throws IOException {
        ArgsWrapper w = new ArgsWrapper(args);
        Object ans = null;
        if (w.isHelpCmdOrNoArgs()) {
            System.out.println(USAGE);
            System.exit(0);
        }
        long s = w.optValueLongOrDef(-1, "-s", "--start-time"); //TODO начало текущего дня
        long e = w.optValueLongOrDef(System.currentTimeMillis(), "-e", "--end-time");

        if (w.isCmd("gen-lag", "gl")) {
            String inname = w.optValueOrDef("lag.log.bin", "-out");
            ans = LagStats.genRndLagFile(inname);
        }
        //bin-log to text
        else if (w.isCmd("lag-view", "lv")) {
            String inname = w.optValueOrDef("lag.log.bin", "-in");
            Path p = Paths.get(".");
            System.out.println(inname+"\n"+p.toAbsolutePath());
            ans = LagStats.getLagReadable(inname, s, e);
        }
        //create-lag-image
        else if (w.isCmd("lag-img", "li")) {
            String inname = w.optValueOrDef("lag.log.bin", "-in");
            String png = w.optValueOrDef("lag.png", "-out");
            int weight = (int) w.optValueLongOrDef(640, "-w", "--weight");
            int height = (int) w.optValueLongOrDef(400, "-h", "--height");

            ans = LagStats.createChartImg(inname, png, weight, height, s, e);
        }
        else {
            ans = "UNKNOWN cmd: "+ w.arg(w.ai) ;
        }

        System.out.println(ans);
    }
}
