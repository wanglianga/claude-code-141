package com.school.canteen.service;

import com.school.canteen.dto.DeliveryDtos.DeliveryRequest;
import com.school.canteen.entity.DeliveryBatch;
import com.school.canteen.entity.PurchaseOrder;
import com.school.canteen.enums.OrderStatus;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.DeliveryBatchRepository;
import com.school.canteen.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeliveryService {

    private final DeliveryBatchRepository batchRepository;
    private final PurchaseOrderRepository orderRepository;
    private final NoGenerator noGenerator;

    public DeliveryService(DeliveryBatchRepository batchRepository, PurchaseOrderRepository orderRepository,
                           NoGenerator noGenerator) {
        this.batchRepository = batchRepository;
        this.orderRepository = orderRepository;
        this.noGenerator = noGenerator;
    }

    public List<DeliveryBatch> list(Long orderId) {
        return orderId != null ? batchRepository.findByOrderId(orderId) : batchRepository.findAll();
    }

    public DeliveryBatch get(Long id) {
        return batchRepository.findById(id).orElseThrow(() -> ApiException.notFound("送货批次不存在: " + id));
    }

    @Transactional
    public DeliveryBatch register(DeliveryRequest req) {
        PurchaseOrder order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> ApiException.notFound("订货单不存在: " + req.orderId()));
        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.DELIVERING) {
            throw ApiException.badRequest("订货单状态不允许登记送货: " + order.getStatus());
        }
        DeliveryBatch batch = new DeliveryBatch();
        String no;
        do {
            no = noGenerator.next("DB");
        } while (batchRepository.existsByBatchNo(no));
        batch.setBatchNo(no);
        batch.setOrder(order);
        batch.setPlateNumber(req.plateNumber());
        batch.setArrivedAt(req.arrivedAt());
        batch.setTemperature(req.temperature());
        batch.setSupplierBatchNo(req.supplierBatchNo());
        batch.setQuarantineCertProvided(req.quarantineCertProvided());
        batch.setQuarantineCertNo(req.quarantineCertNo());
        batch.setReceivedBy(req.receivedBy());
        batch.setNotes(req.notes());

        order.setStatus(OrderStatus.DELIVERING);
        orderRepository.save(order);
        return batchRepository.save(batch);
    }
}
