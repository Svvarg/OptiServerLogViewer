package org.swarg.mc.optistats;

import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.swarg.cmds.ArgsWrapper;
import org.swarg.common.RandomAccessFileManager;


/**
 * Простого сравнение некого кода
 * 05-12-21
 * @author Swarg
 */
public class BenchMarks {

    private static final String BM_USAGE =
            "append-bytes-to-file";

    public static Object cmd(ArgsWrapper w) {
        if (w.isHelpCmdOrNoArgs()) {
            return BM_USAGE;
        }
        Object ans = "?";
        if (w.isCmd("append-bytes-to-file", "ab2f")) {
            ans = doAppendBytes2File(w);
        }
        return ans;
    }

    /**
     * Сравнение примерной скорости записи при добавлении данных в виде набора байт
     * в конец бинарного файла
     * @param w
     * @return
     */
    private static Object doAppendBytes2File(ArgsWrapper w) {
        if (w.isHelpCmd()) {
            return "[-ds|--data-size N (Def:16k)] [-i|--iterations N (Def:100)] [-d|-dir path (Def:'tmp/')]\n"
                   +"Options for RandomAccessFileManager:\n"
                   +"[-fnf|-fm-noimmediate-flush] [-fbs|-filemanager-buff-size N (Def:256k)] ";
        }

        String dir0 = w.optValueOrDef("tmp/", "-d", "--dir");
        int iters  = w.optValueIntOrDef(100, "-i", "--iterations");
        int datasz = w.optValueIntOrDef(16*1024, "-ds", "--date-size");
        boolean deleteFile = !w.hasOpt("-nd", "-not-delete");

        //по умолчанию у RandomAccessFileManager внутренний буффер 256кб
        int fmBuffSize = w.optValueIntOrDef(-1, "-fbs", "--filemanager-buff-size");
        //флаг немедленной записи в файл переданных в файл-менеджер данных
        //если false - менеджер собирает из во внутреннем буффере и только при
        //его опустошении либо по вызову flush() пишет в файл.
        boolean isFlush = !w.hasOpt("-fnf", "--fm-notimmediate-flush");


        Path dir = Paths.get(dir0);
        try {
            Files.createDirectories(dir);
        }
        catch (Exception e) {
            return e;
        }

        byte[] data = new byte[datasz];
        Arrays.fill(data, (byte)88);

        StringBuilder sb = new StringBuilder();
        sb.append("[WARMUP] ");
        measuringAppendBytes2File(sb,dir, deleteFile, iters, data, isFlush, fmBuffSize);

        System.gc();

        try {
            Thread.sleep(3000);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        sb.append("\n[BENCH]  ");
        measuringAppendBytes2File(sb,dir, deleteFile, iters, data, isFlush, fmBuffSize);
        return sb;
    }

    /**
     * 
     * @param sb
     * @param dir
     * @param deleteFile
     * @param iters
     * @param data
     * @param isFlush
     * @param fmBuffSize
     */
    public static void measuringAppendBytes2File(StringBuilder sb, Path dir, boolean deleteFile, int iters, byte[] data,
            boolean isFlush, int fmBuffSize) {
        Path p1 = dir.resolve("FW.bin");
        Path p2 = dir.resolve("RAFM-1.bin");

        // ====  2 ==== \\
        boolean append = true;//добавление в конец файла
        String name0 = p2.toString();
        RandomAccessFileManager fm = null;

        long t21 = System.currentTimeMillis();
        try {
            fm = RandomAccessFileManager.createManager(name0, append, isFlush, fmBuffSize);
            for (int i = 0; i < iters; i++) {
                fm.writeByteArray(data, true);//сразу писать на диск
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        finally {
            if (fm != null) {
                fm.closeOutputStream();//close()
            }
        }
        long t22 = System.currentTimeMillis();
        // ====  2 ==== \\


        // ====  1 ==== \\
        long t11 = System.currentTimeMillis();
        try {
            for (int i = 0; i < iters; i++) {
                Files.write(p1, data,StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        long t12 = System.currentTimeMillis();
        // ====  1 ==== \\

        //Report
        long diff1 = t12-t11;
        long diff2 = t22-t21;
        int fmbyffsz = fm == null ? 0 : fm.getBufferSize();
        sb.append(String.format("Iterations : %d, OneWriteBytesLengths:%d  TmpDir: %s\n", iters, data.length, dir.toAbsolutePath()));
        sb.append(String.format("Files.write: %4d ms (ReOpen file for each write)\n", diff1)); //Переоткрывает файл для каждой записи
        sb.append(String.format("RAFManager : %4d ms (Open once, keep handle of open file) [InnerByteBuffSize: %s]\n", diff2, fmbyffsz));

        if (deleteFile) {
            try {
                Files.deleteIfExists(p1);
                Files.deleteIfExists(p2);
                sb.append("Files Deleted\n");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
