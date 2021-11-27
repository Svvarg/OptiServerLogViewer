package org.swarg.mc.optistats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.List;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.swarg.common.Binary;
import org.swarg.common.Strings;
import org.swarg.stats.TShEntry;

/**
 * 15-11-21
 * @author Swarg
 */
public class LagStats {

    /**
     * Преобразовать бинарный лог в читаемое представление
     * @param in
     * @param startStampTime
     * @param endStampTime
     * @param showMillis показывать и timeMillis
     * @return
     */
    public static Object getReadable(Path in, long startStampTime, long endStampTime, boolean showMillis) {
        List<TShEntry> list = TShEntry.selectFromBin(in, startStampTime, endStampTime);
        if (list == null || list.isEmpty()) {
            return "Emtpty for " + in;
        }
        else {
            final int sz = list.size();
            StringBuilder sb = new StringBuilder(sz * 64);

            for (int i = 0; i < sz; i++) {
                TShEntry le = list.get(i);
                le.appendTo(sb, i, showMillis).append('\n');
            }
            return sb;
        }
    }




    // ============================== DEBUG ===============================  \\

    /**
     * [DEBUG]
     * Генерация бинарного лога со случайными данными в обратном порядке - от
     * конца текущего дня и на случайное количество шагов в прошлое.
     * Генерация идёт в обратном порядке с конца выделенного массива к его началу
     * это сделано для того, чтобы эмулировать бинарный лог имеющий в себе записи
     * нескольких дней. Из которого в дальнейшем нужно будет взять только значения
     * для текущего дня.
     * @param eCount количество генерируемых записей если меньше 0 возьмёт 96
     * @param file куда записать
     * @param canRewrite переписывать для существующего файла (чтобы не затереть
     * случайно реальный лог)
     * @param out
     * @param verbose
     * @return
     * @throws IOException
     */
    public static Path genRndLagFile(int eCount, Path file, boolean canRewrite, PrintStream out, boolean verbose) throws IOException {
        Path dir = file.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
        if (Files.exists(file)) {
            out.print("Already Exists file: " + file);
            if (!canRewrite) {
                out.println(" Use opt [-w|--rewrite-exists]");
                return null;
            } else {
                out.println(" Will be changed");
            }
        }
        
        eCount = eCount <= 0 ? 96 : eCount;
        byte[] a = new byte[eCount*10];
        //для подсчёта количество записей сгенерированых для текущего дня
        long tStart = Utils.getStartTimeOfCurentDay();
        
        //точка начала генерации значений в обратном порядке - начало следующего дня
        long tEnd = Utils.getStartTimeOfNextDay();
        long t = tEnd;

        if (verbose) {
            out.println("Latest TimePoint: " + Strings.formatDateTime(t)+" ("+t+")" );
        }
        int off = eCount;
        Random rnd = new Random();
        int k = 0;
        int i = 0;//totalPointsвсего добавленых случайных временных точек логов
        int todayPoints = 0;// "сегодняшние лаги"
        //всего лагов и сегодняшние для проверки механики рисования графика на сегодня
        List<String> list = verbose ? new ArrayList<>() : null;

        while ( i < eCount) {
            k++;
            t -= k * (20_000 + rnd.nextInt(10_000));
            if (rnd.nextInt() % 2 == 0) {
                int lag = ((k+i) % 5 != 0) ? 50 + rnd.nextInt(2000) : 1000 + rnd.nextInt(10000);
                //Для добавления значений с конца массива. Начиная от конца текущего дня и вглубь прошлого
                off = (((eCount - i)-1) * 10);
                //Serialize to binlog
                Binary.writeLong(a, off, t );           off += 8;
                Binary.writeUnsignedShort(a, off, lag); off += 2;
                if (t > tStart ) {
                    todayPoints++;
                }
                if (verbose) {
                    //index : time
                    list.add(String.format("#% ,3d  %s (%d)   % ,6d",
                            eCount-i, Strings.formatDateTime(t), t, lag));
                }
                ++i;
            }
        }
        if (verbose) {
            Collections.reverse(list);
            for (String l : list) {
                out.println(l);
            }
        }
        //DEBUG
        out.println("TotalLagPoints: " + i + "  ToDayLagTimePoints: " + todayPoints);
        return Files.write(file, a);
    }


}
