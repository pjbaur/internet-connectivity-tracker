package me.paulbaur.ict.target.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Loads target seeds from a classpath YAML file.
 */
@Service
@Slf4j
public class YamlTargetSeedLoader implements TargetSeedLoader {

    static final String DEFAULT_RESOURCE = "targets.yml";

    private final ObjectMapper yamlMapper;

    public YamlTargetSeedLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.findAndRegisterModules();
    }

    @Override
    public List<TargetDefinition> loadSeeds() {
        ClassPathResource resource = new ClassPathResource(DEFAULT_RESOURCE);
        if (!resource.exists()) {
            throw new TargetSeedException("Seed file not found on classpath: " + DEFAULT_RESOURCE);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            TargetSeedProperties properties = yamlMapper.readValue(inputStream, TargetSeedProperties.class);
            List<TargetDefinition> targets = properties.targets();

            if (targets.isEmpty()) {
                log.warn("Seed file '{}' is present but contains no targets", DEFAULT_RESOURCE);
            } else {
                log.info("Loaded {} target seeds from {}", targets.size(), DEFAULT_RESOURCE);
            }

            return targets;
        } catch (IOException e) {
            throw new TargetSeedException("Failed to read seed file '" + DEFAULT_RESOURCE + "'", e);
        } catch (RuntimeException e) {
            throw new TargetSeedException("Invalid target seed data in '" + DEFAULT_RESOURCE + "'", e);
        }
    }
}
