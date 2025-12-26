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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
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

  // SELF-INJECTION: Proxy'nin devreye girmesi için kendisini inject ediyoruz.
  // @Lazy kullanıyoruz ki Circular Dependency hatası almayalım.
  @Autowired
  @Lazy
  private UrlServiceImpl self;

  @Override
  @Transactional
  public Url createShortUrl(UrlCreateRequest request) {
    String uniqueShortCode = generateUniqueShortCode();
    log.info("Creating short url with shortcode {}.", uniqueShortCode);

    shortCodeLookupRepository.save(new ShortCodeLookup(uniqueShortCode));

    Url url = new Url();
    url.setOriginalUrl(request.originalUrl());
    url.setShortCode(uniqueShortCode);
    url.setClickCount(0L);

    return urlRepository.save(url);
  }

  @Override
  public String getOriginalUrl(String shortCode) {
    // 1. URL'i getir (Cache mekanizması burada çalışır)
    // 'this' yerine 'self' kullanıyoruz ki @Cacheable devreye girsin.
    String originalUrl = self.findUrlCached(shortCode);

    // 2. Sayacı artır (Asenkron olarak arka planda çalışır)
    // Kullanıcı redirect olurken biz arkada DB güncelliyoruz.
    try {
      self.incrementClickCount(shortCode);
    } catch (Exception e) {
      log.error("Error incrementing click count for {}: {}", shortCode, e.getMessage());
      // Sayaç hatası redirect'i engellememeli, o yüzden try-catch
    }

    return originalUrl;
  }

  // --- YARDIMCI METODLAR (PUBLIC OLMALI) ---

  @Cacheable(value = "urls", key = "'url:' + #shortCode")
  public String findUrlCached(String shortCode) {
    log.info("Cache MISS: Veritabanından getiriliyor: {}", shortCode);
    Url url = urlRepository.findByShortCode(shortCode)
        .orElseThrow(() -> new UrlNotFoundException(shortCode));
    return url.getOriginalUrl();
  }

  @Async // <-- Bu metod ayrı bir thread'de çalışır
  @Transactional
  public void incrementClickCount(String shortCode) {
    urlRepository.findByShortCode(shortCode).ifPresent(url -> {
      url.setClickCount(url.getClickCount() + 1);
      urlRepository.save(url);
      log.debug("Async click count incremented for: {}", shortCode);
    });
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