package com.school.canteen.service;

import com.school.canteen.entity.Anomaly;
import com.school.canteen.entity.Claim;
import com.school.canteen.enums.ClaimStatus;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.ClaimRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final AnomalyService anomalyService;
    private final NoGenerator noGenerator;

    public ClaimService(ClaimRepository claimRepository, AnomalyService anomalyService, NoGenerator noGenerator) {
        this.claimRepository = claimRepository;
        this.anomalyService = anomalyService;
        this.noGenerator = noGenerator;
    }

    public List<Claim> list(ClaimStatus status, Long supplierId) {
        if (status != null) {
            return claimRepository.findByStatus(status);
        }
        if (supplierId != null) {
            return claimRepository.findBySupplierId(supplierId);
        }
        return claimRepository.findAll();
    }

    public Claim get(Long id) {
        return claimRepository.findById(id).orElseThrow(() -> ApiException.notFound("索赔单不存在: " + id));
    }

    @Transactional
    public Claim create(Long anomalyId, java.math.BigDecimal amount, String reason, String username) {
        Anomaly anomaly = anomalyService.get(anomalyId);
        Claim claim = new Claim();
        String no;
        do {
            no = noGenerator.next("CL");
        } while (claimRepository.existsByClaimNo(no));
        claim.setClaimNo(no);
        claim.setAnomaly(anomaly);
        claim.setSupplier(anomaly.getSupplier());
        claim.setType(anomaly.getType());
        claim.setAmount(amount);
        claim.setReason(reason);
        claim.setCreatedBy(username);
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim decide(Long id, boolean approve, String note, String username) {
        Claim claim = get(id);
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw ApiException.badRequest("索赔单当前状态不允许审批: " + claim.getStatus());
        }
        claim.setStatus(approve ? ClaimStatus.APPROVED : ClaimStatus.REJECTED);
        claim.setDecidedBy(username);
        claim.setDecidedAt(LocalDateTime.now());
        claim.setDecisionNote(note);
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim pay(Long id) {
        Claim claim = get(id);
        if (claim.getStatus() != ClaimStatus.APPROVED) {
            throw ApiException.badRequest("仅已批准的索赔可以标记赔付");
        }
        claim.setStatus(ClaimStatus.PAID);
        return claimRepository.save(claim);
    }
}
