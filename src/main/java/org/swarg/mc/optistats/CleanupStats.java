package org.swarg.mc.optistats;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import org.swarg.mcforge.statistic.CleanupEntry;
import org.swarg.mcforge.statistic.StatEntry;

/**
 * 21-11-21
 * @author Swarg
 */
public class CleanupStats {

    /**
     * Чтение и сериализация бинарных данных из лога статистики состояний сервер
     * порциями по ~8192 байт(Кратное размеру сериализации одного обьекта)
     * Чтение идёт с конца файла(Т.к. свежие данные добавляются в конец) и до
     * момента выхода времени записи за границы startStampTime.
     * Как только нижняя граница времени пройдена выход из режима чтения
     * @param in бинарный лог файл
     * @param startStampTime
     * @param endStampTime
     * @return
     */
    public static List<CleanupEntry> parseFromBin(Path in, long startStampTime, long endStampTime) {
        try {
            long fsz = Files.size(in);

            List<CleanupEntry> list = new ArrayList<>();
            final int oesz = CleanupEntry.getSerializeSize();
            int cnt = 8192 / oesz;//сколько записей за 1 раз будем читать с файла
            int bufflen = cnt * oesz;
            ByteBuffer buf = ByteBuffer.allocate(bufflen);

            try (FileChannel fc = FileChannel.open(in, StandardOpenOption.READ)){
                long off = fsz - bufflen;
                if (off < 0) {
                    off = 0;
                }
                while (off >= 0) {
                    buf.clear();
                    final int r = fc.read(buf, off);
                    boolean inrange = false;
                    int offi = r;
                    final byte[] ba = buf.array();
                    while ((offi-=oesz) >= 0) {
                        final long time = buf.getLong(offi+8);
                        if (Utils.isTimeInRange(time, startStampTime, endStampTime )) {
                            list.add(new CleanupEntry(ba, offi));
                            inrange = true;
                        }
                    }
                    if (!inrange) {
                        break;
                    }
                    off -= bufflen;
                }

            }//finally fc.close();

            Collections.reverse(list);
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
     * @param in файл бинарного лога
     * @param stats Путь к логу статистики для поиска конкретных значений по времени.
     * @param startStampTime начало от когото нужно выводить (0 - с самого начала)
     * @param endStampTime значение timeMillis до которого делать выборку значений
     * @return
     */
    public static Object getReadableTable(Path in, Path stats, long startStampTime, long endStampTime) {
        List<CleanupEntry> list = parseFromBin(in, startStampTime, endStampTime);
        List<StatEntry> statsList = TimingStats.parseFromBin(stats, startStampTime, endStampTime);
        pupulateData(list, statsList);
        if (list == null || list.isEmpty()) {
            return "Emtpty for " + in;
        } else {
            final int sz = list.size();
            StringBuilder sb = new StringBuilder((sz+1) * 140);
            sb.append(CleanupEntry.getTableHeaders());
            for (int i = 0; i < sz; i++) {
                CleanupEntry ce = list.get(i);
                ce.appendAsTableRow(sb);
            }
            return sb;
        }
    }

    /**
     * Заполняю данные об состояниях во время разных фаз очистки на основе лога
     * TimingsStats
     * @param cleans
     * @param stats
     */
    public static void pupulateData(List<CleanupEntry> cleans, List<StatEntry> stats) {
        if (cleans != null && cleans.size() > 0 && stats != null && stats.size() > 0) {
            for (int i = 0; i < cleans.size(); i++) {
                CleanupEntry e = cleans.get(i);
                if (e.init != null) {
                    for (int j = 0; j < stats.size(); j++) {
                        StatEntry s = stats.get(j);
                        if (s != null && s.time == e.init.time) {
                            e.init = s;
                        }
                    }
                }
            }
        }
    }
}
