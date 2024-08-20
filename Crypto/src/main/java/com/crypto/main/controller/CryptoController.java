package com.crypto.main.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.crypto.main.service.CryptoService;

@RestController
@RequestMapping(produces = "application/json")
@CrossOrigin(origins = "*")
public class CryptoController {

	@Autowired
	CryptoService cryptoService;

	@GetMapping("/crypto/price")
	public String getLatestPrice() {
		return CryptoService.getLatestPrice();
	}
	
	@GetMapping("/crypto/trade")
	public String trade(@RequestParam int userId, @RequestParam String type, @RequestParam String symbol, @RequestParam int quantity) {
		return CryptoService.trade(userId, type, symbol, quantity);
	}

	@GetMapping("/crypto/user/crypto")
	public String getUserCrypto(@RequestParam int userId) {
		return CryptoService.getUserCrypto(userId);
	}

	@GetMapping("/crypto/user/order")
	public String getUserOrder(@RequestParam int userId) {
		return CryptoService.getUserOrder(userId);
	}	
}
