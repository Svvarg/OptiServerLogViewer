package org.swarg.mc.optistats;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.jfree.data.time.Second;

/**
 * 16-11-21
 * @author Swarg
 */
public class Utils {
    
    public static final DateTimeFormatter DF_DATE = DateTimeFormatter.ofPattern("dd.MM.yy");
    public static final DateTimeFormatter DF_DATETIME = DateTimeFormatter.ofPattern("dd.MM.yy  HH:mm:ss");
    public static final DateTimeFormatter DF_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

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
     * Для JFreeChart из указанного времени получить инстанс Second
     * @param time
     * @return
     */
    public static Second getSecondOfMillis(long time) {
        //можно ли как-то создавать инстанс секунды без Instant?
        ZonedDateTime zt = Instant.ofEpochMilli(time).atZone(ZoneOffset.systemDefault());
        int s = zt.getSecond();
        int m = zt.getMinute();
        int h = zt.getHour();
        int d = zt.getDayOfMonth();
        int mm= zt.getMonthValue();
        int y = zt.getYear();
        return new Second(s, m, h, d, mm, y);
    }

    public static String getResourceAsString(String name) {
        try  {
            return new String(Files.readAllBytes(Paths.get(Thread.currentThread().getContextClassLoader().getResource(name).toURI())), StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
}
