package io.gomobi.payment.adapter.helloclever;

import io.gomobi.payment.core.enums.TransactionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloCleverStatusMapperTest {

    private final HelloCleverStatusMapper mapper = new HelloCleverStatusMapper();

    @Test
    void mapStatus_receivedMapsToSuccess() {
        assertEquals(TransactionStatus.SUCCESS, mapper.mapStatus("received"));
    }
}

