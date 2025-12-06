package com.degerli.dummyservice;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@RequestMapping("/dummy-service")
public class DummyServiceApplication {

  private static final AtomicLong requestCounter = new AtomicLong(0);
  private static String instanceId;
  private static final List<String> quotes = Arrays.asList(
      "The only way to do great work is to love what you do.",
      "Innovation distinguishes between a leader and a follower.",
      "Stay hungry, stay foolish.",
      "Your time is limited, don't waste it living someone else's life.",
      "Design is not just what it looks like, design is how it works.");

  static {
    try {
      instanceId = InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      instanceId = "unknown";
    }
  }

  public static void main(String[] args) {
    SpringApplication.run(DummyServiceApplication.class, args);
  }

  @GetMapping("/hello")
  public Map<String, Object> hello() {
    return createResponse("Hello from Dummy Service!");
  }

  @GetMapping("/info")
  public Map<String, Object> getInfo() {
    Map<String, Object> response = createResponse("Dummy Service Information");
    response.put("version", "1.0.0");
    response.put("description", "A simple dummy microservice for testing");
    response.put("features", Arrays.asList("REST API", "Health Check", "Metrics"));
    return response;
  }

  @GetMapping("/quote")
  public Map<String, Object> getRandomQuote() {
    Random random = new Random();
    String quote = quotes.get(random.nextInt(quotes.size()));

    Map<String, Object> response = createResponse("Random Quote");
    response.put("quote", quote);
    return response;
  }

  @GetMapping("/data")
  public Map<String, Object> getData() {
    Map<String, Object> response = createResponse("Sample Data");

    // Sample data
    List<Map<String, Object>> items = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      Map<String, Object> item = new HashMap<>();
      item.put("id", i);
      item.put("name", "Item " + i);
      item.put("value", Math.random() * 100);
      items.add(item);
    }

    response.put("items", items);
    response.put("total", items.size());
    return response;
  }

  @PostMapping("/echo")
  public Map<String, Object> echo(
      @RequestBody
      Map<String, Object> payload) {
    Map<String, Object> response = createResponse("Echo Response");
    response.put("receivedPayload", payload);
    response.put("payloadSize", payload.size());
    return response;
  }

  @GetMapping("/slow")
  public Map<String, Object> slowEndpoint() {
    try {
      Thread.sleep(2000); // Simulate slow operation
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return createResponse("Slow response completed");
  }

  @GetMapping("/fast")
  public Map<String, Object> fastEndpoint() {
    return createResponse("Fast response");
  }

  @GetMapping("/status")
  public Map<String, Object> getStatus() {
    Map<String, Object> response = createResponse("Service Status");

    Runtime runtime = Runtime.getRuntime();
    response.put("memory", Map.of("total", runtime.totalMemory() / 1024 / 1024 + " MB", "free",
        runtime.freeMemory() / 1024 / 1024 + " MB", "used",
        (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 + " MB"));
    response.put("processors", runtime.availableProcessors());
    response.put("uptime", getUptime());

    return response;
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "instance", instanceId, "timestamp",
        LocalDateTime.now().toString());
  }

  private Map<String, Object> createResponse(String message) {
    Map<String, Object> response = new HashMap<>();
    response.put("service", "dummy-service");
    response.put("instance", instanceId);
    response.put("message", message);
    response.put("timestamp", LocalDateTime.now());
    response.put("requestCount", requestCounter.incrementAndGet());
    return response;
  }

  private String getUptime() {
    long uptimeMillis = System.currentTimeMillis() - startTime;
    long seconds = uptimeMillis / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    return String.format("%d hours, %d minutes, %d seconds", hours, minutes % 60,
        seconds % 60);
  }

  private static final long startTime = System.currentTimeMillis();
}