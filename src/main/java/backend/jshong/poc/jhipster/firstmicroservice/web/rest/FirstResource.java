package backend.jshong.poc.jhipster.firstmicroservice.web.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/first")
@Slf4j
@RequiredArgsConstructor
public class FirstResource {

    private final RestTemplate restTemplate;

    @GetMapping("/hello")
    public String hello(@RequestHeader("X-XSRF-TOKEN") String token) {
        log.info("Hello from FirstResource");
        String result = "Hello from FirstResource";
        try {
            // Second 마이크로서비스의 API를 내부 호출
            String url = "http://localhost:8082/api/second/hello";

            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "*/*");
            headers.set("X-XSRF-TOKEN", token);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            result = response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Handle HTTP errors
            log.error("HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            // Handle connection errors
            log.error("Connection error: " + e.getMessage());
        } catch (Exception e) {
            // Handle other errors
            log.error("Error: " + e.getMessage());
        }
        return result;
    }
}
