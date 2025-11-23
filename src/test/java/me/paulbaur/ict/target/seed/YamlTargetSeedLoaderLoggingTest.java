package me.paulbaur.ict.target.seed;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import me.paulbaur.ict.common.logging.LogCapture;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class YamlTargetSeedLoaderLoggingTest {

    @Test
    void emptySeedFile_logsResourceAndSchemaVersion() {
        YamlTargetSeedLoader loader = new YamlTargetSeedLoader("seed/targets-empty.yml");

        try (LogCapture capture = LogCapture.capture(YamlTargetSeedLoader.class, Level.INFO)) {
            List<TargetDefinition> seeds = loader.loadSeeds();
            assertThat(seeds).isEmpty();

            ILoggingEvent event = capture.firstMatching(e ->
                    e.getFormattedMessage().contains("Seed file 'seed/targets-empty.yml' is present but contains no targets")
            ).orElseThrow();

            Map<String, Object> args = LogCapture.structuredArguments(event);
            assertThat(args)
                    .containsEntry("resourcePath", "seed/targets-empty.yml")
                    .containsEntry("schemaVersion", 1);
        }
    }
}
