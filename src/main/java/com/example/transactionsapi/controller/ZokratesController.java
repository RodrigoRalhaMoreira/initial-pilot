package com.example.transactionsapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.transactionsapi.zokrates.ZokratesService;

@RestController
@RequestMapping("/zokrates")
public class ZokratesController {

    private final ZokratesService zokratesService;

    public ZokratesController(ZokratesService zokratesService) {
        this.zokratesService = zokratesService;
    }

    @PostMapping("/compile")
    public ResponseEntity<String> compileCircuit() {
        try {
            String circuitFilePath = "/home/moreira/Desktop/faculdade/TESE/initial-pilot/transactionsapi/src/main/java/com/example/transactionsapi/zokrates/example.zok";
            zokratesService.compileCircuit(circuitFilePath);
            return ResponseEntity.ok("Zokrates compilation successful");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error compiling circuit: " + e.getMessage());
        }
    }
}