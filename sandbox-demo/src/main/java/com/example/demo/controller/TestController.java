package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestController {
	@Value("${server.port}")
	private String serverPort;
	
	@RequestMapping(value = "/test", method = RequestMethod.GET	)
	public String test() {
		System.out.println("This is ServerProvider1 port:"  + serverPort);
		log.info("-----");
		return "ok";
	}

}
