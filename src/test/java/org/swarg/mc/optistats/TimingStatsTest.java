package org.swarg.mc.optistats;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.swarg.common.Binary;
import org.swarg.mcforge.statistic.StatEntry;
import static org.junit.Assert.*;

/**
 * 20-11-21
 * @author Swarg
 */
public class TimingStatsTest {

    public TimingStatsTest() {
    }


    @Test
    public void testParseFromBin0() throws IOException {
        long s = 1637248300023L;//2021-11-18T18:11:40.023
        long e = 1637334706777L;//2021-11-19T18:11:46.777
        //Path in = Paths.get("src/test/resources/latest-stats-log.bin").toAbsolutePath();
        //Прочесть 1 мб - FullRead took 3 287 578  NIO:174 812 919
        Path tmp = Paths.get("build/generated-stats.bin");
        StringBuilder sb = null;//new StringBuilder();
        //int[] caps = new int[]{10, 1_000, 2_000, 5000, 10000, 40000, 100_000, 1000_000};

        //for (int k = 0; k < caps.length; k++) {
            //int cap = caps[k];//10_000;
            int cap = 1000;
            genStatsFile(tmp, e, cap, sb);//~1Mb
            //System.out.println(sb);
            Path in = tmp;
            //warmup
            //TimingStats.parseFromBin(in, s, e);
            //TimingStats.parseFromBin0(in, s, e);


            //long t0 = System.nanoTime();
            List<StatEntry> listExp = TimingStats.parseFromBinFull(in, s, e);
            //long t1 = System.nanoTime();
            //List<StatEntry> listRes0 = parseFromBinD(in, s, e);
            //long t2 = System.nanoTime();
            List<StatEntry> listRes = TimingStats.parseFromBin(in, s, e);
            //long t3 = System.nanoTime();

            assertEquals(listExp.size(), listRes.size());
            for (int i = 0; i < listRes.size(); i++) {
                StatEntry exp = listExp.get(i);
                StatEntry res = listRes.get(i);
                if (!exp.equals(res)) {
                    System.out.println(exp);
                    System.out.println(res);
                    fail("element" + i);
                }
            }

            //final long fsz = Files.size(in);
            //System.out.printf("\nEntries inFile: %s FileSize: %sb (%sKb)\n", cap, fsz, fsz/1024);
            //System.out.println("Type         Took (ns) Readed");
            //System.out.printf("parseFromBinF: % ,10d, sz: %s\n", (t1-t0), listExp.size());
            //System.out.printf("parseFromBinD: % ,10d, sz: %s\n", (t2-t1), listRes0.size());
            //System.out.printf("parseFromBin:  % ,10d, sz: %s\n", (t3-t2), listRes.size());
        //}
        /*
            Entries inFile: 10 FileSize: 270b (0Kb)
            Type         Took (ns) Readed
            parseFromBinF:    159,454, sz: 10
            parseFromBinD:    243,286, sz: 9
            parseFromBin:     149,195, sz: 10

            Entries inFile: 1000 FileSize: 27000b (26Kb)
            Type         Took (ns) Readed
            parseFromBinF:    340,600, sz: 289
            parseFromBinD:  3,127,537, sz: 289
            parseFromBin:     621,404, sz: 289

            Entries inFile: 2000 FileSize: 54000b (52Kb)
            Type         Took (ns) Readed
            parseFromBinF:    410,947, sz: 289
            parseFromBinD:  1,606,564, sz: 289
            parseFromBin:     388,377, sz: 289

            Entries inFile: 5000 FileSize: 135000b (131Kb)
            Type         Took (ns) Readed
            parseFromBinF:    670,354, sz: 289
            parseFromBinD:  1,681,015, sz: 289
            parseFromBin:     423,844, sz: 289

            Entries inFile: 10000 FileSize: 270000b (263Kb)
            Type         Took (ns) Readed
            parseFromBinF:  1,202,065, sz: 289
            parseFromBinD:  1,678,963, sz: 289
            parseFromBin:     331,220, sz: 289

            Entries inFile: 40000 FileSize: 1080000b (1054Kb)
            Type         Took (ns) Readed
            parseFromBinF:  1,283,551, sz: 289
            parseFromBinD:  1,639,685, sz: 289
            parseFromBin:     226,285, sz: 289

            Entries inFile: 100000 FileSize: 2700000b (2636Kb)
            Type         Took (ns) Readed
            parseFromBinF:  4,086,023, sz: 289
            parseFromBinD:  1,615,943, sz: 289
            parseFromBin:     236,837, sz: 289

            Entries inFile: 1000000 FileSize: 27000000b (26367Kb)
            Type         Took (ns) Readed
            parseFromBinF:  29,034,509, sz: 289
            parseFromBinD:  1,831,382, sz: 289
            parseFromBin:     192,577, sz: 289
        */
    }


    /**
     * Просто для сравнения скорости чтения при большом размере
     * @param out
     * @param stime
     * @param count
     * @param sb
     * @throws IOException
     */
    public static void genStatsFile(Path out, long stime, int count, StringBuilder sb) throws IOException {
        //new StatEntry(buf.array(), 0)
        final int szo = StatEntry.getSerializeSize();
        byte[] ba = new byte[count * szo];
        //int off = (count-1) * szo;
        int i = count;
        StatEntry se = new StatEntry();
        long time = stime;
        while (--i >= 0) {
            int off = i * szo;
            se.tps = (byte)200;
            se.memUsed = se.tiles = se.chunks = se.entities = i;
            se.serialize(ba, off);
            Binary.writeLong(ba, off, time);
            if (sb != null) {
                sb.append(time).append(' ').append(i).append('\n');
            }
            time -= 60_000 * 5;//эмулирую изменение времени "от сейчас" в прошлое
        }
        Files.write(out, ba);//, CREATE StandardOpenOption.APPEND
    }

    /**
     * Чтение буфером размером под 1 обьект намного медленнее
     * @param in
     * @param startStampTime
     * @param endStampTime
     * @return
     */
    @Deprecated
    public static List<StatEntry> parseFromBinD(Path in, long startStampTime, long endStampTime) {
        try {
            List<StatEntry> list = new ArrayList<>();
            final int oesz = StatEntry.getSerializeSize();
            ByteBuffer buf = ByteBuffer.allocate(oesz);

            long fsz = Files.size(in);
            FileChannel fc = FileChannel.open(in, StandardOpenOption.READ);

            //1 с конца файла найти точку с которой можно начать десериализовывать записи в обьекты
            //по starttime
            long off = fsz - oesz;
            while (off > 0) {
                buf.clear();
                do {
                    fc.read(buf, off);
                } while (buf.hasRemaining());

                final long time = buf.getLong(0);
                if (startStampTime > time) {
                    break;
                }
                if (Utils.isTimeInRange(time, startStampTime, endStampTime )) {
                    list.add(new StatEntry(buf.array(), 0));
                }
                off -= oesz;
            }
            fc.close();

            Collections.reverse(list);
            return list;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
