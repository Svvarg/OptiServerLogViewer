package org.swarg.mc.optistats;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 16-11-21
 * @author Swarg
 */
public class ViewerTest {

    @Test
    public void test_MultipleCommands() {
        System.out.println("MultipleCommands");
        String cmd = "--config "+ System.getProperty("user.home") + "/stats/DefaultConfig.properties : cmd1 : cmd2 : cmd3 : cmd4";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Viewer v = new Viewer(cmd.split(" "))
                .init()
                .setOut(ps);

        v.performRequests();
        String s = baos.toString();

        assertTrue("0", s.contains("--config"));
        assertTrue("1", s.contains("cmd1"));
        assertTrue("2", s.contains("cmd2"));
        assertTrue("3", s.contains("cmd3"));
        assertTrue("4", s.contains("cmd4"));


        baos.reset();
        v.w.setArgs(new String[]{"help", "-v"});
        v.performRequests();
        s = baos.toString().trim();
        assertEquals(Viewer.USAGE, s);
        //System.out.println(s);
    }

    /*Для ручной проверки команд*/
    @Test
    public void test_Cmd() {
        System.out.println("Cmd");
                //debug default command
        //Теперь по умолчанию если конфига рядом нет - ищет в ~/Config.defConfigDirInUserHome
        //String config = System.getProperty("user.home")+"/mcs-stats/DefaultConfig.properties";

        if (0==1) {
            String[] cmd = new String[] {
                    //"lags", "generate", "-o", "$Config.inLags", "-v"
                    //"lags", "generate", "-o", "$Config.inLags", "-w", "-v"
                    //"lags", "view", "-s", "0", "-e", "0", "-sXm"//show all
                    //"lags", "view", "-sm" //latest now currDay
                    //"config","show-props"
                    //"lags", "img"
                    //"stats", "view"
                    //"stats", "img"
                    //"stats", "img", "--lags"
                    "stats", "update", "--lags"
                };
            if (cmd.length > 0) {
                Viewer.main(cmd);
            }
        }
        
        if (0==0) {
            //Viewer.main(new String[]{"config"});
            //Viewer.main(new String[]{"stats", "update", "--last-hours", "24"});//"--config", config
            Viewer.main(new String[]{"ping", "histogram", "-s", "1637953031105"});//"--config", config
            //Viewer.main(("stats img : stats html : lags img : --config " + config).split(" "));
            //Viewer.main(("stats img --lags --config " + config).split(" "));
            //Viewer.main(new String[]{"stats", "img"});
            //Viewer.main(new String[]{"lags",  "img"});
        }        
    }
}
