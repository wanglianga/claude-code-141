package com.school.canteen.config;

import com.school.canteen.entity.*;
import com.school.canteen.enums.*;
import com.school.canteen.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** 初始化演示数据：账号、供应商与合同价、本周菜单、库存安全线 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierPriceRepository priceRepository;
    private final WeeklyMenuRepository menuRepository;
    private final InventoryItemRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, SupplierRepository supplierRepository,
                           SupplierPriceRepository priceRepository, WeeklyMenuRepository menuRepository,
                           InventoryItemRepository inventoryRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.priceRepository = priceRepository;
        this.menuRepository = menuRepository;
        this.inventoryRepository = inventoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        seedUsers();
        seedSuppliers();
        seedMenu();
        seedInventory();
    }

    private void seedUsers() {
        user("admin", "admin123", "系统管理员", Role.ADMIN);
        user("buyer", "buyer123", "采购负责人-王采购", Role.PROCUREMENT);
        user("inspector", "inspect123", "验收员-李验收", Role.INSPECTOR);
        user("finance", "finance123", "财务-赵财务", Role.FINANCE);
        user("canteen", "canteen123", "食堂主管-陈食堂", Role.CANTEEN);
    }

    private void user(String username, String password, String name, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setName(name);
        u.setRole(role);
        userRepository.save(u);
    }

    private void seedSuppliers() {
        Supplier liangyou = supplier("惠民粮油副食批发有限公司", "刘经理", "13800000001",
                Set.of(Category.GRAIN_OIL, Category.CONDIMENT), false);
        Supplier lvyuan = supplier("绿源农产品有限公司", "张经理", "13800000002",
                Set.of(Category.VEGETABLE, Category.FRUIT), false);
        Supplier siji = supplier("四季鲜果蔬配送中心", "孙经理", "13800000005",
                Set.of(Category.VEGETABLE, Category.FRUIT), false);
        Supplier kangyuan = supplier("康源肉业食品有限公司", "周经理", "13800000003",
                Set.of(Category.MEAT), true);
        Supplier chenguang = supplier("晨光乳业配送有限公司", "吴经理", "13800000004",
                Set.of(Category.DAIRY), true);

        price(liangyou, Category.GRAIN_OIL, "大米", "25kg/袋", "kg", "4.50", false, null);
        price(liangyou, Category.GRAIN_OIL, "面粉", "25kg/袋", "kg", "4.00", false, null);
        price(liangyou, Category.GRAIN_OIL, "食用油", "10L/桶", "L", "9.80", false, null);
        price(liangyou, Category.CONDIMENT, "酱油", "5L/桶", "L", "6.00", false, null);
        price(liangyou, Category.CONDIMENT, "食盐", "500g/袋", "kg", "2.00", false, null);

        price(lvyuan, Category.VEGETABLE, "青菜", "散装", "kg", "3.60", false, null);
        price(lvyuan, Category.VEGETABLE, "番茄", "散装", "kg", "4.20", false, null);
        price(lvyuan, Category.VEGETABLE, "白菜", "散装", "kg", "2.80", false, null);
        price(lvyuan, Category.FRUIT, "苹果", "箱装", "kg", "6.50", false, null);
        price(lvyuan, Category.FRUIT, "香蕉", "箱装", "kg", "4.80", false, null);

        price(siji, Category.VEGETABLE, "青菜", "散装", "kg", "3.80", false, null);
        price(siji, Category.VEGETABLE, "番茄", "散装", "kg", "4.50", false, null);
        price(siji, Category.FRUIT, "苹果", "箱装", "kg", "6.80", false, null);

        price(kangyuan, Category.MEAT, "猪肉", "冷鲜", "kg", "28.00", true, "4");
        price(kangyuan, Category.MEAT, "鸡肉", "冷鲜", "kg", "18.00", true, "4");
        price(kangyuan, Category.MEAT, "牛肉", "冷冻", "kg", "60.00", true, "-12");

        price(chenguang, Category.DAIRY, "酸奶", "200g/杯", "杯", "1.80", true, "6");
        price(chenguang, Category.DAIRY, "纯牛奶", "250ml/盒", "盒", "2.50", true, "6");
        price(chenguang, Category.DAIRY, "鸡蛋", "散装", "kg", "9.60", false, null);
    }

    private Supplier supplier(String name, String contact, String phone, Set<Category> categories, boolean coldChain) {
        Supplier s = new Supplier();
        s.setName(name);
        s.setContact(contact);
        s.setPhone(phone);
        s.setAddress("本市农产品物流园");
        s.setCategories(categories);
        s.setColdChainCapable(coldChain);
        return supplierRepository.save(s);
    }

    private void price(Supplier s, Category c, String name, String spec, String unit, String price,
                       boolean coldChain, String tempMax) {
        SupplierPrice p = new SupplierPrice();
        p.setSupplier(s);
        p.setCategory(c);
        p.setIngredientName(name);
        p.setSpec(spec);
        p.setUnit(unit);
        p.setPrice(new BigDecimal(price));
        p.setColdChainRequired(coldChain);
        p.setTempMax(tempMax == null ? null : new BigDecimal(tempMax));
        priceRepository.save(p);
    }

    private void seedMenu() {
        // 下一教学周：2026-10-05（周一）至 2026-10-09（周五）
        LocalDate monday = LocalDate.of(2026, 10, 5);
        WeeklyMenu menu = new WeeklyMenu();
        menu.setTitle("2026年秋季第6周学生营养餐菜单");
        menu.setWeekStart(monday);
        menu.setWeekEnd(monday.plusDays(6));
        menu.setStatus(MenuStatus.PUBLISHED);

        for (int d = 0; d < 5; d++) {
            LocalDate date = monday.plusDays(d);
            meal(menu, date, MealType.BREAKFAST, "大米粥+馒头+煮鸡蛋", 1200, List.of(
                    ing(Category.GRAIN_OIL, "大米", "25", "kg"),
                    ing(Category.GRAIN_OIL, "面粉", "60", "kg"),
                    ing(Category.DAIRY, "鸡蛋", "40", "kg")));
            meal(menu, date, MealType.LUNCH, "米饭+红烧肉+清炒青菜+苹果", 1200, List.of(
                    ing(Category.GRAIN_OIL, "大米", "110", "kg"),
                    ing(Category.MEAT, "猪肉", "70", "kg"),
                    ing(Category.VEGETABLE, "青菜", "90", "kg"),
                    ing(Category.FRUIT, "苹果", "60", "kg"),
                    ing(Category.GRAIN_OIL, "食用油", "12", "L"),
                    ing(Category.CONDIMENT, "酱油", "4", "L")));
            meal(menu, date, MealType.DINNER, "米饭+番茄炒蛋+酸奶", 1200, List.of(
                    ing(Category.GRAIN_OIL, "大米", "80", "kg"),
                    ing(Category.VEGETABLE, "番茄", "55", "kg"),
                    ing(Category.DAIRY, "鸡蛋", "35", "kg"),
                    ing(Category.DAIRY, "酸奶", "1200", "杯"),
                    ing(Category.GRAIN_OIL, "食用油", "8", "L")));
        }
        menuRepository.save(menu);
    }

    private void meal(WeeklyMenu menu, LocalDate date, MealType type, String dish, int servings,
                      List<MenuIngredient> ings) {
        MenuMeal meal = new MenuMeal();
        meal.setMenu(menu);
        meal.setMealDate(date);
        meal.setMealType(type);
        meal.setDishName(dish);
        meal.setServings(servings);
        for (MenuIngredient ing : ings) {
            ing.setMeal(meal);
            meal.getIngredients().add(ing);
        }
        menu.getMeals().add(meal);
    }

    private MenuIngredient ing(Category c, String name, String qty, String unit) {
        MenuIngredient mi = new MenuIngredient();
        mi.setCategory(c);
        mi.setIngredientName(name);
        mi.setQuantity(new BigDecimal(qty));
        mi.setUnit(unit);
        return mi;
    }

    private void seedInventory() {
        inv("大米", "25kg/袋", "kg", Category.GRAIN_OIL, "500", "300");
        inv("面粉", "25kg/袋", "kg", Category.GRAIN_OIL, "200", "100");
        inv("食用油", "10L/桶", "L", Category.GRAIN_OIL, "60", "40");
        inv("酱油", "5L/桶", "L", Category.CONDIMENT, "30", "15");
        inv("猪肉", "冷鲜", "kg", Category.MEAT, "40", "50");
        inv("青菜", "散装", "kg", Category.VEGETABLE, "15", "30");
        inv("番茄", "散装", "kg", Category.VEGETABLE, "20", "30");
        inv("苹果", "箱装", "kg", Category.FRUIT, "80", "50");
        inv("酸奶", "200g/杯", "杯", Category.DAIRY, "500", "800");
        inv("鸡蛋", "散装", "kg", Category.DAIRY, "50", "40");
    }

    private void inv(String name, String spec, String unit, Category c, String qty, String safety) {
        InventoryItem it = new InventoryItem();
        it.setIngredientName(name);
        it.setSpec(spec);
        it.setUnit(unit);
        it.setCategory(c);
        it.setQuantity(new BigDecimal(qty));
        it.setSafetyStock(new BigDecimal(safety));
        inventoryRepository.save(it);
    }
}
