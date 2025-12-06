package com.degerli.kisakes.model.entity;

import com.degerli.kisakes.model.Versioned;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity implements Versioned {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID",
      strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "id",
      updatable = false,
      nullable = false)
  private UUID id;

  @Version
  @Column(name = "version")
  private Integer version;

  @Column(name = "created_at",
      nullable = false,
      updatable = false)
  private Instant createdAt;

  @Column(name = "deleted",
      nullable = false)
  private boolean deleted = false;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      this.createdAt = Instant.now();
    }
  }
}