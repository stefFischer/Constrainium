package at.sfischer.testing.systems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EMBNcsTest {

    @Test
    public void testStartUp() {
        EMBNcs system = new EMBNcs();
        boolean startResult = system.start();
        assertTrue(startResult);

        boolean stopResult = system.stop();
        assertTrue(stopResult);
    }
}
