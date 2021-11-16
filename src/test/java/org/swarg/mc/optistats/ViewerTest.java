package org.swarg.mc.optistats;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 16-11-21
 * @author Swarg
 */
public class ViewerTest
{

    public ViewerTest() {
    }

    @Test
    public void test_getStartTimeOfCurentDay() {
        System.out.println("getStartTimeOfCurentDay");

        long t = Viewer.getStartTimeOfCurentDay();
    }
}
