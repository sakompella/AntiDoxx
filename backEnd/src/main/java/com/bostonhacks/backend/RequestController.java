package com.bostonhacks.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RequestController {
  public final static String API_KEY = "AIzaSyD6ev0oDBpe7_424-Jqkl4hZiJVx6cj4-o";

  @GetMapping("/text")
  public String getText() {
    // todo
    return "";
  }

  @GetMapping("/imageAdvice")
  public String getImage(@RequestParam("")) {

  }
}
