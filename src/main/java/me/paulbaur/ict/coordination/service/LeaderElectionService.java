package me.paulbaur.ict.coordination.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for leader election in a multi-node deployment.
 * Uses distributed locks to ensure only one instance acts as the leader.
 * The leader is responsible for scheduling probes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "ict.coordination.leader-election.enabled", havingValue = "true", matchIfMissing = false)
public class LeaderElectionService {

    private static final String LEADER_LOCK_KEY = "ict:leader-election:lock";

    private final DistributedLockService lockService;

    @Value("${ict.coordination.leader-election.lease-time-seconds:30}")
    private int leaseTimeSeconds;

    private final String nodeId = UUID.randomUUID().toString();
    private volatile boolean isLeader = false;

    @PostConstruct
    public void initialize() {
        log.info("Leader election service initialized", kv("nodeId", nodeId));
        attemptLeaderElection();
    }

    /**
     * Periodically attempt to become leader or renew leadership.
     * Runs every half of the lease time to ensure leadership is maintained.
     */
    @Scheduled(fixedDelayString = "#{${ict.coordination.leader-election.lease-time-seconds:30} * 500}", initialDelay = 5000)
    public void maintainLeadership() {
        attemptLeaderElection();
    }

    /**
     * Attempt to acquire or maintain leadership.
     */
    private void attemptLeaderElection() {
        boolean acquired = lockService.tryLock(
                LEADER_LOCK_KEY,
                0,  // Don't wait if lock is not available
                leaseTimeSeconds,
                TimeUnit.SECONDS
        );

        if (acquired && !isLeader) {
            isLeader = true;
            log.info("Node elected as leader", kv("nodeId", nodeId));
        } else if (!acquired && isLeader) {
            isLeader = false;
            log.info("Node lost leadership", kv("nodeId", nodeId));
        }
    }

    /**
     * Check if this node is currently the leader.
     *
     * @return true if this node is the leader
     */
    public boolean isLeader() {
        return isLeader;
    }

    /**
     * Get the node ID of this instance.
     *
     * @return the node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    @PreDestroy
    public void cleanup() {
        if (isLeader) {
            log.info("Releasing leadership on shutdown", kv("nodeId", nodeId));
            lockService.unlock(LEADER_LOCK_KEY);
            isLeader = false;
        }
    }
}
