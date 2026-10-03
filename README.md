# 校园食材供应商订货验收入库与索赔服务

面向学校食堂的食材供应链后端服务：以**周菜单**驱动采购计划，覆盖 **订货 → 送货 → 验收 → 入库/退货/降级 → 异常 → 索赔 → 结算 → 复盘统计** 全流程，保证订货单、送货批次、验收、库存、退货、索赔和财务结算数据一致。

## 技术栈

- Java 17 · Spring Boot 3.3（Web / Data JPA / Security / Validation）
- PostgreSQL 15（测试用 H2）
- JWT 认证 + 角色权限
- springdoc-openapi（Swagger UI）
- Maven · Docker · docker-compose

## 功能与业务规则

### 主流程

1. **周菜单**：食堂按周维护菜单（餐次、菜品、份数、食材需求），发布后生成采购计划。
2. **采购计划**：按食材汇总全周需求，依据供应商合同价格表选**最低价且具备冷链能力**的供应商，按供应商自动拆分订货单（记录规格、数量、要求到货时间、质检要求、合同价、是否冷链及温度上限）。
3. **送货登记**：记录车牌、到校时间、车厢温度、供应商批次号、检疫票。
4. **验收**：验收员逐项核对温度、外观、保质期、抽检、腐烂比例、是否替换品类；录入实送/合格/拒收/降级数量。
   - 合格 → **自动入库**（库存+流水）
   - 拒收 → **自动生成退货单**
   - 降级 → **生成降级使用审批**，批准后折价入库
   - 食堂已备餐无法拒收 → **强制收货**，按折价率计价并登记异常
5. **异常自动检测**（验收完成/结算生成时触发）：

   | 异常类型 | 触发规则 | 扣罚/索赔建议 |
   |---|---|---|
   | 少送 SHORT_DELIVERY | 实送 < 订购 | 索赔=缺量×合同价 |
   | 晚送 LATE_DELIVERY | 到货晚于要求时间 30 分钟以上 | 扣罚=订单金额 2%（≥50 元） |
   | 冷链超标 COLD_CHAIN_BREACH | 冷链品实测温度超过温度上限 | 扣罚=涉及金额 5%，索赔=拒收+降级×合同价 |
   | 腐烂比例高 HIGH_ROT_RATIO | 蔬果腐烂比例 > 5% | 索赔=拒收×合同价 |
   | 票证缺失 MISSING_CERTIFICATE | 肉类/乳制品未提供检疫票 | 扣罚 100 元 |
   | 临时替换品类 SUBSTITUTION | 验收标记替换 | 扣罚=涉及金额 5% |
   | 无法拒收 FORCED_ACCEPTANCE | 已备餐强制收货 | 索赔=拒收量×合同价×(1-折价率) |
   | 重量不符 WEIGHT_MISMATCH | 送货量与验收合计差异 > 0.5% | 提示复核 |
   | 结算不符 SETTLEMENT_MISMATCH | 供应商发票金额 ≠ 按验收重量应付金额 | 提示复核 |
   | 质检不合格 QUALITY_FAILURE | 外观/保质期/抽检不合格拒收 | 索赔=拒收×合同价 |

   异常创建时即计算**受影响餐次数**；处理异常时必须**归因**（天气质量波动 / 供应商履约不稳 / 验收标准执行偏差），并记录处理时长。
6. **索赔**：基于异常发起，财务审批后计入结算扣减。
7. **结算**：`应付 = 合格量×合同价 + 强制收货折价 + 降级量×合同价×折价率 − 已批准索赔 − 合同扣罚`；传入供应商发票金额可自动核对并登记不一致异常；同一验收明细只会进入一张结算单。
8. **影响分析**：验收失败即刻看到受影响餐次（含库存能否覆盖）、替代食材（同品类库存/其他供应商货源）、供应商责任（异常+扣罚+建议索赔），并可发布**家长公告**。
9. **复盘统计**：按供应商/品类/验收员统计异常（类型分布、归因、平均处理时长、扣罚合计、影响餐次）；供应商异常汇总含**连续异常批次**判定；库存安全线预警。

### 角色权限

