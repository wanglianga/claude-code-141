package com.school.canteen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全流程集成测试：菜单→采购计划→下单→送货→验收（异常）→入库/退货/降级→索赔→结算→影响分析→统计
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullFlowIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;

    static String buyerToken, inspectorToken, financeToken, canteenToken;
    static Long meatOrderId, meatOrderItemId, batchId, acceptanceId;
    static Long shortAnomalyId, downgradeId, claimId, settlementId;

    private String login(String username, String password) throws Exception {
        MvcResult res = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return om.readTree(res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("token").asText();
    }

    private JsonNode get(String url, String token) throws Exception {
        MvcResult res = mvc.perform(MockMvcRequestBuilders.get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return om.readTree(res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private JsonNode post(String url, String token, Object body) throws Exception {
        MvcResult res = mvc.perform(MockMvcRequestBuilders.post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn();
        return om.readTree(res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    @Order(1)
    void login_all_roles() throws Exception {
        buyerToken = login("buyer", "buyer123");
        inspectorToken = login("inspector", "inspect123");
        financeToken = login("finance", "finance123");
        canteenToken = login("canteen", "canteen123");
        assertThat(buyerToken).isNotBlank();
    }

    @Test
    @Order(2)
    void generate_plan_from_menu() throws Exception {
        JsonNode menus = get("/api/menus", buyerToken);
        Long menuId = menus.get(0).get("id").asLong();

        JsonNode plan = post("/api/menus/" + menuId + "/generate-plan?expectedArrival=2026-10-05T07:00:00",
                buyerToken, null);
        assertThat(plan.get("ordersCreated").asInt()).isEqualTo(4); // 粮油/果蔬/肉/乳 四个供应商

        JsonNode orders = get("/api/orders", buyerToken);
        for (JsonNode o : orders) {
            String supplier = o.get("supplier").get("name").asText();
            if (supplier.contains("康源肉业")) {
                meatOrderId = o.get("id").asLong();
                assertThat(o.get("totalAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("9800.00"));
            }
            post("/api/orders/" + o.get("id").asLong() + "/confirm", buyerToken, null);
        }
        assertThat(meatOrderId).isNotNull();

        JsonNode meatOrder = get("/api/orders/" + meatOrderId, buyerToken);
        JsonNode item = meatOrder.get("items").get(0);
        meatOrderItemId = item.get("id").asLong();
        assertThat(item.get("ingredientName").asText()).isEqualTo("猪肉");
        assertThat(item.get("quantity").decimalValue()).isEqualByComparingTo(new BigDecimal("350"));
        assertThat(item.get("coldChainRequired").asBoolean()).isTrue();
    }

    @Test
    @Order(3)
    void register_delivery_late_hot_no_cert() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("orderId", meatOrderId);
        req.put("plateNumber", "沪A12345");
        req.put("arrivedAt", "2026-10-05T09:30:00"); // 要求 07:00 前到，晚 150 分钟
        req.put("temperature", new BigDecimal("12")); // 冷链要求 ≤4℃
        req.put("supplierBatchNo", "KY20261005-01");
        req.put("quarantineCertProvided", false); // 肉类未提供检疫票
        req.put("receivedBy", "李验收");
        JsonNode batch = post("/api/deliveries", inspectorToken, req);
        batchId = batch.get("id").asLong();
        assertThat(batch.get("batchNo").asText()).startsWith("DB");
    }

    @Test
    @Order(4)
    void acceptance_with_anomalies() throws Exception {
        JsonNode acc = post("/api/acceptances/start", inspectorToken,
                Map.of("batchId", batchId, "notes", "冷链车制冷故障"));
        acceptanceId = acc.get("id").asLong();

        Map<String, Object> item = new HashMap<>();
        item.put("orderItemId", meatOrderItemId);
        item.put("deliveredQty", new BigDecimal("340"));   // 少送 10kg
        item.put("acceptedQty", new BigDecimal("300"));
        item.put("rejectedQty", new BigDecimal("30"));     // 拒收 30kg
        item.put("downgradedQty", new BigDecimal("10"));   // 降级 10kg
        item.put("temperatureOk", false);
        item.put("appearanceOk", false);
        item.put("rejectReason", "温度超标，部分变色");
        JsonNode done = post("/api/acceptances/" + acceptanceId + "/complete", inspectorToken,
                Map.of("items", List.of(item)));
        assertThat(done.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(done.get("overallResult").asText()).isEqualTo("PARTIAL");

        // 库存：40 + 300 = 340
        JsonNode inventory = get("/api/inventory", inspectorToken);
        for (JsonNode inv : inventory) {
            if (inv.get("ingredientName").asText().equals("猪肉")) {
                assertThat(inv.get("quantity").decimalValue()).isEqualByComparingTo(new BigDecimal("340"));
            }
        }
        // 退货单 30kg
        JsonNode returns = get("/api/returns", inspectorToken);
        assertThat(returns.size()).isEqualTo(1);
        assertThat(returns.get(0).get("quantity").decimalValue()).isEqualByComparingTo(new BigDecimal("30"));
        // 降级审批待处理
        JsonNode downgrades = get("/api/downgrades?status=PENDING", canteenToken);
        assertThat(downgrades.size()).isEqualTo(1);
        downgradeId = downgrades.get(0).get("id").asLong();
        // 异常：少送/晚送/冷链/票证/质检 共 5 条
        JsonNode anomalies = get("/api/anomalies", buyerToken);
        assertThat(anomalies.size()).isEqualTo(5);
        for (JsonNode a : anomalies) {
            if (a.get("type").asText().equals("SHORT_DELIVERY")) {
                shortAnomalyId = a.get("id").asLong();
                assertThat(a.get("claimSuggestedAmount").decimalValue())
                        .isEqualByComparingTo(new BigDecimal("280.00"));
                assertThat(a.get("affectedMealsCount").asInt()).isGreaterThan(0);
            }
        }
        assertThat(shortAnomalyId).isNotNull();
    }

    @Test
    @Order(5)
    void downgrade_approval_stocks_in() throws Exception {
        post("/api/downgrades/" + downgradeId + "/decide", canteenToken,
                Map.of("approve", true, "priceRatio", new BigDecimal("0.60")));
        JsonNode inventory = get("/api/inventory", canteenToken);
        for (JsonNode inv : inventory) {
            if (inv.get("ingredientName").asText().equals("猪肉")) {
                assertThat(inv.get("quantity").decimalValue()).isEqualByComparingTo(new BigDecimal("350"));
            }
        }
    }

    @Test
    @Order(6)
    void claim_approve() throws Exception {
        JsonNode claim = post("/api/claims", buyerToken,
                Map.of("anomalyId", shortAnomalyId, "amount", new BigDecimal("280"),
                        "reason", "少送 10kg 猪肉，按合同价索赔"));
        claimId = claim.get("id").asLong();
        JsonNode decided = post("/api/claims/" + claimId + "/decide", financeToken,
                Map.of("approve", true, "note", "情况属实"));
        assertThat(decided.get("status").asText()).isEqualTo("APPROVED");
    }

    @Test
    @Order(7)
    void settlement_calculation() throws Exception {
        // base=300*28=8400, downgrade=10*28*0.6=168, claim=280,
        // penalty=晚送196+冷链476+票证100=772, final=8400+168-280-772=7516
        JsonNode st = post("/api/settlements/generate", financeToken, Map.of(
                "supplierId", supplierIdOf("康源肉业"),
                "periodStart", "2026-10-01",
                "periodEnd", "2026-10-31",
                "supplierInvoiceAmount", new BigDecimal("8000"))); // 发票与应付不一致
        settlementId = st.get("id").asLong();
        assertThat(st.get("baseAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("8400.00"));
        assertThat(st.get("downgradeAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("168.00"));
        assertThat(st.get("returnDeduction").decimalValue()).isEqualByComparingTo(new BigDecimal("840.00"));
        assertThat(st.get("claimDeduction").decimalValue()).isEqualByComparingTo(new BigDecimal("280.00"));
        assertThat(st.get("penaltyAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("772.00"));
        assertThat(st.get("finalAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("7516.00"));
        assertThat(st.get("mismatchFlag").asBoolean()).isTrue();

        // 金额不一致异常已自动登记
        JsonNode anomalies = get("/api/anomalies", buyerToken);
        boolean hasMismatch = false;
        for (JsonNode a : anomalies) {
            if (a.get("type").asText().equals("SETTLEMENT_MISMATCH")) {
                hasMismatch = true;
            }
        }
        assertThat(hasMismatch).isTrue();

        JsonNode confirmed = post("/api/settlements/" + settlementId + "/confirm", financeToken, null);
        assertThat(confirmed.get("status").asText()).isEqualTo("CONFIRMED");
    }

    @Test
    @Order(8)
    void impact_and_stats() throws Exception {
        // 影响分析：猪肉拒收/降级 → 影响午餐红烧肉餐次
        JsonNode impact = get("/api/acceptances/" + acceptanceId + "/impact", buyerToken);
        assertThat(impact.get("affectedMeals").size()).isGreaterThan(0);
        assertThat(impact.get("supplierResponsibility").size()).isGreaterThan(0);

        // 供应商异常汇总：连续异常批次
        JsonNode summary = get("/api/suppliers/" + supplierIdOf("康源肉业") + "/anomaly-summary", buyerToken);
        assertThat(summary.get("totalAnomalies").asInt()).isGreaterThanOrEqualTo(5);
        assertThat(summary.get("consecutiveAnomalyBatches").asInt()).isEqualTo(1);

        // 异常归因处理
        JsonNode anomalies = get("/api/anomalies", buyerToken);
        for (JsonNode a : anomalies) {
            post("/api/anomalies/" + a.get("id").asLong() + "/resolve", buyerToken,
                    Map.of("causeCategory", "SUPPLIER_PERFORMANCE", "resolutionNote", "已约谈供应商"));
        }
        JsonNode stats = get("/api/stats/anomalies?groupBy=supplier", buyerToken);
        assertThat(stats.get("totalAnomalies").asInt()).isGreaterThanOrEqualTo(6);
        JsonNode causes = get("/api/stats/anomalies/causes", buyerToken);
        assertThat(causes.get("supplierPerformance").asInt()).isGreaterThanOrEqualTo(5);

        // 家长公告
        JsonNode ann = post("/api/announcements", canteenToken, Map.of(
                "title", "关于10月5日午餐菜品调整的通知",
                "content", "因供应商送货部分猪肉检疫票缺失被拒收，当日红烧肉调整为红烧鸡腿，特此告知。",
                "mealDate", "2026-10-05", "mealType", "LUNCH", "dishName", "米饭+红烧肉+清炒青菜+苹果"));
        JsonNode published = post("/api/announcements/" + ann.get("id").asLong() + "/publish", canteenToken, null);
        assertThat(published.get("status").asText()).isEqualTo("PUBLISHED");
    }

    private Long supplierIdOf(String nameKeyword) throws Exception {
        JsonNode suppliers = get("/api/suppliers", buyerToken);
        for (JsonNode s : suppliers) {
            if (s.get("name").asText().contains(nameKeyword)) {
                return s.get("id").asLong();
            }
        }
        throw new IllegalStateException("供应商不存在: " + nameKeyword);
    }
}
