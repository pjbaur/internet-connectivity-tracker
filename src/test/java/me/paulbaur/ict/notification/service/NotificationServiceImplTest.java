package me.paulbaur.ict.notification.service;

import me.paulbaur.ict.common.model.ProbeStatus;
import me.paulbaur.ict.notification.domain.NotificationConfig;
import me.paulbaur.ict.notification.domain.NotificationPayload;
import me.paulbaur.ict.notification.provider.NotificationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationProvider notificationProvider;

    private NotificationServiceImpl notificationService;

    private static final String TEST_TARGET_ID = "test-target-123";
    private static final String TEST_HOST = "example.com";
    private static final String TEST_WEBHOOK_URL = "http://example.com/webhook";

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationProvider);
        ReflectionTestUtils.setField(notificationService, "notificationsEnabled", true);
    }

    @Test
    void sendNotification_whenNotificationsGloballyDisabled_skipsNotification() {
        // Arrange
        ReflectionTestUtils.setField(notificationService, "notificationsEnabled", false);
        NotificationConfig config = createConfig(true, true, true);
        NotificationPayload payload = createPayload(ProbeStatus.UP, ProbeStatus.DOWN);

        // Act
        notificationService.sendNotification(config, payload);

        // Assert
        verify(notificationProvider, never()).send(any(), any());
    }

    @Test
    void sendNotification_whenNotificationsDisabledForTarget_skipsNotification() {
        // Arrange
        NotificationConfig config = createConfig(false, true, true);
        NotificationPayload payload = createPayload(ProbeStatus.UP, ProbeStatus.DOWN);

        // Act
        notificationService.sendNotification(config, payload);

        // Assert
        verify(notificationProvider, never()).send(any(), any());
    }

    @Test
    void sendNotification_whenUpToDownAndConfigured_sendsNotification() {
        // Arrange
        NotificationConfig config = createConfig(true, true, false);
        NotificationPayload payload = createPayload(ProbeStatus.UP, ProbeStatus.DOWN);

        // Act
        notificationService.sendNotification(config, payload);

        // Assert
        verify(notificationProvider).send(config, payload);
    }

    @Test
    void sendNotification_whenDownToUpAndConfigured_sendsNotification() {
        // Arrange
        NotificationConfig config = createConfig(true, false, true);
        NotificationPayload payload = createPayload(ProbeStatus.DOWN, ProbeStatus.UP);

        // Act
        notificationService.sendNotification(config, payload);

        // Assert
        verify(notificationProvider).send(config, payload);
    }

    @Test
    void sendNotification_whenUpToDownButNotConfigured_skipsNotification() {
        // Arrange
        NotificationConfig config = createConfig(true, false, true);
        NotificationPayload payload = createPayload(ProbeStatus.UP, ProbeStatus.DOWN);

        // Act
        notificationService.sendNotification(config, payload);

        // Assert
        verify(notificationProvider, never()).send(any(), any());
    }

    @Test
    void sendNotification_whenDownToUpButNotConfigured_skipsNotification() {
        // Arrange
        NotificationConfig config = createConfig(true, true, false);
        NotificationPayload payload = createPayload(ProbeStatus.DOWN, ProbeStatus.UP);

        // Act
        notificationService.sendNotification(config, payload);

        // Assert
        verify(notificationProvider, never()).send(any(), any());
    }

    @Test
    void shouldNotify_whenNoStateChange_returnsFalse() {
        // Arrange
        NotificationConfig config = createConfig(true, true, true);
        NotificationPayload payload = createPayload(ProbeStatus.UP, ProbeStatus.UP);

        // Act
        boolean result = notificationService.shouldNotify(config, payload);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotify_whenUpToDownAndConfigured_returnsTrue() {
        // Arrange
        NotificationConfig config = createConfig(true, true, false);
        NotificationPayload payload = createPayload(ProbeStatus.UP, ProbeStatus.DOWN);

        // Act
        boolean result = notificationService.shouldNotify(config, payload);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotify_whenDownToUpAndConfigured_returnsTrue() {
        // Arrange
        NotificationConfig config = createConfig(true, false, true);
        NotificationPayload payload = createPayload(ProbeStatus.DOWN, ProbeStatus.UP);

        // Act
        boolean result = notificationService.shouldNotify(config, payload);

        // Assert
        assertThat(result).isTrue();
    }

    private NotificationConfig createConfig(boolean enabled, boolean notifyUpToDown, boolean notifyDownToUp) {
        return new NotificationConfig(
                TEST_TARGET_ID,
                enabled,
                TEST_WEBHOOK_URL,
                notifyUpToDown,
                notifyDownToUp,
                5000
        );
    }

    private NotificationPayload createPayload(ProbeStatus previous, ProbeStatus current) {
        return new NotificationPayload(
                TEST_TARGET_ID,
                TEST_HOST,
                Instant.now(),
                previous,
                current,
                current == ProbeStatus.DOWN ? "Connection failed" : null
        );
    }
}
