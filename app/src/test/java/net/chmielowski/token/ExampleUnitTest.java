package net.chmielowski.token;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals("Some text", transform("Some text (more text)"));
    }

    private String transform(String input) {
        return input.split(" \\(")[0];
    }
}