package com.school.canteen.service;

import com.school.canteen.entity.Supplier;
import com.school.canteen.entity.SupplierPrice;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.SupplierPriceRepository;
import com.school.canteen.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierPriceRepository priceRepository;

    public SupplierService(SupplierRepository supplierRepository, SupplierPriceRepository priceRepository) {
        this.supplierRepository = supplierRepository;
        this.priceRepository = priceRepository;
    }

    public List<Supplier> list() {
        return supplierRepository.findAll();
    }

    public Supplier get(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("供应商不存在: " + id));
    }

    @Transactional
    public Supplier create(Supplier supplier) {
        if (supplierRepository.existsByName(supplier.getName())) {
            throw ApiException.conflict("供应商名称已存在: " + supplier.getName());
        }
        supplier.setId(null);
        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier update(Long id, Supplier patch) {
        Supplier s = get(id);
        s.setContact(patch.getContact());
        s.setPhone(patch.getPhone());
        s.setAddress(patch.getAddress());
        s.setCategories(patch.getCategories());
        s.setColdChainCapable(patch.isColdChainCapable());
        if (patch.getStatus() != null) {
            s.setStatus(patch.getStatus());
        }
        return supplierRepository.save(s);
    }

    public List<SupplierPrice> prices(Long supplierId) {
        return priceRepository.findBySupplierId(supplierId);
    }

    @Transactional
    public SupplierPrice addPrice(Long supplierId, SupplierPrice price) {
        Supplier s = get(supplierId);
        price.setId(null);
        price.setSupplier(s);
        return priceRepository.save(price);
    }

    @Transactional
    public void deletePrice(Long priceId) {
        priceRepository.deleteById(priceId);
    }
}
