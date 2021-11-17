package org.swarg.mc.optistats.model;

import org.swarg.common.Binary;
import org.swarg.common.Strings;

/**
 * 17-11-21
 * @author Swarg
 */
public class LagEntry {
    public long time;
    public int lag;

    public LagEntry(byte[] a, int off) {
        this.time = Binary.readLong(a, off);
        this.lag = Binary.readUnsignedShort(a, off + 8);
    }
    
    public static int getSerializeSize() {
        return 8+2;
    }

    public StringBuilder appendTo(StringBuilder sb, int i, boolean showMillis) {
        String line;
        String fdt = Strings.formatDateTime(time);
        if (showMillis) {
            line = String.format("#% ,3d  %s (%d)   % ,6d", i, fdt, time, lag);
        } else {
            line = String.format("#% ,3d  %s   % ,6d", i, fdt, lag);
        }
        return sb.append(line);
    }
}
