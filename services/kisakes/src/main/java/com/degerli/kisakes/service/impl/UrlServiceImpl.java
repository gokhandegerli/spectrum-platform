package com.degerli.kisakes.service.impl;

import com.degerli.kisakes.exception.UrlNotFoundException;
import com.degerli.kisakes.model.dto.UrlCreateRequest;
import com.degerli.kisakes.model.entity.ShortCodeLookup;
import com.degerli.kisakes.model.entity.Url;
import com.degerli.kisakes.repository.ShortCodeLookupRepository;
import com.degerli.kisakes.repository.UrlRepository;
import com.degerli.kisakes.service.UrlService;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlServiceImpl implements UrlService {

  private static final String CHARS
      = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int SHORT_CODE_LENGTH = 7;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UrlRepository urlRepository;
  private final ShortCodeLookupRepository shortCodeLookupRepository;

  @Override
  @Transactional
  public Url createShortUrl(UrlCreateRequest request) {
    String uniqueShortCode = generateUniqueShortCode();
    log.info("Creating short url with shortcode {}.", uniqueShortCode);

    shortCodeLookupRepository.save(new ShortCodeLookup(uniqueShortCode));

    Url url = new Url();
    url.setOriginalUrl(request.originalUrl());
    url.setShortCode(uniqueShortCode);

    return urlRepository.save(url);
  }

  @Override
  @Cacheable(value = "urls",
      key = "'url:' + #shortCode")
  public String getOriginalUrl(String shortCode) {
    log.info("Cache MISS: Veritabanından getiriliyor: {}", shortCode);
    Url url = urlRepository.findByShortCode(shortCode)
        .orElseThrow(() -> new UrlNotFoundException(shortCode));
    return url.getOriginalUrl();
  }

  private String generateUniqueShortCode() {
    String shortCode;
    do {
      shortCode = RANDOM.ints(SHORT_CODE_LENGTH, 0, CHARS.length())
          .mapToObj(CHARS::charAt)
          .map(Object::toString)
          .collect(Collectors.joining());
    }
    while (shortCodeLookupRepository.existsById(shortCode));
    return shortCode;
  }
}