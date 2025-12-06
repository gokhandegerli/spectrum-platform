package com.degerli.kisakes.repository;

import com.degerli.kisakes.model.entity.Url;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UrlRepository extends JpaRepository<Url, UUID> {

  Optional<Url> findByShortCode(String shortCode);

}