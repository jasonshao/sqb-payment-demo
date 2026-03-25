package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.service.SqbPrecreateService;
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

@WebMvcTest(SqbPrecreateController.class)
@AutoConfigureMockMvc(addFilters = false)
class SqbPrecreateControllerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SqbPrecreateService precreateService;

    @Test
    void testPrecreateSuccess() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"PRECREATE_SUCCESS",
                        "data":{
                            "sn":"789284025",
                            "client_sn":"sn001",
                            "order_status":"CREATED",
                            "qr_code":"https://qr.alipay.com/test123",
                            "total_amount":"100"
                        }
                    }
                }
                """;
        when(precreateService.precreate(any()))
                .thenReturn(CompletableFuture.completedFuture(new SqbResponse(MAPPER.readTree(responseJson))));

        mockMvc.perform(post("/api/precreate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payway":"4","totalAmount":100,"subject":"测试商品","operator":"cashier01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.qrCode").value("https://qr.alipay.com/test123"))
                .andExpect(jsonPath("$.data.totalAmount").value("100"));
    }

    @Test
    void testPrecreateValidationMissingPayway() throws Exception {
        mockMvc.perform(post("/api/precreate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalAmount":100,"subject":"测试","operator":"op"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPrecreateValidationNegativeAmount() throws Exception {
        mockMvc.perform(post("/api/precreate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payway":"3","totalAmount":-1,"subject":"测试","operator":"op"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPrecreateValidationMissingSubject() throws Exception {
        mockMvc.perform(post("/api/precreate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payway":"3","totalAmount":100,"operator":"op"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
