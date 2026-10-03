package com.school.canteen.service;

import com.school.canteen.entity.AcceptanceItem;
import com.school.canteen.entity.DowngradeApproval;
import com.school.canteen.entity.OrderItem;
import com.school.canteen.enums.DowngradeStatus;
import com.school.canteen.enums.TxnType;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.DowngradeApprovalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DowngradeService {

    private final DowngradeApprovalRepository repository;
    private final InventoryService inventoryService;

    public DowngradeService(DowngradeApprovalRepository repository, InventoryService inventoryService) {
        this.repository = repository;
        this.inventoryService = inventoryService;
    }

    public List<DowngradeApproval> list(DowngradeStatus status) {
        return status != null ? repository.findByStatus(status) : repository.findAll();
    }

    public DowngradeApproval get(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("降级审批单不存在: " + id));
    }

    /** 审批降级使用；批准后食材入库（按折价计价，结算时体现） */
    @Transactional
    public DowngradeApproval decide(Long id, boolean approve, BigDecimal priceRatio, String username) {
        DowngradeApproval da = get(id);
        if (da.getStatus() != DowngradeStatus.PENDING) {
            throw ApiException.badRequest("该审批已处理: " + da.getStatus());
        }
        da.setStatus(approve ? DowngradeStatus.APPROVED : DowngradeStatus.REJECTED);
        if (priceRatio != null) {
            da.setPriceRatio(priceRatio);
        }
        da.setApprover(username);
        da.setDecidedAt(LocalDateTime.now());
        da = repository.save(da);

        if (approve) {
            AcceptanceItem ai = da.getAcceptanceItem();
            OrderItem oi = ai.getOrderItem();
            inventoryService.change(oi.getCategory(), oi.getIngredientName(), oi.getSpec(), oi.getUnit(),
                    da.getQuantity(), TxnType.IN_DOWNGRADE, "DOWNGRADE", da.getId(), username,
                    "降级使用入库（折价率 " + da.getPriceRatio() + "）");
        }
        return da;
    }
}
