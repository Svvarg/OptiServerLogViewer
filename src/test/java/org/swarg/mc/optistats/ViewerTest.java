package org.swarg.mc.optistats;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 16-11-21
 * @author Swarg
 */
public class ViewerTest {

    /*Для ручной проверки команд*/
    @Test
    public void test_Cmd() {
        System.out.println("Cmd");
                //debug default command
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
                "stats", "html", "--lags"
            };
        if (cmd.length > 0) {
            Viewer.main(cmd);
        }
    }
}
