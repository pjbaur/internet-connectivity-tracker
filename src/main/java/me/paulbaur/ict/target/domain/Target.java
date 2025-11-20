package me.paulbaur.ict.target.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A network target that is probed for connectivity; represents a host/port combination monitored by the system")
public class Target {

    @Schema(description = "Unique identifier of the target (UUID)", example = "00000000-0000-0000-0000-000000000000")
    private UUID id;

    @Schema(description = "Human-readable label for the target", example = "example.org")
    private String label;

    @Schema(description = "Hostname or IP address of the target", example = "93.184.216.34")
    private String host;

    @Schema(description = "Port number for the target service", example = "80")
    private int port;

    @Schema(description = "Is the targeet enabled?", example = "true")
    private boolean enabled;

    public Target(UUID id, String label, String host, int port) {
        this.id = id;
        this.label = label;
        this.host = host;
        this.port = port;
        this.enabled = true;
    }

    public Target(UUID id, String label, String host, int port, boolean enabled) {
        this.id = id;
        this.label = label;
        this.host = host;
        this.port = port;
        this.enabled = enabled;
    }

    public UUID getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Target target = (Target) o;
        return port == target.port && Objects.equals(id, target.id) && Objects.equals(host, target.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, host, port);
    }

    @Override
    public String toString() {
        return "Target{" +
                "id=" + id +
                ", label='" + label + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", enabled=" + enabled +
                '}';
    }
}
