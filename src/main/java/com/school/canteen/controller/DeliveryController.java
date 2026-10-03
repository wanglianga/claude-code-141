package com.school.canteen.controller;

import com.school.canteen.dto.DeliveryDtos.DeliveryRequest;
import com.school.canteen.entity.DeliveryBatch;
import com.school.canteen.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "送货批次")
@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Operation(summary = "送货批次列表")
    @GetMapping
    public List<DeliveryBatch> list(@RequestParam(required = false) Long orderId) {
        return deliveryService.list(orderId);
    }

    @Operation(summary = "送货批次详情")
    @GetMapping("/{id}")
    public DeliveryBatch get(@PathVariable Long id) {
        return deliveryService.get(id);
    }

    @Operation(summary = "登记送货到校（车牌、到货时间、车厢温度、批次号、检疫票）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSPECTOR','CANTEEN')")
    public DeliveryBatch register(@Valid @RequestBody DeliveryRequest req) {
        return deliveryService.register(req);
    }
}
