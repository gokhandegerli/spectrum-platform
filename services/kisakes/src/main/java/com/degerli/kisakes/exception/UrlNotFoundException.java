package com.degerli.kisakes.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UrlNotFoundException extends RuntimeException {

  public UrlNotFoundException(String shortCode) {
    super("No URL found for the short code: " + shortCode);
  }
}