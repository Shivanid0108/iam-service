package com.shivani.iamservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

	@GetMapping("/api/admin/ping")
	public String ping() {
		return "admin ok";
	}

}
