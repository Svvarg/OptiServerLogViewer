package org.swarg.mc.optistats;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.swarg.common.Strings;
import org.swarg.mcforge.statistic.TShEntry;
import org.swarg.mc.optistats.LagStats;

/**
 * Обработка пинг статистики
 * Структура бинарного лога такая же как у лаг-статистики 8 байт время 2 байта значение
 * 0     - (пере)подключение
 * 65535 - дисконект(обрыв связи) проверить!
 * 25-11-21
 * @author Swarg
 */
public class PingStats {
    /**
     * Преобразовать бинарный лог в читаемое представление
     * @param in
     * @param startStampTime
     * @param endStampTime
     * @param showMillis показывать и timeMillis
     * @param onlyPing
     * @return
     */
    public static Object getReadable(Path in, long startStampTime, long endStampTime, boolean showMillis, boolean onlyPing) {
        List<TShEntry> list = LagStats.parseFromBin(in, startStampTime, endStampTime);
        if (list == null || list.isEmpty()) {
            return "Emtpty for " + in;
        }
        else {
            final int sz = list.size();
            StringBuilder sb = new StringBuilder(sz * 64);

            for (int i = 0; i < sz; i++) {
                TShEntry le = list.get(i);
                //0 - в это время было (пере)подключение 65535 - дисконект
                if (le.value == 0 || le.value == 65535) {
                    if (!onlyPing) {//прятать служебные записи о соединении и дисконнекте
                        String fdt = Strings.formatDateTime(le.time);
                        sb.append(String.format("#% 4d  %s (%d)  %s\n", i, fdt, le.time, le.value == 0 ? "Connection" : "Disconnect"));
                    }
                } else {
                    le.appendTo(sb, i, showMillis).append('\n');
                }
            }
            return sb;
        }
    }


    /**
     * Простая гистограмма
     * Пример вывода
     * value    count     firstTime        (ft millis)        latestTime       (lt millis)
     *     60      30  01:13:40 27.11.21 (1637964820697) - 07:51:40 27.11.21 (1637988700700)
     * пинг,    сколько таких значений встречается во всех записях указанного промежутка
     * время первой найденой записи - время последней (т.е. все эти записи находятся
     * в интервале между этими двумя датами. Для уникального значений будет
     * только одна временная точка - время её возникновения.
     * сколько
     * @param in
     * @param startStampTime
     * @param endStampTime
     * @param showMillis
     * @param basketGranulatiry точность карзины 1 - каждому значению своя корзина
     * @return
     */
    public static Object getHistogram(Path in, long startStampTime, long endStampTime, boolean showMillis, int basketGranulatiry) {
        List<TShEntry> list = LagStats.parseFromBin(in, startStampTime, endStampTime);
        if (list == null || list.isEmpty()) {
            return "Emtpty for " + in;
        }
        else {
            final int sz = list.size();
            Map<Integer, int[]> mbasket = new TreeMap();
            for (int i = 0; i < sz; i++) {
                TShEntry le = list.get(i);
                final int v = le.value;
                //0 - в это время было (пере)подключение, 65535 - дисконект
                if (le.value == 0 || le.value == 65535) {
                    ;//connection and disconnect - ignore
                } else {                    
                    Integer key = Integer.valueOf(v);
                    int[] box = mbasket.get(key);
                    if (box == null) {
                        box = new int[3];
                        box[1] = i;//индекс первой записи с данным значением
                        box[2] = -1;
                        mbasket.put(key, (box));
                    } else {
                        box[2] = i;//запоминаю последний записанный индекс записи (для получения в дальнейшем времени по индексу)
                    }
                    box[0]++;
                }
            }
            //читабельный вывод
            StringBuilder sb = new StringBuilder();
            if (mbasket.size() > 0) {
                for (Map.Entry<Integer, int[]> e : mbasket.entrySet()) {
                    Integer ping = e.getKey();
                    int[] box = e.getValue();
                    int cnt = (box != null) ? box[0] : -1;
                    TShEntry le0 = list.get(box[1]);
                    String fdt0 = Strings.formatDateTime(le0.time);//время проявления первого такого значения
                    sb.append(String.format("% 6d  % 6d  %s (%s)", ping, cnt, fdt0, le0.time));
                    final int ilast = box[2];
                    if (ilast > -1) {
                        TShEntry lel = list.get(ilast);
                        String fdtl = Strings.formatDateTime(lel.time);//время проявления последнего такого значения
                        sb.append(" - ").append(fdtl).append(" (").append(lel.time).append(")");
                    }
                    sb.append('\n');
                }
            }
            return sb;
        }
    }

}
