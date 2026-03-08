package at.sfischer.testing.systems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PayrollApplicationTest {

    @Test
    public void testStartUp() {
        PayrollApplication system = new PayrollApplication();
        boolean startResult = system.start();
        assertTrue(startResult);

        boolean stopResult = system.stop();
        assertTrue(stopResult);
    }
}
