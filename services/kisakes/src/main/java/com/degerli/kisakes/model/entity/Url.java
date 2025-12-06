package com.degerli.kisakes.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "urls")
@Getter
@Setter
@SQLDelete(sql = "UPDATE urls SET deleted = true, version = version + 1 WHERE id = ? AND "
    + "version = ?")
@SQLRestriction("deleted = false")
public class Url extends BaseEntity {

  @Column(name = "short_code",
      nullable = false,
      unique = true,
      length = 10)
  private String shortCode;

  @Column(name = "original_url",
      nullable = false,
      length = 2048)
  private String originalUrl;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "click_count",
      nullable = false)
  private Long clickCount = 0L;

}