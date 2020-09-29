package ca.taylorsoftware.javagenerator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class ThreadedGeneratorTest {

    //-------------------------------------------------------------------------
    private static class EmptyGenerator extends ThreadedGenerator<String> {
        @Override
        protected void generator() throws InterruptedException {
        }
    }

    @Test
    void testEmptyGenerator() {
        List<String> expected = new ArrayList<>();
        try (EmptyGenerator iter = new EmptyGenerator()) {
            assertIterableEquals(expected, iter);
        }

        try (EmptyGenerator iter = new EmptyGenerator()) {
            assertThat(iter, not(contains("one")));
        }

        try (EmptyGenerator iter = new EmptyGenerator()) {
            assertThat(iter, not(contains("two", "three")));
        }
    }



    //-------------------------------------------------------------------------
    private static class SingleGenerator extends ThreadedGenerator<String> {
        @Override
        protected void generator() throws InterruptedException {
            yieldReturn("one");
        }
    }

    @Test
    void testSingleGenerator() {
        List<String> expected = Arrays.asList("one");
        try (SingleGenerator iter = new SingleGenerator()) {
            assertIterableEquals(expected, iter);
        }

        try (SingleGenerator iter = new SingleGenerator()) {
            assertThat(iter, contains("one"));
        }

        try (SingleGenerator iter = new SingleGenerator()) {
            assertThat(iter, not(contains("one", "two")));
        }
    }



    //-------------------------------------------------------------------------
    private static class SimpleGenerator extends ThreadedGenerator<String> {
        @Override
        protected void generator() throws InterruptedException {
            yieldReturn("one");
            yieldReturn("two");
            yieldReturn("three");
        }
    }

    @Test
    void testSimpleGenerator() {
        List<String> expected = Arrays.asList("one", "two", "three");
        try (SimpleGenerator iter = new SimpleGenerator()) {
            assertIterableEquals(expected, iter);
        }

        try (SimpleGenerator iter = new SimpleGenerator()) {
            assertThat(iter, contains("one", "two", "three"));
        }

        try (SimpleGenerator iter = new SimpleGenerator()) {
            assertThat(iter, not(contains("one", "two", "three", "four")));
        }

        try (SimpleGenerator iter = new SimpleGenerator()) {
            assertThat(iter, not(contains("two", "three")));
        }
    }

    @Test
    void testSimplePrematureClose() {
        try (SimpleGenerator iter = new SimpleGenerator()) {
            int counter = 0;
            for (String it : iter) {
                SimpleGenerator.message("%s\n", it);
                ++counter;
                if (counter == 2) {
                    iter.close();
                }
            }
        }

    }



    //-------------------------------------------------------------------------
    private static class DelayedStartGenerator extends ThreadedGenerator<String> {
        @Override
        protected void generator() throws InterruptedException {
            message("DelayedStartGenerator() - sleep(1000)\n");
            Thread.sleep(1000);
            message("DelayedStartGenerator() - sleep done.\n");
            yieldReturn("one");
            yieldReturn("two");
            yieldReturn("three");
        }
    }

    @Test()
    void testDelayedStartGenerator() {
        List<String> expected = Arrays.asList("one", "two", "three");
        try (DelayedStartGenerator iter = new DelayedStartGenerator()) {
            assertIterableEquals(expected, iter);
        }

        try (DelayedStartGenerator iter = new DelayedStartGenerator()) {
            assertThat(iter, contains("one", "two", "three"));
        }

        try (DelayedStartGenerator iter = new DelayedStartGenerator()) {
            assertThat(iter, not(contains("one", "two", "three", "four")));
        }

        try (DelayedStartGenerator iter = new DelayedStartGenerator()) {
            assertThat(iter, not(contains("two", "three")));
        }
    }



    //-------------------------------------------------------------------------
    private static class DelayedFinishGenerator extends ThreadedGenerator<String> {
        @Override
        protected void generator() throws InterruptedException {
            yieldReturn("one");
            yieldReturn("two");
            yieldReturn("three");
            message("DelayedFinishGenerator() - sleep(1000)\n");
            Thread.sleep(1000);
            message("DelayedFinishGenerator() - sleep done.\n");
        }
    }

    @Test()
    void testDelayedFinishGenerator() {
        List<String> expected = Arrays.asList("one", "two", "three");
        try (DelayedFinishGenerator iter = new DelayedFinishGenerator()) {
            assertIterableEquals(expected, iter);
        }

        try (DelayedFinishGenerator iter = new DelayedFinishGenerator()) {
            assertThat(iter, contains("one", "two", "three"));
        }

        try (DelayedFinishGenerator iter = new DelayedFinishGenerator()) {
            assertThat(iter, not(contains("one", "two", "three", "four")));
        }

        try (DelayedFinishGenerator iter = new DelayedFinishGenerator()) {
            assertThat(iter, not(contains("two", "three")));
        }
    }



    //-------------------------------------------------------------------------
    private static class DelayedGenerator extends ThreadedGenerator<Integer> {
        volatile boolean isForegroundRunning = true;
        volatile boolean isBackgroundRunning = false;

        @Override
        protected void generator() throws InterruptedException {
            try {
                setBackgroundRunning(true);

                // Delay before the first call to 'yieldReturn(...)'.
                Thread.sleep(100);

                for (int counter = 0; counter < 3; ++counter) {
                    setBackgroundRunning(false);
                    yieldReturn(counter);
                    setBackgroundRunning(true);

                    Thread.sleep(100);
                }
                setBackgroundRunning(false);

            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        void setForegroundRunning(boolean isRunning) throws InterruptedException {
            if (isBackgroundRunning) {
                String msg = "The Background is running when NOT expected!";
                throw new InterruptedException(msg);
            }
            isForegroundRunning = isRunning;
        }

        void setBackgroundRunning(boolean isRunning) throws InterruptedException {
            if (isForegroundRunning) {
                String msg = "The Foreground is running when NOT expected!";
                throw new InterruptedException(msg);
            }
            isBackgroundRunning = isRunning;
        }
    }

    @Test
    void confirmTreadedGenerator() {
        Assertions.assertThrows(InterruptedException.class, () -> {
            try (DelayedGenerator iter = new DelayedGenerator()) {
                int counter = 0;
                iter.setForegroundRunning(false);
                for (Integer it : iter) {
                    iter.setForegroundRunning(true);

                    ++counter;
                    assertEquals(counter, it);
                    DelayedGenerator.message("%d\n", it);
                    Thread.sleep(100);

                    iter.setForegroundRunning(false);
                }
            }
        });
    }

}
