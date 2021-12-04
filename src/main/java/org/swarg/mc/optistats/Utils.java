package org.swarg.mc.optistats;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 16-11-21
 * @author Swarg
 */
public class Utils {
    
    public static final DateTimeFormatter DF_DATE = DateTimeFormatter.ofPattern("dd.MM.yy");
    public static final DateTimeFormatter DF_DATETIME = DateTimeFormatter.ofPattern("dd.MM.yy  HH:mm:ss");
    public static final DateTimeFormatter DF_TIME   = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yy");

    public static long getStartTimeOfCurentDay() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long getStartTimeOfNextDay() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) + 1);
        return c.getTimeInMillis();
    }

    /**
     * Проверка вхождение точки времени (millis) tval в заданный промежуток
     * между точками s и e
     * Если точки s e равны нулю -
     * @param s если меньше=0 все
     * @param e если меньше=0 то нет ограничения справа
     * @param tval
     * @return
     */
    public static boolean isTimeInRange(long tval, long s, long e) {
        boolean left = s > 0 ? tval >= s : true;
        boolean right = e > 0 ? tval <= e : true;
        return left && right;
    }


    /**
     * Для графика форматированное время начала и конца временного отрезка
     * по значениям которых построен график
     * @param start
     * @param end
     * @return 
     */
    public static String getFormatedTimeInterval(long start, long end) {
        ZoneId zid = ZoneOffset.systemDefault();
        ZonedDateTime zts = Instant.ofEpochMilli(start).atZone(zid);
        ZonedDateTime zte = Instant.ofEpochMilli(end).atZone(zid);
        return zts.format(Utils.DF_DATETIME) + " - " +
                zte.format((zts.getDayOfYear() == zte.getDayOfYear()
                        ? Utils.DF_TIME : Utils.DF_DATETIME));
    }


    /**
     * Прочесть только заданный участок в файле
     * @param file
     * @param position
     * @param buff куда помещать прочитанное
     * @return 
     */
    public static int readBinBytes(Path file, long position, byte[] buff) {
        if (file != null && position >= 0 && buff != null && buff.length > 0 ) {
            try {
                File aFile = file.toFile();
                final long filesz = aFile.length();
                int len = buff.length;
                if (position + len > filesz) {
                    return -1;
                }
                RandomAccessFile raf = new RandomAccessFile(aFile, "rw");
                try {
                    raf.seek(position);
                    return raf.read(buff, 0, len);
                } finally {
                    raf.close();
                }
            } catch (IOException e) {
            }
        }
        return -1;
    }


    /**
     * TODO
     * @param file
     * @param position
     * @param len
     * @return
     */
    public static ByteBuffer readBinBytesNIO(Path file, long position, int len) {
        if (file != null && position >= 0 && len > 0) {
            try {
                final long filesz = Files.size(file);
                if (position + len > filesz) {
                    return null;
                }
                FileChannel fc = FileChannel.open(file, StandardOpenOption.READ);
                fc.position(position);
                ByteBuffer buffer = ByteBuffer.allocate(len);
                do {
                    fc.read(buffer);
                } while (buffer.hasRemaining());

                return buffer;
            }
            catch (IOException e) {
            }
        }
        return null;
    }
    
    /**
     * Единое для всех отображение в случаях когда запрашиваемая область данных
     * пуста. отобразить переод выборки + откуда она была совершения + сообщение
     * @param msg
     * @param in
     * @param ts
     * @param te
     * @param showMillis
     * @return
     */
    public static String showPeriod(String msg, Path in, long ts, long te, boolean showMillis) {
        StringBuilder sb = new StringBuilder();
        if (msg != null && !msg.isEmpty()) {
            sb.append(msg).append(' ');
        }
        sb.append(in);
        if (in != null) {
            sb.append(Files.exists(in) ? " [Exists]" : " [NotFound]");
        }
        sb.append("\nIn Time Period: ");
        appendTimePeriod(sb, ts,te, showMillis);
        return sb.toString();
    }
    /**
     * Время в милли в удобочитаемый вид
     * 12-11-21 00:00:00 - 04:00:00
     * 00:00:00 12-11-21 - 22:00:00 14-11-21
     * @param sb
     * @param ts
     * @param te
     * @param showMillis
     * @return
     */
    public static StringBuilder appendTimePeriod(StringBuilder sb, long ts, long te, boolean showMillis) {
        ZonedDateTime zdts = Instant.ofEpochMilli( ts ).atZone(ZoneId.systemDefault());
        ZonedDateTime zdte = Instant.ofEpochMilli( te ).atZone(ZoneId.systemDefault());

        //в пределах одного дня - сначала дату потом время начала - время конца
        // 12-11-21 00:00:00 - 04:00:00
        if (zdts.getDayOfMonth() == zdte.getDayOfMonth()) {
            sb.append( zdts.format(Utils.DF_DATETIME) );
            if (showMillis) {
                sb.append(" (").append(ts).append(')');
            }
            sb.append(" - ");
            sb.append(zdte.format(Utils.DF_TIME));
            if (showMillis) {
                sb.append(" (").append(te).append(')');
            }
        } 
        //00:00:00 12-11-21 - 22:00:00 14-11-21
        else {
            sb.append( zdts.format(DT_FORMAT) );
            if (showMillis) {
                sb.append(" (").append(ts).append(')');
            }
            sb.append(" - ");
            sb.append( zdte.format(DT_FORMAT) );
            if (showMillis) {
                sb.append(" (").append(te).append(')');
            }
        }
        return sb;
    }

    /**
     * Перевести количество миллисекунд в читаемы вид
     * часы, минуты, секунды, миллисекунды
     * 1273919ms => (21m 13s 919ms)
     * не показывать нулевые единицы (например 4h, а не 4h 0m 0s 0ms)
     * @param d
     * @param sb
     * @return
     */
    public static StringBuilder getReadableDuration(int d, StringBuilder sb) {
        boolean f = false;
        if (d < 3600_000) {
            sb.append(d).append("ms ");
            f = true;
        }
        if (d > 1000) {
            if (f) {
                sb.append('(');
            }
            int h = d / 3600000;
            int m = ( d - (h * 3600_000) ) /60_000;
            int s = (d - (h * 3600_000) - (m*60_000)) /1000;
            int ms = (d - (h * 3600_000) - (m * 60_000) - s * 1000);
            boolean prev = false;
            if (h > 0) {
                if (prev) sb.append(' ');
                sb.append(h).append('h');
                prev = true;
            }
            if (m > 0) {
                if (prev) sb.append(' ');
                sb.append(m).append('m');
                prev = true;
            }
            if (s > 0) {
                if (prev) sb.append(' ');
                sb.append(s).append('s');
                prev = true;
            }
            if (ms > 0) {
                if (prev) sb.append(' ');
                sb.append(ms).append("ms");
                prev = true;
            }
            if (f) {
                sb.append(')');
            }

        }
        return sb;
    }
}
