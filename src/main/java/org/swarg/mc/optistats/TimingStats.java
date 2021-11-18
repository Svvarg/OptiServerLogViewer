package org.swarg.mc.optistats;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.swarg.common.Binary;
import org.swarg.mcforge.statistic.StatEntry;

/**
 * Создание графика и просмотр содержимого бинарного лога timingStat Лога
 * модель и как сериализуется описано в mcfbackendlib
 * org.swarg.mcforge.statistic.StatEntry
 *
 * The normal way of generating HTML for a web response is to use a JSP or a
 * Java templating engine like Velocity or FreeMarker.
 * 17-11-21
 * @author Swarg
 */
public class TimingStats {

    public static List<StatEntry> parseFromBin(Path in, long startStampTime, long endStampTime) {
        try {
            List<StatEntry> list = new ArrayList<>();
            byte[] ba = Files.readAllBytes(in);
            final int oesz = StatEntry.getSerializeSize();
            int cnt = ba.length / oesz; //27
            int off = 0;
            for (int i = 0; i < cnt; i++) {
                final long time = Binary.readLong(ba, off);
                if (!Utils.isTimeInRange(time, startStampTime, endStampTime )) {
                    off += oesz;//просто пропускаю если время не подходит
                    continue;
                }
                list.add(new StatEntry(ba, off));
                off += oesz;
            }
            return list;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Для просмотра бинарного лога в простом текстовом виде в заданной временной
     * области
     * @param inname файл бинарного лога
     * @param startStampTime начало от когото нужно выводить (0 - с самого начала)
     * @param endStampTime значение timeMillis до которого делать выборку значений
     * @return
     */
    public static Object getReadable(Path in, long startStampTime, long endStampTime) {
        List<StatEntry> list = parseFromBin(in, startStampTime, endStampTime);
        if (list == null || list.isEmpty()) {
            return "Emtpty for " + in;
        } else {
            final int sz = list.size();
            StringBuilder sb = new StringBuilder(sz * 116);

            for (int i = 0; i < sz; i++) {
                StatEntry se = list.get(i);
                se.appendTo(sb).append('\n');
            }
            return sb;
        }
    }


}
