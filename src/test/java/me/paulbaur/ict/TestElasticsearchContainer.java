package me.paulbaur.ict;

import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

public class TestElasticsearchContainer {

    public static final ElasticsearchContainer ES =
            new ElasticsearchContainer(
                    DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.0")
            )
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false");

    static {
        ES.start();
    }
}
