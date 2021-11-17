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

/**
 * 16-11-21
 * @author Swarg
 */
public class Utils {

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
