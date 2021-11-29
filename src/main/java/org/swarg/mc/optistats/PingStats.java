package org.swarg.mc.optistats;

import java.util.List;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import org.swarg.common.Strings;
import org.swarg.stats.TShEntry;

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
     * @param showLineNumber Добавлять для каждой строки её порядковый номер
     * @return
     */
    public static Object getReadable(Path in, long startStampTime, long endStampTime, boolean showMillis, boolean onlyPing, boolean showLineNumber) {
        List<TShEntry> list = TShEntry.selectFromBin(in, startStampTime, endStampTime);
        if (list == null || list.isEmpty()) {
            return "Emtpty for " + in + " In TimePeriod: " + startStampTime + " - " + endStampTime;
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
                    if (showLineNumber) {
                        sb.append(String.format("#% 4d  ", i));
                    }

                    le.appendTo(sb, showMillis).append('\n');
                }
            }
            return sb;
        }
    }


    /**
     * Построение гистограммы уникальных значений в наборе данных для указанной
     * гранулярности карзины значений. Для пинга оптимально указывать гранулярность 5
     * @param in
     * @param startStampTime
     * @param endStampTime
     * @param showMillis
     * @param basketGranulatiry точность карзины 1 - каждому значению своя корзина
     * Для пинга оптимально указывать 5, при 10 возникают вопросы восприятия например 50 это все от 50 до 60 но логичнее было бы к 50 относить [45-55)
     * @return
     */
    public static Object getHistogram(Path in, long startStampTime, long endStampTime, boolean showMillis, int basketGranulatiry) {
        List<TShEntry> list = getPingsOnly(TShEntry.selectFromBin(in, startStampTime, endStampTime));
        if (list == null || list.isEmpty()) {
            return "Emtpty for " + in + " In TimePeriod: " + startStampTime + " - " + endStampTime;
        }
        else {
            StringBuilder sb = new StringBuilder("[Ping Histo] Period: ");
            TShEntry.appendDateTimeRange(list, showMillis, sb).append('\n');
            sb.append("  ping   count     FirstTime          millis            LastTime\n");
                     //    50     116  01:09:42 27.11.21 (1637964582208) - 10:27:40 27.11.21 (1637998060699)
            List<int[]> histo = TShEntry.getHistogram(list, basketGranulatiry);
            TShEntry.getReadableHistogram(list, histo, basketGranulatiry, showMillis, sb).toString();
            //todo показать шаг выборки между значений
            return sb;
        }
    }

    /**
     * Более простое представление данных о пинге
     * на основе сравнения количества сэмплев с нормой и с превышением указанного
     * порогового значения
     * @param in
     * @param startStampTime
     * @param endStampTime
     * @param showMillis
     * @param threshold этого значения счить пинг нарушением порог
     * @return
     */
    public static Object getRatio(Path in, long startStampTime, long endStampTime, boolean showMillis, int threshold) {
        List<TShEntry> list = getPingsOnly(TShEntry.selectFromBin(in, startStampTime, endStampTime));
        if (list == null || list.isEmpty()) {
            return "Emtpty for " + in;
        }
        else {
            StringBuilder sb = new StringBuilder("[Ping Ratio] Period: ");
            TShEntry.appendDateTimeRange(list, showMillis, sb).append('\n');
            //игронировать конекты и дисконекты закодированные в значения            
            TShEntry.appendRatioAndAvrg(list, threshold, "ms", sb);
            return sb;
        }
    }


    /**
     * Получить лист только со значениями пинга без служебной информации в виде
     * времени подключения и дизконектов
     * @param list
     * @return
     */
    public static List<TShEntry> getPingsOnly(List<TShEntry> list) {
        if (list != null && list.size() > 0) {
            List<TShEntry> l0 = new ArrayList<>(list.size());
            final int sz = list.size();
            for (int i = 0; i < sz; i++) {
                TShEntry e = list.get(i);
                if (e != null && e.value > 0 && e.value != 65535) {
                    l0.add(e);
                }
            }
            return l0;
        }
        return Collections.EMPTY_LIST;
    }
}
