package at.sfischer.testing.systems;

import at.sfischer.testing.Arguments;
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

    @Test
    public void testStartUpWithAgent() {
        PayrollApplication system = new PayrollApplication();
        boolean startResult = system.start(Arguments.getOtelJavaAgentPath(), Arguments.OTEL_DEBUG_LOGS);
        assertTrue(startResult);

        boolean stopResult = system.stop();
        assertTrue(stopResult);
    }
}
