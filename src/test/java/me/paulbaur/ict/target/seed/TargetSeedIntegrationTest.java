package me.paulbaur.ict.target.seed;

import me.paulbaur.ict.target.domain.Target;
import me.paulbaur.ict.target.manager.TargetManager;
import me.paulbaur.ict.target.store.TargetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.main.allow-bean-definition-overriding=true"
})
@Testcontainers
@Import(TargetSeedIntegrationTest.PostgresTargetRepositoryConfig.class)
class TargetSeedIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("ict.test.db.url", POSTGRES::getJdbcUrl);
        registry.add("ict.test.db.username", POSTGRES::getUsername);
        registry.add("ict.test.db.password", POSTGRES::getPassword);
    }

    @Autowired
    TargetRepository targetRepository;

    @Autowired
    TargetSeedLoader targetSeedLoader;

    @Autowired
    TargetManager targetManager;

    @MockBean
    me.paulbaur.ict.probe.service.ProbeScheduler probeScheduler;

    @Test
    void seedsLoadedIntoRepositoryAtStartup() {
        List<Target> targets = targetRepository.findAll();

        assertThat(targets).hasSize(2);
        assertThat(targets)
                .extracting(Target::getHost)
                .containsExactlyInAnyOrder("alpha.example.com", "beta.example.com");
    }

    @Test
    void seedingIsIdempotentAcrossReinitialization() {
        int initialCount = targetRepository.findAll().size();

        targetManager.initializeFromSeeds(targetSeedLoader.loadSeeds());

        List<Target> after = targetRepository.findAll();
        assertThat(after).hasSize(initialCount);
        assertThat(after)
                .extracting(Target::getHost)
                .containsExactlyInAnyOrder("alpha.example.com", "beta.example.com");
    }

    @TestConfiguration
    static class PostgresTargetRepositoryConfig {

        @Bean
        @Primary
        DataSource targetDataSource() {
            var dataSource = new org.postgresql.ds.PGSimpleDataSource();
            dataSource.setURL(POSTGRES.getJdbcUrl());
            dataSource.setUser(POSTGRES.getUsername());
            dataSource.setPassword(POSTGRES.getPassword());
            return dataSource;
        }

        @Bean
        @Primary
        TargetRepository targetRepository(DataSource dataSource) {
            return new PostgresTargetRepository(dataSource);
        }
    }

    /**
     * Minimal JDBC-backed repository for integration testing the seed flow.
     */
    static final class PostgresTargetRepository implements TargetRepository {
        private final DataSource dataSource;

        PostgresTargetRepository(DataSource dataSource) {
            this.dataSource = dataSource;
            initializeSchema();
        }

        @Override
        public List<Target> findAll() {
            String sql = "select id, label, host, port, enabled from targets order by host";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                List<Target> targets = new ArrayList<>();
                while (rs.next()) {
                    targets.add(mapRow(rs));
                }
                return targets;
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to fetch targets", e);
            }
        }

        @Override
        public Optional<Target> findById(UUID id) {
            String sql = "select id, label, host, port, enabled from targets where id = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setObject(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to fetch target by id " + id, e);
            }
        }

        @Override
        public Target save(Target target) {
            String sql = """
                    insert into targets (id, label, host, port, enabled)
                    values (?, ?, ?, ?, ?)
                    on conflict (id) do update
                    set label = excluded.label,
                        host = excluded.host,
                        port = excluded.port,
                        enabled = excluded.enabled
                    """;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setObject(1, target.getId());
                stmt.setString(2, target.getLabel());
                stmt.setString(3, target.getHost());
                stmt.setInt(4, target.getPort());
                stmt.setBoolean(5, target.isEnabled());
                stmt.executeUpdate();
                return target;
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to save target " + target, e);
            }
        }

        @Override
        public boolean delete(UUID id) {
            String sql = "delete from targets where id = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setObject(1, id);
                int rows = stmt.executeUpdate();
                return rows > 0;
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to delete target " + id, e);
            }
        }

        private void initializeSchema() {
            String ddl = """
                    create table if not exists targets (
                        id uuid primary key,
                        label varchar(255) not null,
                        host varchar(255) not null,
                        port integer not null,
                        enabled boolean not null,
                        constraint uq_host_port unique (host, port)
                    )
                    """;
            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement()) {
                stmt.execute(ddl);
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to initialize targets table", e);
            }
        }

        private Target mapRow(ResultSet rs) throws SQLException {
            UUID id = rs.getObject("id", UUID.class);
            String label = rs.getString("label");
            String host = rs.getString("host");
            int port = rs.getInt("port");
            boolean enabled = rs.getBoolean("enabled");
            return new Target(id, label, host, port, enabled);
        }
    }
}
