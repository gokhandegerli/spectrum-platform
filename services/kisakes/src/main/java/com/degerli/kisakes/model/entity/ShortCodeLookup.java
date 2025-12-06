package com.degerli.kisakes.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "short_code_lookup")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortCodeLookup {

  @Id
  private String shortCode;

}