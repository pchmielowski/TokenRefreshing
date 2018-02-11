package net.chmielowski.token;

import org.junit.Assert;
import org.junit.Test;

public class ExampleUnitTest {
    private boolean flag = false;

    @Test
    public void withException() {
        flag = false;
        try {
            foo(true);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        Assert.assertEquals(true, flag);
    }

    @Test
    public void withoutException() {
        flag = false;
        try {
            foo(false);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        Assert.assertEquals(true, flag);
    }

    private void foo(boolean shouldThrow) throws Exception {
        try {
            if (shouldThrow)
                throw new Exception("Original");
        } catch (Exception e) {
            throw e;
        } finally {
            flag = true;
        }
    }
}