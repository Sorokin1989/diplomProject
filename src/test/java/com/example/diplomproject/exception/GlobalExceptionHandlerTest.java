package com.example.diplomproject.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest(controllers = TestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void handleNotFound_shouldReturn404Page() throws Exception {
        mockMvc.perform(get("/trigger-no-such-element"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/errors/404"))
                .andExpect(model().attribute("error", "Тестовое сообщение"));
    }

    @Test
    void handleBadRequest_shouldReturn400Page() throws Exception {
        mockMvc.perform(get("/trigger-illegal-argument"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/errors/400"))
                .andExpect(model().attribute("error", "Illegal argument"));
    }

    @Test
    void handleGenericException_shouldReturn500Page() throws Exception {
        mockMvc.perform(get("/trigger-generic-exception"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/errors/500"))
                .andExpect(model().attribute("error", "Произошла непредвиденная ошибка"));
    }
}