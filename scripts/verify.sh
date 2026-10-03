#!/usr/bin/env bash
# 校园食材订货验收索赔服务 —— 全流程验证脚本
# 用法: ./scripts/verify.sh [BASE_URL]   默认 http://localhost:8080
# 注意: 脚本包含严格数值断言，请在全新启动（空库）的服务上运行；
#       重复运行请先重置数据: docker compose down -v && docker compose up -d
set -u
BASE="${1:-http://localhost:8080}"
PASS=0; FAIL=0

ok()   { PASS=$((PASS+1)); echo "  ✅ $1"; }
bad()  { FAIL=$((FAIL+1)); echo "  ❌ $1"; }
check(){ if [ "$2" = "$3" ]; then ok "$1"; else bad "$1 (期望=$3 实际=$2)"; fi; }

jqget() { python3 -c "import sys,json;d=json.load(sys.stdin);print(eval(\"d$1\"))" 2>/dev/null; }

login() { curl -s "$BASE/api/auth/login" -X POST -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"$2\"}" | jqget "['token']"; }

echo "== 1. 登录各角色 =="
BUYER=$(login buyer buyer123);     [ -n "$BUYER" ] && ok "采购负责人登录" || bad "采购负责人登录"
INSP=$(login inspector inspect123); [ -n "$INSP" ] && ok "验收员登录" || bad "验收员登录"
FIN=$(login finance finance123);    [ -n "$FIN" ] && ok "财务登录" || bad "财务登录"
CAN=$(login canteen canteen123);    [ -n "$CAN" ] && ok "食堂主管登录" || bad "食堂主管登录"
AUTH_H="Authorization: Bearer $BUYER"; AUTH_I="Authorization: Bearer $INSP"
AUTH_F="Authorization: Bearer $FIN";   AUTH_C="Authorization: Bearer $CAN"

