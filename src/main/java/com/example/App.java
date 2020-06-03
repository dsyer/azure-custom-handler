package com.example;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(proxyBeanMethods = false)
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}

@RestController
class TriggerController {

  @PostMapping("/trigger")
  public Map<String, Object> trigger(@RequestBody Map<String, Object> request){
    System.out.println("Java queue trigger handler: " + request);
    Map<String, Object> resp = new LinkedHashMap<String, Object>();
    String value = request.containsKey("value") ? (String) request.get("value") : "World";
    resp.put("log", "Java: test log1: " + request);
    resp.put("data", "Hello " + value);
    return resp;
  }

}