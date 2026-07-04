package at.sfischer.testing.systems;

import at.sfischer.testing.Arguments;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
        Map<String, String> environmentVariables = Map.of("OTEL_SERVICE_NAME", "EMBNcs");
        EMBNcs system = new EMBNcs();
        boolean startResult = system.start(environmentVariables,
                Arguments.getOtelJavaAgentArgument(),
                Arguments.OTEL_DEBUG_TRUE,
                Arguments.OTEL_EXPORTER_LOGGING,
                Arguments.getServiceNameArgument("EMBNcs"),
                Arguments.getServiceNameAttributeArgument("EMBNcs")
        );
        assertTrue(startResult);

        boolean stopResult = system.stop();
        assertTrue(stopResult);
    }
}
