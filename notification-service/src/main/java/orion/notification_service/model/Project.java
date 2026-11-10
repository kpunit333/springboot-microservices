package orion.notification_service.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import orion.notification_service.util.StringListConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "secret_key", unique = true, nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID secretKey;

    @Column(name = "status", length = 15)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expire_at")
    private OffsetDateTime expireAt;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "code", length = 18, unique = true, nullable = false, updatable = false)
    private String code;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Convert(converter = StringListConverter.class)
    @Column(name = "channels", columnDefinition = "text")
    @Builder.Default
    private List<String> channels = new ArrayList<>();
}

