package org.swarg.mc.optistats;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
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
     * @param showLineNumber показывать порядковый номер строки
     * @return
     */
    public static Object getReadable(Path in, long startStampTime, long endStampTime, boolean showMillis, boolean showLineNumber) {
        List<TShEntry> list = TShEntry.selectFromBin(in, startStampTime, endStampTime);
        if (list == null || list.isEmpty()) {
            return Utils.showPeriod("Emtpty for ", in, startStampTime, endStampTime, true);
        }
        else {
            final int sz = list.size();
            StringBuilder sb = new StringBuilder((sz+1) * 64);
            //отображаю именно указанный период выборки, а не крайние времени когда были обнаружены лаги
            Utils.appendTimePeriod(sb, startStampTime, endStampTime, showMillis);

            if (showLineNumber){
                sb.append("#   N ");
            }
            sb.append("  Time     Date      ").append(showMillis?"(millis)       ":"").append("Lag(ms)\n");

            for (int i = 0; i < sz; i++) {
                TShEntry le = list.get(i);
                if (showLineNumber) {
                    sb.append(String.format("#% 4d  ", i));
                }
                le.appendTo(sb, showMillis).append('\n');
            }
            return sb;
        }
    }


    /**
     * Сравнить количество значений не превышающих заданное пороговое значение
     * с теми которые превышают порог.
     * Лаг - это уже наружение, но данным методом можно быстро выявить процентное
     * соотвеное по количеству каких лагов больше
     * @param in
     * @param startStampTime
     * @param endStampTime
     * @param showMillis
     * @param threshold этого значения счить пинг нарушением порог
     * @return
     */
    public static Object getRatio(Path in, long startStampTime, long endStampTime, boolean showMillis, int threshold) {
        List<TShEntry> list = TShEntry.selectFromBin(in, startStampTime, endStampTime);
        if (list == null || list.isEmpty()) {
            return Utils.showPeriod("Emtpty for ", in, startStampTime, endStampTime, true);
        }
        else {
            StringBuilder sb = new StringBuilder("[Lags Ratio] Period: ");
            //TShEntry.appendDateTimeRange(list, showMillis, sb).append('\n');
            //отображаю именно указанный период выборки, а не крайние времени когда были обнаружены лаги
            Utils.appendTimePeriod(sb, startStampTime, endStampTime, showMillis);
            TShEntry.appendRatioAndAvrg(list, threshold, "ms", sb);
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
        List<TShEntry> list = TShEntry.selectFromBin(in, startStampTime, endStampTime);
        if (list == null || list.isEmpty()) {
            return Utils.showPeriod("Emtpty for ", in, startStampTime, endStampTime, true);
        }
        else {
            StringBuilder sb = new StringBuilder("[Lags Histo] Period: ");
            //TShEntry.appendDateTimeRange(list, showMillis, sb).append('\n');
            //отображаю именно указанный период выборки, а не крайние времени когда были обнаружены лаги
            Utils.appendTimePeriod(sb, startStampTime, endStampTime, showMillis);
            sb.append("  lags   count     FirstTime          millis            LastTime");
            sb.append("  [Granulatiry:").append(basketGranulatiry).append("]\n");
            //    50     116  01:09:42 27.11.21 (1637964582208) - 10:27:40 27.11.21 (1637998060699)
            List<int[]> histo = TShEntry.getHistogram(list, basketGranulatiry);
            TShEntry.getReadableHistogram(list, histo, basketGranulatiry, showMillis, sb).toString();
            //todo показать шаг выборки между значений
            return sb;
        }
    }
    /**
     * Получить в заданном временном участке сводку по лагам
     * сумма длительности всех лагов, максимальный и средний лаг, процент от периода
     * [Lags Sum] Period: 00:00:00 02.12.21 - 00:00:00 03.12.21
     * AverageLag: 1253(ms) [20] MaxLag: 1764(ms) at 16:47:02 02.12.21
     * Total LagsDuration: 25060ms (25s 60ms), Period:24h , LagPercent: 0,0290 %
     * @param in
     * @param startStampTime
     * @param endStampTime
     * @param showMillis
     * @return
     */
    public static Object getSummary(Path in, long startStampTime, long endStampTime, boolean showMillis) {
        List<TShEntry> list = TShEntry.selectFromBin(in, startStampTime, endStampTime);
        if (list == null || list.isEmpty()) {
            return Utils.showPeriod("Emtpty for ", in, startStampTime, endStampTime, true);
        }
        else {
            StringBuilder sb = new StringBuilder("[Lags Sum] Period: ");
            //отобразить границы исследуюемого времени, а не крайние значения времени из листа с лагами
            Utils.appendTimePeriod(sb, startStampTime, endStampTime, showMillis).append('\n');

            int[] va = LagStats.getSummary(list);//0sum 1avrg 2imax
            final int sz = list.size();
            int sum  = va[0];
            int avrg = va[1];
            int imax = va[2];
            sb.append("AverageLag: ").append(avrg).append("(ms) [").append(sz).append("] ");
            if (imax > -1) {
                final long mtime = list.get(imax).time;
                int max = list.get(imax).value;
                sb.append("MaxLag: ").append(max).append("(ms) at ").append(Strings.formatDateTime(mtime));
                if (showMillis) {
                    sb.append(" (").append(mtime).append(") ");
                }
                sb.append('\n');
            }

            sb.append("Total LagsDuration: ");
            Utils.getReadableDuration(sum, sb);

            //рассматриваемый период
            //более правильно брать именно заданные рамки
            long ts = startStampTime;
            long te = endStampTime;
            int diff = (int) (te - ts); //100%
            Utils.getReadableDuration(diff, sb.append(", Period:"));
            //sum x
            double p = sum * 100.0D / diff;
            sb.append(", LagPercent: ").append(String.format("%6.4f %%", p));
            return sb;
        }
    }
    /**
     * Получить сумму значений всех лагов, среднее значение (сумма на количество)
     * и индекс максимального лага в list
     *
     * @param list
     * @return
     */
    public static int[] getSummary(List<TShEntry> list) {
        int sum = 0;
        int max = 0;
        int imax =-1;
        final int sz = list.size();
        for (int i = 0; i < sz; i++) {
            TShEntry e = list.get(i);
            final int v = e.value;
            sum += v;
            if (v > max) {
                max = v;
                imax = i;
            }
        }
        int avrg = sz == 0 ? 0 : sum / sz;
        return new int[] {sum, avrg, imax};
    }

    /**
     * Сравнить величину и частоту лагов по дням для заданных часов
     *
     * Например для вявления изменения величины и частоты лагов по ночам.
     * Больше всего интересует период 0-4
     * С возможностью указать руками интересующий сравниваемый период
     * Смысл втом чтобы сравнить данные за разные дни в один временной промежуток
     * времени например с 0 до 4х утра
     *
     * Пример вывода (lags cd -s -01-12-21 -e -03-12-21 -nm  )
     * [Compare Lags] Full Observe Period: 00:00:00 01.12.21 - 00:00:00 03.12.21
     * Observed time period for each Day:  00:00:00 - 04:00:00  (4h)
     * 
     * # 01.12.21  00:00:00 - 04:00:00
     * AverageLag: 3538(ms) [Cnt:360] Max: 35884(ms) at 00:45:53 01.12.21
     * Total Lags Duration: 1273919ms (21m 13s 919ms), LagPercent: 8,8467 %
     *
     * # 02.12.21  00:00:00 - 04:00:00
     * AverageLag: 1212(ms) [Cnt:9] Max: 1392(ms) at 01:39:05 02.12.21
     * Total Lags Duration: 10916ms (10s 916ms), LagPercent: 0,0758 %

     * @param in
     * @param startTime начала всего промежутка сравнения по дням
     * @param endTime конец всего промежутка
     * @param showMillis
     * @param hs начальный час обработки данных
     * @param he конечный час
     * @return
     */
    public static Object getCompareDaysInHours(Path in, long startTime, long endTime, boolean showMillis, int hs, int he) {
        List<TShEntry> list = TShEntry.selectFromBin(in, startTime, endTime);
        if (list == null || list.isEmpty()) {
            return Utils.showPeriod("Emtpty for ", in, startTime, endTime, true);
        }
        else {
            StringBuilder sb = new StringBuilder("[Compare Lags] Full Observe Period: ");
            //Показать общий выбранный период времени для которого будет создано сравнение
            Utils.appendTimePeriod(sb, startTime, endTime, showMillis).append('\n');

            //Instant i = Instant.ofEpochMilli(startTime).;
            LocalDateTime t0 = Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
            final int y = t0.getYear();
            final int m = t0.getMonthValue();
            final int d = t0.getDayOfMonth();

            //время начала выборки значений для первого для из указанного промежутка времени
            LocalDateTime tfs = LocalDateTime.of(y, m, d, hs, 0, 0, 0);
            LocalDateTime tfe = LocalDateTime.of(y, m, d, he, 0, 0, 0);
            long mfs = tfs.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long mfe = tfe.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            List<TShEntry> tmp = new ArrayList<>();

            //для всех дней он будет одинаковый
            int period = (int)(mfe-mfs);
            //Наблюдаемый переод времени для каждого дня. Период для которого будут обрабатывается данные
            sb.append("Observed time period for each Day:  ");
            sb.append(hs < 10 ? '0' : "").append(hs).append(":00:00 - ").append(he < 10 ? '0' : "").append(he).append(":00:00  ");
            Utils.getReadableDuration(period, sb.append("(")).append(")\n\n");

            long ts = mfs;
            long te = mfe;
            boolean started = false;

            do {
                tmp.clear();
                TShEntry.select(list, ts, te, tmp);
                final int sz = tmp.size();
                //пропускаю с начала дни в для которых нед данных
                if (sz > 0 || started) {
                    started = true;

                    //отображаем Дату и временной интервал для текущего рассматриваемого дня
                    Utils.appendTimePeriod(sb.append("# "), ts, te, showMillis).append("\n");

                    int[] va = getSummary(tmp);
                    int sum  = va[0];
                    int avrg = va[1];
                    int imax = va[2];
                    int diff = (int) (te - ts);//100%
                    double p = sum * 100.0D / diff;

                    sb.append("AverageLag: ").append(avrg).append("(ms) [Cnt:").append(sz).append("] ");
                    if (imax>-1) {
                        final long mtime = tmp.get(imax).time;
                        int max = tmp.get(imax).value;
                        //максимальное значение лага (верхняя граница 65535 - выше обрезает
                        sb.append("Max: ").append(max).append("(ms) at ").append(Strings.formatDateTime(mtime));
                        if (showMillis) {
                            sb.append(" (").append(mtime).append(") ");
                        }
                        sb.append('\n');
                    }
                    //сумма времени в котором сервер был в лаге
                    sb.append("Total Lags Duration: ");
                    Utils.getReadableDuration(sum, sb);
                    sb.append(", LagPercent: ").append(String.format("%6.4f %%", p));
                    sb.append("\n\n");
                }

                final long step = 24*60*60000;
                ts += step;//следующий день
                te += step;

            } while(te <= endTime);

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
