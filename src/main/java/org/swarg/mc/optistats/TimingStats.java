package org.swarg.mc.optistats;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import org.swarg.mcforge.statistic.StatEntry;
import org.swarg.common.Binary;

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
    public static List<StatEntry> parseFromBin(Path in, long startStampTime, long endStampTime) {
        try {
            long fsz = Files.size(in);

            List<StatEntry> list = new ArrayList<>();
            final int oesz = StatEntry.getSerializeSize();
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
                    //do {r = fc.read(buf, off);} while (buf.hasRemaining());
                    final int r = fc.read(buf, off);//? может ли бысть здесь случай прочтения байтов не кратных размеру обьекта?

                    //всё содержимое блока подходит по времени начала
                    boolean inrange = false;
                    int offi = r;
                    final byte[] ba = buf.array();
                    while ((offi-=oesz) >= 0) {
                        final long time = buf.getLong(offi);
                        if (Utils.isTimeInRange(time, startStampTime, endStampTime )) {
                            list.add(new StatEntry(ba, offi));
                            inrange = true;
                        }
                    }
                    /*если ни одна запись не подошла по времени значит произошел
                    выход за рамки StartStampTime - выходим*/
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
     * Выборка данных из бинарного лога в указанном диапазоне времени
     * Через простое чтение всего файла в память
     * @param in
     * @param startStampTime
     * @param endStampTime
     * @return
     */
    @Deprecated
    public static List<StatEntry> parseFromBinFull(Path in, long startStampTime, long endStampTime) {
        try {
            List<StatEntry> list = new ArrayList<>();
            final int oesz = StatEntry.getSerializeSize();
            byte[] ba = Files.readAllBytes(in);
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
     * @param in файл бинарного лога
     * @param startStampTime начало от когото нужно выводить (0 - с самого начала)
     * @param endStampTime значение timeMillis до которого делать выборку значений
     * @return
     */
    public static Object getReadableTable(Path in, long startStampTime, long endStampTime) {
        List<StatEntry> list = parseFromBin(in, startStampTime, endStampTime);
        if (list == null || list.isEmpty()) {
            return "Emtpty for " + in;
        } else {
            final int sz = list.size();
            StringBuilder sb = new StringBuilder(sz * 96);
            //headers
            sb.append(StatEntry.getTableHeaders());

            for (int i = 0; i < sz; i++) {
                StatEntry se = list.get(i);
                se.appendAsTableRow(sb);
            }
            return sb;
        }
    }


}
