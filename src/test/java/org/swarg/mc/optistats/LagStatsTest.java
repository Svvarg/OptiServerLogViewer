package org.swarg.mc.optistats;

import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.swarg.mcforge.statistic.LagEntry;
import org.swarg.mc.optistats.jfreechart.LagStatsJFC;
import static org.junit.Assert.*;

/**
 * 15-11-21
 * @author Swarg
 */
public class LagStatsTest {

    /**
     * Test of genRndLagFile method, of class LagStats.
     */
    //@Test
    public void testGenRndLagFile() throws Exception {
        System.out.println("genRndLagFile");
        Path log = Paths.get("build/generated-lags.bin");
        boolean canRewrite = true;
        LagStats.genRndLagFile(96, log, canRewrite, System.out, false);
        //System.out.println(LagStats.getLagReadable(log, 0, 0));//+
        LagStatsJFC.createChartImg(log, Paths.get("lag.png"), 1280, 400, 0, 0);
    }

    /*Проверка правильности чтения данных порциями*/
    @Test
    public void testParseFromBin() throws IOException {
        long s = 1637248300023L;//2021-11-18T18:11:40.023
        long e = 1637334706777L;//2021-11-19T18:11:46.777
        Path tmp = Paths.get("build/generated-lags.bin");

        genLagsFile(tmp, e, 1000, null);

        Path in = tmp;

        List<LagEntry> listExp = LagStats.parseFromBinFull(in, s, e);
        List<LagEntry> listRes = LagStats.parseFromBin(in, s, e);

        assertEquals(listExp.size(), listRes.size());
        for (int i = 0; i < listRes.size(); i++) {
            LagEntry exp = listExp.get(i);
            LagEntry res = listRes.get(i);
            if (!exp.equals(res)) {
                System.out.println(exp);
                System.out.println(res);
                fail("element" + i);
            }
        }
    }

    /**
     * Для проверки чтения данных из файла по частям
     * Генерация бинарного лога заданного размера
     * @param out
     * @param etime конечное время от которого генерировать назад во времени
     * @param count сколько записей создавать
     * @param sb
     * @throws IOException
     */
    public static void genLagsFile(Path out, long etime, int count, StringBuilder sb) throws IOException {
        final int szo = LagEntry.getSerializeSize();
        byte[] ba = new byte[count * szo];

        int i = count;
        LagEntry e = new LagEntry();
        long time = etime;
        while (--i >= 0) {
            int off = i * szo;
            
            e.serialize(ba, off, time, /*lag*/i);
            
            if (sb != null) {
                sb.append(time).append(' ').append(i).append('\n');
            }
            time -= 60_000 * 5;//эмулирую изменение времени "от сейчас" в прошлое
        }
        Files.write(out, ba);//, CREATE StandardOpenOption.APPEND
    }

}
