package at.sfischer.testing.systems;

import at.sfischer.testing.Arguments;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
        Map<String, String> environmentVariables = Map.of("OTEL_SERVICE_NAME", "PayrollApplication");
        PayrollApplication system = new PayrollApplication();
        boolean startResult = system.start(environmentVariables,
                Arguments.getOtelJavaAgentArgument(),
                Arguments.OTEL_DEBUG_TRUE,
                Arguments.OTEL_EXPORTER_LOGGING,
                Arguments.getServiceNameArgument("PayrollApplication"),
                Arguments.getServiceNameAttributeArgument("PayrollApplication")
        );
        assertTrue(startResult);

        boolean stopResult = system.stop();
        assertTrue(stopResult);
    }
}
