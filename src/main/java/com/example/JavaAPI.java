package com.example;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JavaAPI {

  @PostMapping("/AnotherTrigger")
  public InvokeResponse AnotherTriggerHandler(@RequestBody InvokeRequest request){
    System.out.println("Java queue trigger handler: " + request.Data);
    InvokeResponse resp = new InvokeResponse();
    resp.Logs.add("Java: test log1");
    resp.Logs.add("Java: test log2");
    resp.ReturnValue = "HelloWorld";
    return resp;
  }

  @RequestMapping(value = {"/SimpleHttpTrigger", "/SimpleHttpTriggerWithReturn"})
  public Map<String, String> SimpleHttpTrigger(){
    System.out.println("Java: Simple Http Trigger");    

    Map<String, String> dynOutput = new HashMap<String, String>();
    dynOutput.put("home", "123-456-789");
    dynOutput.put("office", "987-654-321");

    return dynOutput;
  }

}