package org.swarg.mc.optistats;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
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