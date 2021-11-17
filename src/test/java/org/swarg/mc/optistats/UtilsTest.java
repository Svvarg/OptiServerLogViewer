package org.swarg.mc.optistats;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 17-11-21
 * @author Swarg
 */
public class UtilsTest {

    @Test
    public void test_isTimeInRange() {
        System.out.println("isTimeInRange");

        int cap = 10;
        long[] s = new long[cap];
        long[] e = new long[cap];
        long[] v = new long[cap];
        boolean[] ex = new boolean[cap];
        int n = 0;
        final boolean T = true;
        final boolean F = !T;
        v[n] = 2; s[n] = 0; e[n] = 5;   ex[n++] = T;//нет ограничений слева
        v[n] = 2; s[n] = 2; e[n] = 5;   ex[n++] = T;
        v[n] = 2; s[n] = 3; e[n] = 5;   ex[n++] = F;
        v[n] = 5; s[n] = 3; e[n] = 0;   ex[n++] = T;//справа нет ограничений
        v[n] = 5; s[n] = 0; e[n] = 0;   ex[n++] = T;//any
        v[n] = 8; s[n] = 3; e[n] = 7;   ex[n++] = F;
        v[n] = 1; s[n] = 3; e[n] = 7;   ex[n++] = F;

        for (int i = 0; i < n; i++) {
            boolean b = Utils.isTimeInRange(v[i], s[i], e[i]);
            assertEquals("line#"+i, ex[i], b);
        }
    }
}
