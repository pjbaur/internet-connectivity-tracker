package me.paulbaur.ict.target.seed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class YamlTargetSeedLoaderTest {

    @Test
    void loadSeeds_parsesTargetsAndSchemaVersion(CapturedOutput output) {
        YamlTargetSeedLoader loader = new YamlTargetSeedLoader("seed/targets-valid.yml");

        List<TargetDefinition> seeds = loader.loadSeeds();

        assertThat(seeds).hasSize(2);
        assertThat(seeds.get(0)).satisfies(seed -> {
            assertThat(seed.label()).isEqualTo("Seed One");
            assertThat(seed.host()).isEqualTo("one.example.com");
            assertThat(seed.port()).isEqualTo(80);
            assertThat(seed.method()).isEqualTo("TCP");
        });
        assertThat(seeds.get(1)).satisfies(seed -> {
            assertThat(seed.label()).isEqualTo("Seed Two");
            assertThat(seed.host()).isEqualTo("two.example.com");
            assertThat(seed.port()).isEqualTo(53);
        });

        assertThat(output.getOut()).contains("schemaVersion=1").contains("seed/targets-valid.yml");
    }

    @Test
    void loadSeeds_missingResource_throwsTargetSeedException() {
        YamlTargetSeedLoader loader = new YamlTargetSeedLoader("seed/missing-targets.yml");

        assertThatThrownBy(loader::loadSeeds)
                .isInstanceOf(TargetSeedException.class)
                .hasMessageContaining("Seed file not found");
    }

    @Test
    void loadSeeds_malformedYaml_throwsTargetSeedException() {
        YamlTargetSeedLoader loader = new YamlTargetSeedLoader("seed/targets-malformed.yml");

        assertThatThrownBy(loader::loadSeeds)
                .isInstanceOf(TargetSeedException.class)
                .hasMessageContaining("Failed to read seed file");
    }
}
