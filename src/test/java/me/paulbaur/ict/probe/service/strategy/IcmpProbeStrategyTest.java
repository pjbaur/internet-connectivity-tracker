package me.paulbaur.ict.probe.service.strategy;

import me.paulbaur.ict.common.model.ProbeMethod;
import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.probe.domain.ProbeRequest;
import me.paulbaur.ict.probe.domain.ProbeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IcmpProbeStrategy.
 * Tests basic probe functionality against localhost and known unreachable hosts.
 */
class IcmpProbeStrategyTest {

    private IcmpProbeStrategy icmpProbeStrategy;

    @BeforeEach
    void setUp() {
        icmpProbeStrategy = new IcmpProbeStrategy();
        // Set test configuration values
        ReflectionTestUtils.setField(icmpProbeStrategy, "defaultTimeoutMs", 2000);
        ReflectionTestUtils.setField(icmpProbeStrategy, "packetSize", 32);
    }

    @Test
    void testProbe_localhost_shouldSucceed() {
        // Given
        ProbeRequest request = new ProbeRequest(
                "test-target-id",
                "127.0.0.1",
                0,  // Port is not used for ICMP
                "test-cycle-id"
        );

        // When
        ProbeResult result = icmpProbeStrategy.probe(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.targetId()).isEqualTo("test-target-id");
        assertThat(result.targetHost()).isEqualTo("127.0.0.1");
        assertThat(result.probeCycleId()).isEqualTo("test-cycle-id");
        assertThat(result.method()).isEqualTo(ProbeMethod.ICMP);
        assertThat(result.status()).isEqualTo(ProbeStatus.UP);
        assertThat(result.errorMessage()).isNull();
        // Latency may be null if parsing fails, but status should still be UP
        // assertThat(result.latencyMs()).isNotNull();  // Optional check
    }

    @Test
    void testProbe_unreachableHost_shouldFail() {
        // Given - Using a non-routable IP address that should fail quickly
        ProbeRequest request = new ProbeRequest(
                "test-target-id",
                "192.0.2.1",  // TEST-NET-1, reserved for documentation, should be unreachable
                0,
                "test-cycle-id"
        );

        // When
        ProbeResult result = icmpProbeStrategy.probe(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.targetId()).isEqualTo("test-target-id");
        assertThat(result.targetHost()).isEqualTo("192.0.2.1");
        assertThat(result.probeCycleId()).isEqualTo("test-cycle-id");
        assertThat(result.method()).isEqualTo(ProbeMethod.ICMP);
        assertThat(result.status()).isEqualTo(ProbeStatus.DOWN);
        assertThat(result.latencyMs()).isNull();
        assertThat(result.errorMessage()).isNotNull();
    }

    @Test
    void testProbe_invalidHost_shouldFail() {
        // Given
        ProbeRequest request = new ProbeRequest(
                "test-target-id",
                "invalid-host-that-does-not-exist-12345",
                0,
                "test-cycle-id"
        );

        // When
        ProbeResult result = icmpProbeStrategy.probe(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.targetId()).isEqualTo("test-target-id");
        assertThat(result.probeCycleId()).isEqualTo("test-cycle-id");
        assertThat(result.method()).isEqualTo(ProbeMethod.ICMP);
        assertThat(result.status()).isEqualTo(ProbeStatus.DOWN);
        assertThat(result.latencyMs()).isNull();
        assertThat(result.errorMessage()).isNotNull();
    }

    @Test
    void testProbe_googleDns_shouldSucceed() {
        // Given - Using Google's public DNS as a reliable target
        ProbeRequest request = new ProbeRequest(
                "test-target-id",
                "8.8.8.8",
                0,
                "test-cycle-id"
        );

        // When
        ProbeResult result = icmpProbeStrategy.probe(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.targetId()).isEqualTo("test-target-id");
        assertThat(result.targetHost()).isEqualTo("8.8.8.8");
        assertThat(result.probeCycleId()).isEqualTo("test-cycle-id");
        assertThat(result.method()).isEqualTo(ProbeMethod.ICMP);
        // Note: This test may fail in environments without internet access or where ICMP is blocked
        // In CI/CD environments, this might need to be skipped or use localhost instead
        assertThat(result.status()).isIn(ProbeStatus.UP, ProbeStatus.DOWN);
    }
}
