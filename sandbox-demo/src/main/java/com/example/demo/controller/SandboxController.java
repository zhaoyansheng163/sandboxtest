package com.example.demo.controller;

import com.example.demo.dto.SandboxReturnType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class SandboxController {

    @GetMapping("/test/void")
    public String testVoid() {
        error();
        return "success";
    }

    private void error() {
        throw new IllegalStateException("Illegal state!");
    }


    @GetMapping("/test/object")
    public SandboxReturnType testObject() {
        SandboxReturnType type = errorObject();
        log.info("type:{}", type);
        return type;
    }

    private SandboxReturnType errorObject() {
        throw new IllegalStateException("Illegal state!");
    }

}
