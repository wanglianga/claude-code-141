package com.school.canteen.service;

import com.school.canteen.entity.ReturnOrder;
import com.school.canteen.enums.ReturnStatus;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.ReturnOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReturnService {

    private final ReturnOrderRepository repository;

    public ReturnService(ReturnOrderRepository repository) {
        this.repository = repository;
    }

    public List<ReturnOrder> list(ReturnStatus status, Long supplierId) {
        if (status != null) {
            return repository.findByStatus(status);
        }
        if (supplierId != null) {
            return repository.findBySupplierId(supplierId);
        }
        return repository.findAll();
    }

    @Transactional
    public ReturnOrder confirmReturned(Long id) {
        ReturnOrder ro = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("退货单不存在: " + id));
        if (ro.getStatus() != ReturnStatus.PENDING) {
            throw ApiException.badRequest("退货单当前状态不允许确认: " + ro.getStatus());
        }
        ro.setStatus(ReturnStatus.RETURNED);
        ro.setReturnedAt(LocalDateTime.now());
        return repository.save(ro);
    }
}
