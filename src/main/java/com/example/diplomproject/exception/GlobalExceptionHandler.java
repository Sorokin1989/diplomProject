package com.example.diplomproject.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log= LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(NoSuchElementException ex, Model model) {
        log.warn("Resource not found: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "pages/errors/404";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String handleBadRequest(RuntimeException ex, Model model) {
        log.warn("Bad request: {}", ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "pages/errors/400";
    }
    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResourceFound(NoResourceFoundException ex, Model model) {
        log.debug("Static resource not found: {}", ex.getResourcePath());
        model.addAttribute("error", "Запрашиваемый ресурс не найден");
        return "pages/errors/404";  // можно использовать вашу существующую страницу 404
    }
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Unexpected error", ex);
        model.addAttribute("error", "Произошла непредвиденная ошибка");
        return "pages/errors/500";
    }
}