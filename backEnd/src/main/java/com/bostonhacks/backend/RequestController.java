package com.bostonhacks.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
public class RequestController {

  @GetMapping("/text")
  public String getText() {
    // todo
    return "";
  }

  @PostMapping("/upload-image")
  public String uploadImage(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {

  }

  @GetMapping("/image-advice")
  public String getImageAdvice(@RequestParam("")) {

  }
}
