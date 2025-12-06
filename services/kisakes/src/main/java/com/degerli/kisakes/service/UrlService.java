package com.degerli.kisakes.service;


import com.degerli.kisakes.model.dto.UrlCreateRequest;
import com.degerli.kisakes.model.entity.Url;

public interface UrlService {

  Url createShortUrl(UrlCreateRequest request);

  String getOriginalUrl(String shortCode);
}