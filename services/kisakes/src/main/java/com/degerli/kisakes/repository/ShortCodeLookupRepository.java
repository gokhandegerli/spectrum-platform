package com.degerli.kisakes.repository;

import com.degerli.kisakes.model.entity.ShortCodeLookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShortCodeLookupRepository extends JpaRepository<ShortCodeLookup, String> {
  // JpaRepository<Object, String> kullanıyoruz çünkü bu tabloya karşılık gelen
  // tam bir Entity sınıfımız yok, sadece ID'si (short_code) ile ilgileniyoruz.
  // existsById metodu bizim için yeterli.
}