package com.example.diplomproject.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.NoSuchElementException;

@RestController
public class TestController {

    @GetMapping("/trigger-no-such-element")
    public void throwNoSuchElement() {
        throw new NoSuchElementException("Тестовое сообщение");
    }

    @GetMapping("/trigger-illegal-argument")
    public void throwIllegalArg() {
        throw new IllegalArgumentException("Illegal argument");
    }

    @GetMapping("/trigger-generic-exception")
    public void throwGeneric() {
        throw new RuntimeException("Unexpected");
    }
}