echo "== 2. 周菜单 → 采购计划 → 确认下单 =="
MENU_ID=$(curl -s "$BASE/api/menus" -H "$AUTH_H" | jqget "[0]['id']")
PLAN=$(curl -s -X POST "$BASE/api/menus/$MENU_ID/generate-plan?expectedArrival=2026-10-05T07:00:00" -H "$AUTH_H")
check "按菜单生成4张订货单" "$(echo "$PLAN" | jqget "['ordersCreated']")" "4"
ORDERS=$(curl -s "$BASE/api/orders" -H "$AUTH_H")
MEAT_ORDER=$(echo "$ORDERS" | python3 -c "
import sys,json
for o in json.load(sys.stdin):
    if '康源肉业' in o['supplier']['name']: print(o['id']); break")
check "肉类订货单金额(350kg×28)" "$(echo "$ORDERS" | python3 -c "
import sys,json
for o in json.load(sys.stdin):
    if '康源肉业' in o['supplier']['name']: print(o['totalAmount']); break")" "9800.0"
for OID in $(echo "$ORDERS" | python3 -c "import sys,json;print(' '.join(str(o['id']) for o in json.load(sys.stdin)))"); do
  curl -s -X POST "$BASE/api/orders/$OID/confirm" -H "$AUTH_H" > /dev/null
done
check "订单确认后状态" "$(curl -s "$BASE/api/orders/$MEAT_ORDER" -H "$AUTH_H" | jqget "['status']")" "CONFIRMED"
MEAT_ITEM=$(curl -s "$BASE/api/orders/$MEAT_ORDER" -H "$AUTH_H" | jqget "['items'][0]['id']")

echo "== 3. 送货到校（晚送+冷链超标+缺检疫票） =="
BATCH=$(curl -s -X POST "$BASE/api/deliveries" -H "$AUTH_I" -H "Content-Type: application/json" -d "{
  \"orderId\": $MEAT_ORDER, \"plateNumber\": \"沪A12345\",
  \"arrivedAt\": \"2026-10-05T09:30:00\", \"temperature\": 12,
  \"supplierBatchNo\": \"KY20261005-01\", \"quarantineCertProvided\": false, \"receivedBy\": \"李验收\"}")
BATCH_ID=$(echo "$BATCH" | jqget "['id']")
[ -n "$BATCH_ID" ] && ok "登记送货批次 $(echo "$BATCH" | jqget "['batchNo']")" || bad "登记送货批次"

echo "== 4. 验收（少送/拒收/降级） → 入库+退货+降级+异常 =="
ACC_ID=$(curl -s -X POST "$BASE/api/acceptances/start" -H "$AUTH_I" -H "Content-Type: application/json" \
  -d "{\"batchId\": $BATCH_ID}" | jqget "['id']")
DONE=$(curl -s -X POST "$BASE/api/acceptances/$ACC_ID/complete" -H "$AUTH_I" -H "Content-Type: application/json" -d "{
  \"items\": [{\"orderItemId\": $MEAT_ITEM, \"deliveredQty\": 340, \"acceptedQty\": 300,
    \"rejectedQty\": 30, \"downgradedQty\": 10, \"temperatureOk\": false, \"appearanceOk\": false,
    \"rejectReason\": \"冷链温度超标，部分变色\"}]}")
check "验收完成" "$(echo "$DONE" | jqget "['status']")" "COMPLETED"
check "总体结论部分合格" "$(echo "$DONE" | jqget "['overallResult']")" "PARTIAL"
check "猪肉库存=40+300" "$(curl -s "$BASE/api/inventory" -H "$AUTH_I" | python3 -c "
import sys,json
for i in json.load(sys.stdin):
    if i['ingredientName']=='猪肉': print(i['quantity']); break")" "340.0"
check "生成退货单30kg" "$(curl -s "$BASE/api/returns" -H "$AUTH_I" | jqget "[0]['quantity']")" "30.0"
check "降级审批待处理" "$(curl -s "$BASE/api/downgrades?status=PENDING" -H "$AUTH_C" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")" "1"
ANOMS=$(curl -s "$BASE/api/anomalies" -H "$AUTH_H")
check "自动检测出5类异常" "$(echo "$ANOMS" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")" "5"
echo "$ANOMS" | python3 -c "
import sys,json
types={a['type'] for a in json.load(sys.stdin)}
need={'SHORT_DELIVERY','LATE_DELIVERY','COLD_CHAIN_BREACH','MISSING_CERTIFICATE','QUALITY_FAILURE'}
print('  异常类型:', ', '.join(sorted(types)))
assert need <= types, '缺少异常类型'
" && ok "异常类型覆盖 少送/晚送/冷链/票证/质检" || bad "异常类型不全"
SHORT_ID=$(echo "$ANOMS" | python3 -c "
import sys,json
for a in json.load(sys.stdin):
    if a['type']=='SHORT_DELIVERY': print(a['id']); break")

echo "== 5. 降级审批入库 =="
DG_ID=$(curl -s "$BASE/api/downgrades?status=PENDING" -H "$AUTH_C" | jqget "[0]['id']")
curl -s -X POST "$BASE/api/downgrades/$DG_ID/decide" -H "$AUTH_C" -H "Content-Type: application/json" \
  -d '{"approve": true, "priceRatio": 0.6}' > /dev/null
check "降级批准后库存=350" "$(curl -s "$BASE/api/inventory" -H "$AUTH_C" | python3 -c "
import sys,json
for i in json.load(sys.stdin):
    if i['ingredientName']=='猪肉': print(i['quantity']); break")" "350.0"

echo "== 6. 索赔 =="
CLAIM_ID=$(curl -s -X POST "$BASE/api/claims" -H "$AUTH_H" -H "Content-Type: application/json" \
  -d "{\"anomalyId\": $SHORT_ID, \"amount\": 280, \"reason\": \"少送10kg猪肉，按合同价索赔\"}" | jqget "['id']")
check "索赔批准" "$(curl -s -X POST "$BASE/api/claims/$CLAIM_ID/decide" -H "$AUTH_F" -H "Content-Type: application/json" \
  -d '{"approve": true, "note": "情况属实"}' | jqget "['status']")" "APPROVED"

echo "== 7. 结算（含发票金额核对） =="
SUP_ID=$(curl -s "$BASE/api/suppliers" -H "$AUTH_H" | python3 -c "
import sys,json
for s in json.load(sys.stdin):
    if '康源肉业' in s['name']: print(s['id']); break")
ST=$(curl -s -X POST "$BASE/api/settlements/generate" -H "$AUTH_F" -H "Content-Type: application/json" -d "{
  \"supplierId\": $SUP_ID, \"periodStart\": \"2026-10-01\", \"periodEnd\": \"2026-10-31\",
  \"supplierInvoiceAmount\": 8000}")
check "合格计价 300×28=8400" "$(echo "$ST" | jqget "['baseAmount']")" "8400.0"
check "降级折价 10×28×0.6=168" "$(echo "$ST" | jqget "['downgradeAmount']")" "168.0"
check "退货扣减展示 30×28=840" "$(echo "$ST" | jqget "['returnDeduction']")" "840.0"
check "索赔扣减 280" "$(echo "$ST" | jqget "['claimDeduction']")" "280.0"
check "合同扣罚 196+476+100=772" "$(echo "$ST" | jqget "['penaltyAmount']")" "772.0"
check "应付 8400+168-280-772=7516" "$(echo "$ST" | jqget "['finalAmount']")" "7516.0"
check "发票8000≠应付7516 → 不一致标记" "$(echo "$ST" | jqget "['mismatchFlag']")" "True"
ST_ID=$(echo "$ST" | jqget "['id']")
check "结算单确认" "$(curl -s -X POST "$BASE/api/settlements/$ST_ID/confirm" -H "$AUTH_F" | jqget "['status']")" "CONFIRMED"
check "金额不一致自动登记异常" "$(curl -s "$BASE/api/anomalies" -H "$AUTH_H" | python3 -c "
import sys,json
print(any(a['type']=='SETTLEMENT_MISMATCH' for a in json.load(sys.stdin)))")" "True"

echo "== 8. 影响分析 / 异常处理 / 复盘统计 =="
IMPACT=$(curl -s "$BASE/api/acceptances/$ACC_ID/impact" -H "$AUTH_H")
N_MEALS=$(echo "$IMPACT" | python3 -c "import sys,json;print(len(json.load(sys.stdin)['affectedMeals']))")
[ "$N_MEALS" -gt 0 ] && ok "受影响餐次 $N_MEALS 个（含替代食材与供应商责任）" || bad "影响分析"
SUM=$(curl -s "$BASE/api/suppliers/$SUP_ID/anomaly-summary" -H "$AUTH_H")
check "连续异常批次=1" "$(echo "$SUM" | jqget "['consecutiveAnomalyBatches']")" "1"
for AID in $(echo "$ANOMS" | python3 -c "import sys,json;print(' '.join(str(a['id']) for a in json.load(sys.stdin)))"); do
  curl -s -X POST "$BASE/api/anomalies/$AID/resolve" -H "$AUTH_H" -H "Content-Type: application/json" \
    -d '{"causeCategory": "SUPPLIER_PERFORMANCE", "resolutionNote": "已约谈供应商，纳入月度考核"}' > /dev/null
done
check "按供应商统计异常" "$(curl -s "$BASE/api/stats/anomalies?groupBy=supplier" -H "$AUTH_H" | jqget "['totalAnomalies']" )" "6"
check "归因统计(供应商履约)" "$(curl -s "$BASE/api/stats/anomalies/causes" -H "$AUTH_H" | python3 -c "
import sys,json;print(json.load(sys.stdin)['supplierPerformance'] >= 5)")" "True"
ANN_ID=$(curl -s -X POST "$BASE/api/announcements" -H "$AUTH_C" -H "Content-Type: application/json" -d '{
  "title": "关于10月5日午餐菜品调整的通知",
  "content": "因供应商送货部分猪肉检疫票缺失被拒收，当日红烧肉调整为红烧鸡腿，特此告知。",
  "mealDate": "2026-10-05", "mealType": "LUNCH", "dishName": "米饭+红烧肉+清炒青菜+苹果"}' | jqget "['id']")
check "家长公告发布" "$(curl -s -X POST "$BASE/api/announcements/$ANN_ID/publish" -H "$AUTH_C" | jqget "['status']")" "PUBLISHED"
check "安全库存预警接口" "$(curl -s "$BASE/api/inventory/below-safety" -H "$AUTH_H" | python3 -c "
import sys,json;print(len(json.load(sys.stdin)) >= 1)")" "True"

echo "== 9. Swagger / OpenAPI =="
curl -s -o /dev/null -w "%{http_code}" "$BASE/v3/api-docs" | grep -q 200 && ok "OpenAPI 文档 /v3/api-docs" || bad "OpenAPI 文档"
curl -s -o /dev/null -w "%{http_code}" "$BASE/swagger-ui/index.html" | grep -q 200 && ok "Swagger UI" || bad "Swagger UI"

echo ""
echo "==================================="
echo "验证完成: 通过 $PASS 项, 失败 $FAIL 项"
[ "$FAIL" = "0" ] && echo "ALL CHECKS PASSED" || exit 1
