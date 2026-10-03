package com.school.canteen.service;

import com.school.canteen.entity.*;
import com.school.canteen.enums.Category;
import com.school.canteen.enums.MenuStatus;
import com.school.canteen.enums.OrderStatus;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final WeeklyMenuRepository menuRepository;
    private final PurchaseOrderRepository orderRepository;
    private final SupplierPriceRepository priceRepository;
    private final NoGenerator noGenerator;

    public MenuService(WeeklyMenuRepository menuRepository, PurchaseOrderRepository orderRepository,
                       SupplierPriceRepository priceRepository, NoGenerator noGenerator) {
        this.menuRepository = menuRepository;
        this.orderRepository = orderRepository;
        this.priceRepository = priceRepository;
        this.noGenerator = noGenerator;
    }

    public List<WeeklyMenu> list() {
        return menuRepository.findAll();
    }

    public WeeklyMenu get(Long id) {
        return menuRepository.findById(id).orElseThrow(() -> ApiException.notFound("菜单不存在: " + id));
    }

    @Transactional
    public WeeklyMenu create(WeeklyMenu menu) {
        if (menu.getWeekStart() == null) {
            throw ApiException.badRequest("weekStart 不能为空");
        }
        menuRepository.findByWeekStart(menu.getWeekStart()).ifPresent(m -> {
            throw ApiException.conflict("该周菜单已存在: " + menu.getWeekStart());
        });
        menu.setId(null);
        menu.setWeekEnd(menu.getWeekStart().plusDays(6));
        for (MenuMeal meal : menu.getMeals()) {
            meal.setId(null);
            meal.setMenu(menu);
            for (MenuIngredient ing : meal.getIngredients()) {
                ing.setId(null);
                ing.setMeal(meal);
            }
        }
        return menuRepository.save(menu);
    }

    @Transactional
    public WeeklyMenu publish(Long id) {
        WeeklyMenu menu = get(id);
        if (menu.getStatus() != MenuStatus.DRAFT) {
            throw ApiException.badRequest("仅草稿状态的菜单可以发布");
        }
        menu.setStatus(MenuStatus.PUBLISHED);
        return menuRepository.save(menu);
    }

    /**
     * 按周菜单生成采购计划：汇总食材需求 → 按供应商价格表选最低价供应商 → 按供应商拆分订货单。
     * 返回生成的订货单与无法匹配供应商的食材清单。
     */
    @Transactional
    public Map<String, Object> generatePlan(Long menuId, LocalDateTime expectedArrival, String username) {
        WeeklyMenu menu = get(menuId);
        if (menu.getMeals().isEmpty()) {
            throw ApiException.badRequest("菜单没有任何餐次，无法生成采购计划");
        }
        LocalDateTime arrival = expectedArrival != null ? expectedArrival
                : menu.getWeekStart().atTime(7, 0);

        // 1. 汇总需求：key = 品类|食材|单位
        Map<String, MenuIngredient> demand = new LinkedHashMap<>();
        for (MenuMeal meal : menu.getMeals()) {
            for (MenuIngredient ing : meal.getIngredients()) {
                String key = ing.getCategory() + "|" + ing.getIngredientName() + "|" + ing.getUnit();
                demand.computeIfAbsent(key, k -> {
                    MenuIngredient agg = new MenuIngredient();
                    agg.setCategory(ing.getCategory());
                    agg.setIngredientName(ing.getIngredientName());
                    agg.setUnit(ing.getUnit());
                    agg.setQuantity(BigDecimal.ZERO);
                    return agg;
                });
                MenuIngredient agg = demand.get(key);
                agg.setQuantity(agg.getQuantity().add(ing.getQuantity()));
            }
        }

        // 2. 匹配供应商（同食材同单位取合同价最低且状态正常的供应商）
        Map<Supplier, List<OrderItem>> bySupplier = new LinkedHashMap<>();
        List<String> unmatched = new ArrayList<>();
        for (MenuIngredient agg : demand.values()) {
            List<SupplierPrice> candidates = priceRepository
                    .findByIngredientNameAndUnit(agg.getIngredientName(), agg.getUnit()).stream()
                    .filter(p -> p.getSupplier().getStatus() == Supplier.Status.ACTIVE)
                    .filter(p -> !p.isColdChainRequired() || p.getSupplier().isColdChainCapable())
                    .sorted(Comparator.comparing(SupplierPrice::getPrice))
                    .toList();
            if (candidates.isEmpty()) {
                unmatched.add(agg.getCategory() + "/" + agg.getIngredientName() + " " + agg.getQuantity() + agg.getUnit());
                continue;
            }
            SupplierPrice sp = candidates.get(0);
            OrderItem item = new OrderItem();
            item.setCategory(agg.getCategory());
            item.setIngredientName(agg.getIngredientName());
            item.setSpec(sp.getSpec());
            item.setQuantity(agg.getQuantity());
            item.setUnit(agg.getUnit());
            item.setExpectedArrival(arrival);
            item.setQualityRequirement(defaultQualityRequirement(agg.getCategory()));
            item.setContractPrice(sp.getPrice());
            item.setColdChainRequired(sp.isColdChainRequired());
            item.setTempMax(sp.getTempMax());
            bySupplier.computeIfAbsent(sp.getSupplier(), k -> new ArrayList<>()).add(item);
        }

        // 3. 按供应商生成订货单
        List<PurchaseOrder> orders = new ArrayList<>();
        for (Map.Entry<Supplier, List<OrderItem>> e : bySupplier.entrySet()) {
            PurchaseOrder order = new PurchaseOrder();
            order.setOrderNo(uniqueOrderNo());
            order.setMenu(menu);
            order.setSupplier(e.getKey());
            order.setStatus(OrderStatus.DRAFT);
            order.setCreatedBy(username);
            order.setRemark("由周菜单《" + menu.getTitle() + "》自动生成");
            BigDecimal total = BigDecimal.ZERO;
            for (OrderItem item : e.getValue()) {
                item.setOrder(order);
                total = total.add(item.getQuantity().multiply(item.getContractPrice()));
                order.getItems().add(item);
            }
            order.setTotalAmount(total);
            orders.add(orderRepository.save(order));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("menuId", menu.getId());
        result.put("ordersCreated", orders.size());
        result.put("orders", orders.stream().map(o -> Map.of(
                "orderId", o.getId(),
                "orderNo", o.getOrderNo(),
                "supplier", o.getSupplier().getName(),
                "itemCount", o.getItems().size(),
                "totalAmount", o.getTotalAmount())).collect(Collectors.toList()));
        result.put("unmatchedIngredients", unmatched);
        return result;
    }

    private String uniqueOrderNo() {
        String no;
        do {
            no = noGenerator.next("PO");
        } while (orderRepository.existsByOrderNo(no));
        return no;
    }

    private String defaultQualityRequirement(Category category) {
        return switch (category) {
            case MEAT -> "索取动物检疫合格证明；冷链运输，到货温度≤4℃；色泽正常无异味";
            case DAIRY -> "生产日期不超过保质期 1/3；冷链运输，到货温度≤6℃";
            case VEGETABLE -> "新鲜无腐烂，腐烂比例≤5%；农残抽检合格";
            case FRUIT -> "无霉变无机械伤，腐烂比例≤5%";
            case GRAIN_OIL -> "包装完好，生产日期在保质期内，无受潮结块";
            case CONDIMENT -> "包装完好，标签齐全，在保质期内";
        };
    }
}
