package com.school.canteen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DeliveryDtos {

    /** 登记送货批次 */
    public record DeliveryRequest(
            @NotNull Long orderId,
            @NotBlank String plateNumber,
            @NotNull LocalDateTime arrivedAt,
            BigDecimal temperature,
            String supplierBatchNo,
            boolean quarantineCertProvided,
            String quarantineCertNo,
            String receivedBy,
            String notes) {
    }
}
