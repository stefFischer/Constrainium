package at.sfischer.driver.rest;

import at.sfischer.driver.SystemDriverProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemDriverProviderTest {

    @Test
    void restDriverProviderIsDiscovered1() {
        List<SystemDriverProvider> providers = SystemDriverProvider.providers();

        assertThat(providers)
                .anyMatch(p -> p instanceof RestDriverProvider);
    }

    @Test
    void restDriverProviderIsDiscovered2() {
        SystemDriverProvider provider =
                SystemDriverProvider.getProvider(RestDriverProvider.DRIVER_NAME);

        assertThat(provider)
                .isInstanceOf(RestDriverProvider.class);
    }
}
