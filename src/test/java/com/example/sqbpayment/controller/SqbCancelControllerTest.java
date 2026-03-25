package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.service.SqbCancelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SqbCancelController.class)
@AutoConfigureMockMvc(addFilters = false)
class SqbCancelControllerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SqbCancelService cancelService;

    @Test
    void testCancelSuccess() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"CANCEL_SUCCESS",
                        "data":{
                            "sn":"789284025",
                            "client_sn":"order001",
                            "order_status":"PAY_CANCELED",
                            "total_amount":"100"
                        }
                    }
                }
                """;
        when(cancelService.cancel(any()))
                .thenReturn(CompletableFuture.completedFuture(new SqbResponse(MAPPER.readTree(responseJson))));

        mockMvc.perform(post("/api/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sn":"789284025"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("PAY_CANCELED"));
    }

    @Test
    void testCancelByClientSn() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"CANCEL_SUCCESS",
                        "data":{
                            "order_status":"CANCELED"
                        }
                    }
                }
                """;
        when(cancelService.cancel(any()))
                .thenReturn(CompletableFuture.completedFuture(new SqbResponse(MAPPER.readTree(responseJson))));

        mockMvc.perform(post("/api/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientSn":"order001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("CANCELED"));
    }
}
