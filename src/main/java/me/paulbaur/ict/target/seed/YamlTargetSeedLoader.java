package me.paulbaur.ict.target.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * Loads target seeds from a classpath YAML file.
 */
@Service
@Slf4j
public class YamlTargetSeedLoader implements TargetSeedLoader {

    static final String DEFAULT_RESOURCE = "targets.yml";

    private final ObjectMapper yamlMapper;
    private final String resourcePath;

    public YamlTargetSeedLoader() {
        this(DEFAULT_RESOURCE);
    }

    YamlTargetSeedLoader(String resourcePath) {
        this(resourcePath, new ObjectMapper(new YAMLFactory()));
    }

    YamlTargetSeedLoader(String resourcePath, ObjectMapper yamlMapper) {
        this.resourcePath = Objects.requireNonNull(resourcePath, "resourcePath must not be null");
        this.yamlMapper = Objects.requireNonNull(yamlMapper, "yamlMapper must not be null");
        this.yamlMapper.findAndRegisterModules();
    }

    @Override
    public List<TargetDefinition> loadSeeds() {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new TargetSeedException("Seed file not found on classpath: " + resourcePath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            TargetSeedProperties properties = yamlMapper.readValue(inputStream, TargetSeedProperties.class);
            List<TargetDefinition> targets = properties.targets();
            int schemaVersion = properties.schemaVersion();

            if (targets.isEmpty()) {
                log.warn("Seed file '{}' is present but contains no targets", resourcePath);
            } else {
                log.info("Loaded {} target seeds (schemaVersion={}) from {}", targets.size(), schemaVersion, resourcePath);
            }

            return targets;
        } catch (IOException e) {
            throw new TargetSeedException("Failed to read seed file '" + resourcePath + "'", e);
        } catch (RuntimeException e) {
            throw new TargetSeedException("Invalid target seed data in '" + resourcePath + "'", e);
        }
    }
}
