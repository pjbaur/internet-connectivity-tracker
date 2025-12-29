package me.paulbaur.ict.common.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class ProbeSchedulerHealthIndicatorTest {

    private ProbeSchedulerHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new ProbeSchedulerHealthIndicator();
    }

    @Test
    void shouldReturnUpAfterRecentExecution() {
        healthIndicator.recordExecution();

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("lastExecutionTime");
        assertThat(health.getDetails()).containsKey("totalExecutions");
        assertThat(health.getDetails().get("totalExecutions")).isEqualTo(1L);
    }

    @Test
    void shouldTrackTotalExecutions() {
        healthIndicator.recordExecution();
        healthIndicator.recordExecution();
        healthIndicator.recordExecution();

        Health health = healthIndicator.health();

        assertThat(health.getDetails().get("totalExecutions")).isEqualTo(3L);
    }

    @Test
    void shouldTrackFailures() {
        healthIndicator.recordExecution();
        healthIndicator.recordFailure();
        healthIndicator.recordExecution();
        healthIndicator.recordFailure();

        Health health = healthIndicator.health();

        assertThat(health.getDetails().get("totalExecutions")).isEqualTo(2L);
        assertThat(health.getDetails().get("failedExecutions")).isEqualTo(2L);
    }

    @Test
    void shouldCalculateSuccessRate() {
        healthIndicator.recordExecution();
        healthIndicator.recordExecution();
        healthIndicator.recordExecution();
        healthIndicator.recordExecution();
        healthIndicator.recordFailure();

        Health health = healthIndicator.health();

        String successRate = (String) health.getDetails().get("successRate");
        // 4 successful executions, 1 failure out of 4 total executions = 75%
        assertThat(successRate).isEqualTo("75.00%");
    }
}
