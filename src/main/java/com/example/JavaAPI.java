package com.example;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JavaAPI {

  @PostMapping("/trigger")
  public InvokeResponse AnotherTriggerHandler(@RequestBody InvokeRequest request){
    System.out.println("Java queue trigger handler: " + request.Data);
    InvokeResponse resp = new InvokeResponse();
    resp.Logs.add("Java: test log1");
    resp.Logs.add("Java: test log2");
    String value = request.Data.containsKey("Value") ? (String) request.Data.get("Value") : "World";
    resp.ReturnValue = "Hello " + value;
    return resp;
  }

}