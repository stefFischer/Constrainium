package at.sfischer.generator.random;

import at.sfischer.generator.InputGeneratorProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InputGeneratorProviderTest {

    @Test
    void restDriverProviderIsDiscovered1() {
        List<InputGeneratorProvider> providers = InputGeneratorProvider.providers();

        assertThat(providers)
                .anyMatch(p -> p instanceof RandomGeneratorProvider);
    }

    @Test
    void restDriverProviderIsDiscovered2() {
        InputGeneratorProvider provider =
                InputGeneratorProvider.getProvider(RandomGeneratorProvider.GENERATOR_NAME);

        assertThat(provider)
                .isInstanceOf(RandomGeneratorProvider.class);
    }
}