| 角色 | 账号 | 密码 | 主要权限 |
|---|---|---|---|
| 系统管理员 | `admin` | `admin123` | 全部 |
| 采购负责人 | `buyer` | `buyer123` | 供应商/菜单/计划/订单/异常处理/索赔发起/统计 |
| 验收员 | `inspector` | `inspect123` | 送货登记、验收、库存查看 |
| 财务 | `finance` | `finance123` | 索赔审批、结算生成/确认/付款 |
| 食堂主管 | `canteen` | `canteen123` | 菜单、降级审批、库存出库、家长公告 |

## 快速开始

### 方式一：Docker Compose 一键启动（推荐）

```bash
docker compose up --build
```

启动后：
- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

首次启动自动建表并写入演示数据（账号、5 家供应商及合同价、2026-10-05 周菜单、库存安全线）。

### 方式二：本地 Maven 运行

前置：JDK 17+、Maven 3.6+、PostgreSQL（可用 `docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=campus_food postgres:15`）。

```bash
# 默认连接 jdbc:postgresql://localhost:5432/campus_food（postgres/postgres）
mvn spring-boot:run

# 或用环境变量覆盖连接
DB_URL=jdbc:postgresql://localhost:5432/campus_food DB_USER=postgres DB_PASSWORD=postgres \
  java -jar target/canteen-procurement.jar
```

### 配置项（环境变量）

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/campus_food` | 数据库连接 |
| `DB_USER` / `DB_PASSWORD` | `postgres` / `postgres` | 数据库账号 |
| `SERVER_PORT` | `8080` | 服务端口 |
| `JWT_SECRET` | 开发默认值 | JWT 密钥，生产必须覆盖 |
| `JWT_EXPIRE_MINUTES` | `720` | 令牌有效期 |
| `SEED_ENABLED` | `true` | 是否写入演示数据 |

## 验证方式

### 1. 自动化测试（集成测试，H2）

```bash
mvn test
```

`FullFlowIntegrationTest` 覆盖全流程：登录 → 菜单生成计划（4 张订单）→ 下单 → 送货（晚送/冷链超标/缺票）→ 验收（少送/拒收/降级）→ 库存/退货/降级联动 → 5 类异常 → 降级审批入库 → 索赔审批 → 结算金额精确断言（8400+168−280−772=7516）→ 发票不一致异常 → 影响分析 → 归因统计 → 家长公告。

### 2. 全流程脚本验证（对运行中的服务）

```bash
./scripts/verify.sh            # 默认 http://localhost:8080
./scripts/verify.sh http://host:port
```

脚本逐项检查 34 个断言（登录、计划、订单、送货、验收、库存、退货、降级、异常、索赔、结算、影响分析、统计、公告、Swagger），全部通过输出 `ALL CHECKS PASSED`。

### 3. 手动体验（Swagger UI 或 curl）

```bash
# 登录拿 token
TOKEN=$(curl -s http://localhost:8080/api/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"buyer","password":"buyer123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# 周菜单生成采购计划
curl -X POST "http://localhost:8080/api/menus/1/generate-plan?expectedArrival=2026-10-05T07:00:00" \
  -H "Authorization: Bearer $TOKEN"
```

其余端点见 Swagger UI 的分组说明（认证、供应商、周菜单与采购计划、订货单、送货批次、验收、库存、退货、降级审批、异常、索赔、结算、家长公告、复盘统计）。

## 已执行的验证结论

- `mvn test`：8/8 集成测试通过（H2）。
- 应用在真实 PostgreSQL 15 上以 `java -jar` 启动，`scripts/verify.sh` 34/34 项全部通过。
- Dockerfile 与 docker-compose.yml 已提供；本开发环境无 Docker 守护进程，镜像构建未在本机执行，Compose 配置按标准多阶段构建编写。

## 项目结构

```
src/main/java/com/school/canteen/
├── config/        # 安全、OpenAPI、Jackson、演示数据初始化
├── controller/    # 14 个 REST 控制器
├── dto/           # 请求 DTO
├── entity/        # 20 个 JPA 实体
├── enums/         # 品类/状态/异常类型/归因等枚举
├── exception/     # 统一异常处理
├── repository/    # Spring Data JPA
├── security/      # JWT 签发与过滤器
└── service/       # 业务规则（验收联动、异常检测、结算、影响分析、统计）
scripts/verify.sh  # 全流程验证脚本
```
