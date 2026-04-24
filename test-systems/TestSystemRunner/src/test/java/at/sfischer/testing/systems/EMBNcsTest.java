package at.sfischer.testing.systems;

import at.sfischer.testing.Arguments;
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

    @Test
    public void testStartUpWithAgent() {
        EMBNcs system = new EMBNcs();
        boolean startResult = system.start(Arguments.getOtelJavaAgentPath(), Arguments.OTEL_DEBUG_LOGS);
        assertTrue(startResult);

        boolean stopResult = system.stop();
        assertTrue(stopResult);
    }
}
