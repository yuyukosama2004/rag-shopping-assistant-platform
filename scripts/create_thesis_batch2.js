const fs = require("fs");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, HeadingLevel, BorderStyle, WidthType,
  ShadingType, PageBreak, PageNumber, TableOfContents
} = require("docx");

// ============================================================
// 常量配置
// ============================================================
const FONT_BODY = "宋体";
const FONT_HEADING = "黑体";
const FONT_EN = "Times New Roman";
const SIZE_BODY = 24;
const SIZE_HEADING1 = 32;
const SIZE_HEADING2 = 28;
const SIZE_HEADING3 = 24;
const SIZE_TITLE = 36;
const SIZE_COVER_SCHOOL = 52;
const LINE_SPACING = 360;

const noBorder = { style: BorderStyle.NONE, size: 0 };
const noBorders = { top: noBorder, bottom: noBorder, left: noBorder, right: noBorder };

function body(text, opts = {}) {
  return new Paragraph({
    spacing: { line: LINE_SPACING },
    indent: opts.noIndent ? undefined : { firstLine: 480 },
    alignment: opts.align || AlignmentType.JUSTIFIED,
    ...(opts.extra || {}),
    children: [new TextRun({ text, font: opts.font || FONT_BODY, size: opts.size || SIZE_BODY, bold: opts.bold || false })],
  });
}

function h1(text) { return new Paragraph({ heading: HeadingLevel.HEADING_1, spacing: { line: LINE_SPACING, before: 240, after: 120 }, children: [new TextRun({ text, font: FONT_HEADING, size: SIZE_HEADING1, bold: true })] }); }
function h2(text) { return new Paragraph({ heading: HeadingLevel.HEADING_2, spacing: { line: LINE_SPACING, before: 180, after: 100 }, children: [new TextRun({ text, font: FONT_HEADING, size: SIZE_HEADING2, bold: true })] }); }
function h3(text) { return new Paragraph({ heading: HeadingLevel.HEADING_3, spacing: { line: LINE_SPACING, before: 120, after: 60 }, children: [new TextRun({ text, font: FONT_HEADING, size: SIZE_HEADING3, bold: true })] }); }
function empty() { return new Paragraph({ spacing: { line: LINE_SPACING }, children: [new TextRun({ text: "", font: FONT_BODY, size: SIZE_BODY })] }); }

function imgPlace(label) {
  return [
    empty(),
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { line: 300 }, children: [new TextRun({ text: `[ 此处插入${label} ]`, font: FONT_BODY, size: 20, italics: true, color: "888888" })] }),
    new Paragraph({ spacing: { line: 240, after: 80 }, alignment: AlignmentType.CENTER, children: [new TextRun({ text: label, font: FONT_HEADING, size: 20, bold: true })] }),
    empty(),
  ];
}

function pageBreak() { return new Paragraph({ children: [new PageBreak()] }); }

// ============================================================
// 封面
// ============================================================
function buildCover() {
  const children = [];
  for (let i = 0; i < 8; i++) children.push(empty());
  children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { line: 600 }, children: [
    new TextRun({ text: "淮", font: FONT_HEADING, size: SIZE_COVER_SCHOOL, bold: true }),
    new TextRun({ text: "  阴  工  学  院", font: FONT_HEADING, size: SIZE_COVER_SCHOOL, bold: true }),
  ]}));
  children.push(empty());
  children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { line: 500 }, children: [new TextRun({ text: "毕业设计（论文）", font: FONT_HEADING, size: SIZE_TITLE, bold: true })] }));
  children.push(empty()); children.push(empty());
  const info = [
    ["学生姓名：", "汪俣珩", "学    号：", "112204150143"],
    ["学    院：", "计算机与软件工程学院", "", ""],
    ["专    业：", "软件工程（软件1221）", "", ""],
    ["设计（论文）题目：", "基于Spring Cloud微服务架构的", "", ""],
    ["", "手机电商平台设计与实现", "", ""],
    ["校内指导老师：", "王媛媛  副教授", "", ""],
    ["校外指导老师：", "汪涛", "", ""],
  ];
  children.push(new Paragraph({ alignment: AlignmentType.CENTER, children: [new Table({
    width: { size: 8000, type: WidthType.DXA }, columnWidths: [2200, 3500, 1000, 1300],
    rows: info.map(row => new TableRow({ children: row.map((c, i) => new TableCell({
      borders: noBorders, width: { size: [2200, 3500, 1000, 1300][i], type: WidthType.DXA },
      margins: { top: 40, bottom: 40, left: 60, right: 60 }, verticalAlign: "center",
      children: [new Paragraph({ alignment: (i === 0 || i === 2) ? AlignmentType.RIGHT : AlignmentType.LEFT, children: [new TextRun({ text: c, font: FONT_BODY, size: SIZE_BODY })] })],
    })) })),
  })] }));
  children.push(empty()); children.push(empty());
  children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { line: 500 }, children: [new TextRun({ text: "2026年5月", font: FONT_BODY, size: SIZE_BODY })] }));
  children.push(pageBreak());
  return children;
}

// ============================================================
// 中文摘要
// ============================================================
function buildAbstractCN() {
  return [
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 200, after: 200 }, children: [new TextRun({ text: "毕业设计（论文）中文摘要", font: FONT_HEADING, size: SIZE_HEADING2, bold: true })] }),
    empty(),
    body("随着数字化经济的蓬勃发展与移动支付的全面普及，电子商务在现代商业体系中的战略地位日益凸显。手机电商平台作为垂直电商的典型代表，面临的业务场景具有鲜明特殊性：手机作为高客单价、强关注度的商品，在新品首发、秒杀大促等特定活动期间，往往在极短时间内引发海量并发访问请求，同时业务链路涵盖复杂的商品规格检索、库存精准锁定、分布式订单生成与支付回调等高度耦合的环节，这对系统的并发处理能力、数据一致性保障以及整体架构的容灾可用性提出了极其严苛的要求。"),
    body("本课题立足于上述现实挑战，旨在构建一个能够抵御瞬时高并发冲击、保障核心交易链路稳定运行的高可用分布式手机电商平台。系统基于Spring Cloud Alibaba微服务生态体系进行整体架构设计，采用前后端分离的开发模式。后端采用Spring Boot框架与Spring Cloud Gateway网关作为统一入口，以Nacos为服务注册与配置中心；前端基于Vue.js 3框架结合Element Plus组件库构建响应式用户界面。针对手机电商高并发场景下的核心痛点，系统深度集成了Redis分布式缓存与RabbitMQ消息队列，通过Lua脚本实现原子性的库存预扣减，利用消息队列异步削峰将峰值写入流量转化为平缓的后台消费流，从而有效保障了MySQL数据持久层的稳定性。在流量防护层面，引入Sentinel流量哨兵在网关层实施细粒度的QPS限流与熔断降级策略，确保系统在资源红线下的存活能力。此外，系统创新性地接入OpenRouter API调用大语言模型，构建了对话式的场景化AI智能导购模块，能够理解用户的自然语言非结构化需求，结合数据库中的结构化商品参数通过Prompt工程输出个性化推荐，实现了业务形态由"人找货"向"智能交互匹配"的跨越。系统全部服务采用Docker Compose进行容器化编排部署，通过严格的物理资源上限约束实现了轻量化、高可用的运行环境。"),
    body("系统测试结果表明，该平台功能完整、运行稳定，在高并发下单场景下表现出良好的吞吐量与容错能力，AI导购模块能够根据用户描述的预算及功能偏好给出具有参考价值的机型推荐，达到了预期的设计目标。"),
    empty(),
    body("关键词：Spring Cloud，微服务，电商平台，高并发，智能导购，Redis", { noIndent: true, bold: true }),
    pageBreak(),
  ];
}

// ============================================================
// 英文摘要
// ============================================================
function buildAbstractEN() {
  return [
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 200, after: 100 }, children: [new TextRun({ text: "毕业设计（论文）外文摘要", font: FONT_HEADING, size: SIZE_HEADING2, bold: true })] }),
    empty(),
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 }, children: [new TextRun({ text: "Design and Implementation of Mobile E-commerce Platform Based on Spring Cloud Microservice Architecture", font: FONT_EN, size: SIZE_BODY, bold: true })] }),
    empty(),
    body("With the vigorous development of the digital economy and the widespread adoption of mobile payment, e-commerce has become increasingly prominent in modern business systems. As a typical representative of vertical e-commerce, mobile phone e-commerce platforms face distinctive challenges: mobile phones, as high-value and high-attention commodities, often trigger massive concurrent access requests within extremely short periods during new product launches and flash sales. The business chain involves complex processes such as product specification retrieval, precise inventory locking, distributed order generation, and third-party payment callbacks, which impose stringent requirements on system concurrency processing capability, data consistency assurance, and overall architecture fault tolerance.", { font: FONT_EN }),
    body("This project addresses these practical challenges by constructing a highly available distributed mobile e-commerce platform capable of withstanding instantaneous high-concurrency shocks and ensuring stable operation of the core transaction chain. The system adopts a microservice architecture based on the Spring Cloud Alibaba ecosystem, employing a front-end and back-end separation development model. The back-end utilizes the Spring Boot framework with Spring Cloud Gateway as the unified entry point and Nacos as the service registration and configuration center. The front-end is built on the Vue.js 3 framework combined with the Element Plus component library to create a responsive user interface. To address the core pain points of high-concurrency mobile e-commerce scenarios, the system deeply integrates Redis distributed caching and RabbitMQ message queues, implementing atomic inventory pre-deduction through Lua scripts and utilizing message queue asynchronous peak-shaving to convert peak write traffic into smooth background consumption flows, thereby effectively ensuring the stability of the MySQL data persistence layer. In terms of traffic protection, Sentinel is introduced to implement fine-grained QPS rate limiting and circuit-breaking degradation strategies at the gateway layer, ensuring system survivability under resource constraints. Furthermore, the system innovatively integrates the OpenRouter API to invoke large language models, constructing a conversational scenario-based AI shopping assistant module capable of understanding users' unstructured natural language requirements. All services are deployed through Docker Compose container orchestration with strict resource constraints to achieve a lightweight, highly available runtime environment.", { font: FONT_EN }),
    body("System testing results demonstrate that the platform is functionally complete and operates stably, exhibiting good throughput and fault tolerance in high-concurrency order scenarios. The AI shopping assistant module can provide valuable model recommendations based on user-described budgets and functional preferences, achieving the expected design objectives.", { font: FONT_EN }),
    empty(),
    body("Keywords: Spring Cloud, Microservices, E-commerce Platform, High Concurrency, AI Shopping Assistant, Redis", { font: FONT_EN, noIndent: true, bold: true }),
    pageBreak(),
  ];
}

// ============================================================
// 目录
// ============================================================
function buildTOC() {
  return [
    new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 200, after: 200 }, children: [new TextRun({ text: "目  录", font: FONT_HEADING, size: SIZE_HEADING1, bold: true })] }),
    empty(),
    new TableOfContents("目录", { hyperlink: true, headingStyleRange: "1-3" }),
    pageBreak(),
  ];
}

// ============================================================
// 第1章 绪论
// ============================================================
function buildChapter1() {
  return [
    h1("1 绪论"),
    h2("1.1 研究背景及意义"),
    body("随着信息技术的飞速发展与移动互联网的深度普及，电子商务已成为全球经济体系中不可或缺的重要组成部分。中国互联网络信息中心（CNNIC）发布的第55次《中国互联网络发展状况统计报告》显示，截至2025年12月，我国网络购物用户规模已突破10亿，电商交易总额持续保持两位数增长态势。在消费品类中，智能手机凭借其高频更新换代周期与较高的单品价值，成为电商平台的核心销售品类之一。根据权威市场调研机构Counterpoint Research的统计数据，2025年中国智能手机在线销量占整体市场的比例已超过40%，且这一比例在"双十一"、"618"等大促活动期间呈现出更加集中的爆发特征。"),
    body("手机电商平台面临的业务场景具有鲜明的特殊性。首先，手机作为高客单价商品，消费者的购买决策通常涉及复杂的跨品牌参数对比，包括处理器（SoC）、屏幕规格、影像系统、电池续航、快充协议等十余个技术维度，传统电商简单的"图文+详情页"展示模式已无法满足用户深度对比的需求。其次，在新品首发、限时秒杀等特定活动期间，平台往往在极短时间内承受海量并发访问请求——以国产主流品牌旗舰机型的首发为例，单场活动的并发峰值QPS可达数万级别，这远超传统单体架构的应用承载能力。此外，高并发下单的业务链路涉及商品检索、库存锁定、订单生成、支付回调等多个强耦合环节，任何一个节点的性能瓶颈或故障都可能在极短时间内引发连锁反应，导致系统整体服务的不可用，即工程领域所称的"雪崩效应"。因此，如何构建一套能够从容应对瞬时流量冲击、保障核心交易链路稳定运行的高可用技术架构，已成为当前电子商务领域与软件工程学科共同关注的核心研究课题。"),
    body("微服务架构作为一种现代化的分布式系统设计范式，通过将庞大复杂的系统业务按照业务领域进行合理切分，形成若干个独立部署、独立扩展的轻量级服务单元，能够显著提升系统的灵活性、容错能力与持续交付效率。然而，在实际的工程落地中，尤其是在中小企业的研发部署环境下，过度细粒度的微服务拆分反而会带来严重的JVM内存开销、服务间RPC网络通信延迟以及复杂的分布式运维成本。近年来的软件工程研究开始关注"微服务过载"现象，提出基于逻辑独立性和物理性能消耗的综合评估模型来进行微服务边界的合理提取与合并。这一"适度聚合"的架构理念，为本课题在有限硬件资源约束下的微服务治理提供了重要的理论支撑。"),
    body("与此同时，人工智能技术的成熟正在深刻改变电商行业的交互范式。大语言模型（LLM）凭借其强大的自然语言理解与生成能力，为电商推荐系统带来了从"被动搜索"到"主动交互"的转型机遇。传统电商平台的推荐机制主要依赖协同过滤或标签规则匹配，无法理解用户的非结构化自然语言需求——例如当用户描述"预算三千元左右，主要用来打游戏和拍照"时，基于规则的系统难以进行准确的语义解析与精准的商品匹配。而大语言模型能够在此类场景中发挥其语义理解优势，结合结构化的商品数据库实现情境化的个性化导购，这正是本课题在AI赋能电商领域探索的核心创新方向之一。"),
    body("综上所述，本课题旨在结合分布式微服务架构的前沿技术与大语言模型的智能交互能力，提出并实现一种基于Spring Cloud Alibaba微服务生态的轻量级手机电商平台。课题的研究意义主要体现在以下几个方面：在理论层面，探索了如何通过"核心链路独立、非核心业务聚合"的适度拆分策略来平衡业务解耦与系统物理开销之间的张力，为资源受限环境下的微服务架构演进提供工程参考；在技术层面，深度应用Redis Lua原子操作、RabbitMQ异步削峰与Sentinel严苛限流等关键技术，构建了一套完整的"前置拦截——缓存预扣——异步落库——柔性补偿"四阶段高并发防护体系；在应用层面，创新性地将大语言模型融入电商导购业务链路，通过Prompt工程实现从自然语言需求到结构化商品推荐的智能匹配，验证了AI技术在垂直电商领域的实用价值。"),
    h2("1.2 国内外研究现状"),
    h3("1.2.1 电商系统架构演进"),
    body("电商系统的技术架构在过去十余年间经历了从传统单体架构向面向服务架构（SOA）、再到全面微服务架构的深刻转变。早期的电商平台通常采用单体架构进行开发与部署，所有业务模块——用户管理、商品管理、订单处理、库存控制等——被打包在同一个应用部署单元中，共享同一套JVM进程与数据库连接池。这种架构模式在业务规模较小、用户访问量有限的阶段具有开发效率高、测试部署简单等优势。然而，随着业务复杂度的提升和用户规模的增长，单体架构的固有缺陷逐渐暴露：代码耦合严重导致维护成本呈指数增长，模块间缺乏有效隔离使得局部故障容易扩散为全局不可用，且无法针对热点模块（如秒杀场景下的订单模块）进行独立的弹性扩容。"),
    body("为解决上述问题，业界逐步引入面向服务架构（SOA）的理念，将系统功能拆分为粗粒度的服务单元，通过企业服务总线（ESB）进行集中式的服务编排与消息路由。SOA在一定程度上缓解了单体架构的耦合问题，但ESB本身成为了新的集中式瓶颈，且SOA的服务粒度通常较粗，未能充分实现独立部署与独立扩展。"),
    body("近年来，微服务架构已成为构建复杂高并发系统的主流选择。与SOA相比，微服务强调以业务领域为边界进行更细粒度的服务划分，每个微服务拥有独立的数据存储、独立的部署流水线与独立的横向扩展能力。在国际上，微服务架构在大型电商平台中得到了广泛验证。Amazon在微服务化转型过程中，将原有的单体电商应用拆分为数百个独立微服务，每个服务拥有专属的数据库与缓存层，通过事件驱动架构实现跨服务的异步通信与最终一致性。Netflix作为微服务架构的先驱实践者，开发了包括Zuul网关、Eureka注册中心、Hystrix熔断器在内的一整套微服务基础设施组件，为行业提供了宝贵的开源参考范例。"),
    body("当前国际学术界和工程界的微服务研究重点已从基础的架构拆分转向更为深层次的问题，包括微服务系统中的事件驱动通信、服务隔离机制以及架构演进策略等。MACH（Microservices, API-first, Cloud-native, Headless）架构模式正被广泛应用于现代数字化零售平台的重构中，以实现系统模块的完全独立与高度灵活。同时，业界领先的电商平台普遍利用事件驱动与分布式追踪技术，将微服务架构下的响应时间与容错隔离能力提升到了新的量级。"),
    h3("1.2.2 高并发治理与流量防护技术"),
    body("在国内，"双十一"、"618"等大规模促销活动的常态化，驱动着国内互联网企业和学术界在微服务架构的"高并发治理"与"流量防护"方面积累了处于世界前沿的技术底蕴。以阿里巴巴、京东为代表的国内头部电商平台，经过多年大促场景的实战锤炼，已形成了一套成熟的多层防护体系，其核心思想可概括为"前置限流——缓存拦截——异步削峰——柔性补偿"的纵深防御策略。"),
    body("在服务治理层面，Spring Cloud Alibaba作为Spring Cloud生态面向国内场景的增强方案，集成了Nacos、Sentinel、Seata等核心组件，已成为国内中小型互联网企业进行微服务架构落地的首选技术栈。Nacos作为新一代的注册中心与配置中心，在CAP理论中兼顾了可用性与分区容错性，支持服务实例的健康检查、动态路由与基于DNS的服务发现。Sentinel作为流量哨兵组件，通过对每个服务接口实施细粒度的QPS限流、线程数控制、系统自适应保护以及熔断降级等策略，能够在系统资源达到瓶颈时快速执行预设的防护策略，有效防止级联故障的发生。研究表明，在典型的秒杀场景中，将Sentinel部署于网关层并结合热点参数限流，可在达到单机物理处理阈值时将超过承载能力的请求快速拒绝，从而保障核心链路的存活。"),
    body("在数据处理层面，Redis作为一个高性能的键值存储系统，凭借其单线程事件驱动模型和丰富的数据结构支持，被广泛应用于电商系统的缓存屏障构建。业内大量实证研究证实，在高并发读场景下，通过Redis集群构建多级缓存体系可将数据库查询量降低90%以上。而在高并发写场景下，利用Redis Lua脚本的原子执行特性进行库存预扣减，结合RabbitMQ或Apache Kafka等消息中间件进行异步削峰，已成为行业解决秒杀场景下库存超卖与数据库承载压力的共识方案。消息队列的引入将瞬间涌入的峰值写请求转化为后端服务可以平稳消费的异步消息流，有效斩断了并发峰值对MySQL等关系型数据库的直接冲击。"),
    h3("1.2.3 AI技术在电商推荐领域的应用"),
    body("在人工智能技术方面，随着以GPT系列、Claude系列等为代表的大语言模型在自然语言理解与生成能力上的突破性进展，电商行业的产品推荐与用户交互范式正在发生深刻变革。传统的电商推荐系统主要依赖协同过滤、矩阵分解或基于内容的推荐算法，这些方法的共同局限在于仅能基于用户的历史行为数据和商品的标签化特征进行"冷冰冰"的规则匹配，无法理解用户以自然语言方式表达的模糊化、情境化需求——例如当用户表述为"想给父母买一款操作简单的手机"或"需要一款拍照效果好、适合旅游携带的机型"时，传统推荐算法难以捕捉其中的情感倾向与社会化语义。"),
    body("大语言模型的出现为解决上述问题提供了新的技术路径。LLM具备从非结构化自然语言文本中抽取关键意图和约束条件的能力，同时其强大的文本生成能力可以将结构化的商品参数数据转化为流畅自然、富有人情味的推荐理由。在技术实现路径上，基于API调用的云服务模式（如OpenRouter API、OpenAI API等）极大地降低了中小型电商平台集成AI能力的技术门槛与算力成本——平台无需自行训练或部署大模型，只需通过精心设计的Prompt将用户需求与商品数据组织为结构化的上下文窗口，提交至云端LLM进行推理，并以流式响应将推荐结果逐字返回前端。这种"轻量化AI集成"模式，为资源受限环境下的电商智能化升级提供了一条高性价比的技术路径。"),
    body("然而，现有的AI赋能电商研究多集中于通用型全品类电商平台，在垂直品类（如智能手机）电商的精细化管理与推荐方面尚存在研究空白。智能手机的商品参数极其繁杂且高度结构化（涉及SoC型号、制程工艺、GeekBench跑分、屏幕面板类型、像素排列方式等数十个专业技术指标），如何将这些结构化的参数数据与用户的非结构化自然语言需求进行有效的语义对齐，是大语言模型在该垂直领域落地的核心挑战，也是本课题AI导购模块重点解决的关键问题。"),
    h2("1.3 主要工作"),
    body("本课题立足于上述研究背景与技术现状，针对传统手机电商平台在应对瞬时高并发流量时存在的性能瓶颈，以及在满足用户深度选机需求时体现的智能化不足等问题，设计并实现一套基于Spring Cloud微服务架构的手机电商平台。论文主要工作包括以下几个层面："),
    body("（1）轻量化微服务架构设计与实现。针对传统电商庞大微服务体系在单机受限硬件环境下的JVM内存过载问题，实行"适度聚合"策略，将系统精简为网关服务、用户服务、商品服务与订单服务（含库存管理与AI导购）四大核心微服务，在保障业务逻辑独立性的同时降低服务间通信开销与运维复杂度。基于Spring Cloud Gateway构建统一API网关，实现JWT鉴权、动态路由与全局跨域处理；以Nacos为注册配置中心，实现各微服务实例的动态发现与统一配置管理。"),
    body("（2）高并发下单核心链路的防护体系建设。针对手机秒杀场景下瞬时流量冲击的痛点，构建了"前置拦截——缓存预扣——异步落库——柔性补偿"的四阶段纵深防护链路。具体包括：Gateway层集成Sentinel实施网关级别的QPS限流与熔断降级；订单服务层通过Redis Lua脚本实现原子性的商品库存预扣减；将同步的峰值写入请求通过RabbitMQ消息队列转化为后台异步消费流；通过定时任务实现超时未支付订单的库存自动补偿回滚。"),
    body("（3）基于大语言模型的场景化AI智能导购模块。引入OpenRouter API调用云端LLM，设计并实现了一套从自然语言需求解析到结构化商品推荐的完整对话链路。系统首先对用户输入的非结构化需求文本进行意图解析（提取预算区间、品牌偏好、功能侧重等关键约束），随后查询MySQL数据库获取符合价格区间与品牌偏好的候选机型参数列表，将组织好的候选数据与用户需求通过Few-shot Prompt模板拼接为标准化的LLM输入，以Server-Sent Events流式响应的形式将推荐结果逐字返回前端进行渲染展示。"),
    body("（4）分布式中间件的深度集成与性能调优。包括：Redis多级缓存策略的设计与缓存防穿透方案的实现；RabbitMQ消息队列的交换机与队列配置、死信队列的补偿机制设计；Redisson分布式客户端在幂等性校验、分布式锁以及Lua脚本执行上的应用；MyBatis-Plus在分页查询、联合索引优化与逻辑删除方面的运用。"),
    body("（5）前后端分离的完整系统实现。前端基于Vue.js 3框架结合TypeScript语言与Element Plus UI组件库，采用Axios拦截器实现Token注入与统一异常处理，完成了包括用户注册登录、商品浏览与多条件检索、购物车管理、高并发下单与订单追踪、AI导购对话等五个核心功能页面的开发与联调。"),
    body("（6）系统测试与验证。编写功能测试用例覆盖核心业务流程，进行API接口联调验证与性能压力测试，利用JMeter模拟高并发秒杀场景，验证Sentinel限流策略的有效性与系统在资源约束条件下的整体稳定性。"),
    pageBreak(),
  ];
}

// ============================================================
// 第2章 相关理论与技术
// ============================================================
function buildChapter2() {
  return [
    h1("2 相关理论与技术"),
    body("本章对本课题所涉及的核心技术栈进行系统性的理论阐述，为后续章节的系统设计与实现提供技术基础。内容涵盖Spring Cloud Alibaba微服务生态组件、分布式中间件技术、前端框架以及大语言模型集成技术。"),
    h2("2.1 Spring Boot与Spring Cloud Alibaba微服务架构"),
    body("Spring Boot是由Pivotal团队开发的一个基于Spring框架的快速应用开发脚手架，其核心设计理念是"约定优于配置"。通过Starter依赖管理、自动配置（Auto-Configuration）和内嵌Servlet容器（如Tomcat）三大核心机制，Spring Boot极大地简化了企业级Java应用的开发流程。开发者无需编写繁琐的XML配置文件，只需引入相应的Starter依赖，Spring Boot即可根据类路径中存在的类自动配置对应的功能组件。本课题的所有微服务均基于Spring Boot 3.2框架构建，利用其成熟的生态与稳定的运行时表现支撑核心业务逻辑的执行。"),
    body("Spring Cloud是一套基于Spring Boot的分布式系统开发工具集，为微服务架构中常见的服务注册与发现、配置管理、智能路由、负载均衡、熔断降级等问题提供了一站式的解决方案。Spring Cloud Alibaba是由阿里巴巴与Spring Cloud社区联合推出的子项目，针对国内开发者的使用习惯与业务场景进行了深度定制与增强。其核心组件包括：Nacos（服务注册发现与动态配置管理）、Sentinel（流量控制与系统防护）、RocketMQ（分布式消息中间件）、Seata（分布式事务解决方案）以及Dubbo Spring Cloud（高性能RPC通信框架）等。本课题主要采用Nacos、Sentinel与Spring Cloud Gateway三款组件构建微服务治理基础设施。"),
    h2("2.2 Spring Cloud Gateway网关"),
    body("Spring Cloud Gateway是Spring Cloud生态系统中的第二代API网关，基于Spring WebFlux的响应式编程模型构建，底层采用Netty作为网络通信引擎。与第一代网关Zuul相比，Spring Cloud Gateway在吞吐量和内存效率方面有显著提升，其非阻塞I/O模型能够以更少的线程资源处理更高的并发连接数。Spring Cloud Gateway的核心工作机制基于"路由（Route）——断言（Predicate）——过滤器（Filter）"三元组模型：每个路由包含一个目标URI与一组断言条件，当请求满足断言条件时，网关将该请求转发至目标URI；过滤器链则允许在请求转发前或响应返回后对HTTP消息进行拦截与修改。"),
    body("在本课题的架构中，Spring Cloud Gateway承担着统一入口的关键角色，负责三项核心职责：其一，基于JWT Token的全局用户认证与鉴权，通过自定义GlobalFilter实现Token的解析、校验与白名单放行逻辑；其二，基于Nacos注册中心的动态路由转发，通过lb://service-name协议实现对下游微服务实例的负载均衡调用；其三，全局CORS（跨域资源共享）配置，保障前端开发环境对后端API的合法跨域访问。"),
    h2("2.3 Nacos服务注册与配置中心"),
    body("Nacos（Naming and Configuration Service）是阿里巴巴开源的一款集服务发现、配置管理和服务元数据管理于一体的动态服务发现平台。在微服务架构中，服务实例的网络地址是动态变化的（受容器调度、扩缩容、故障恢复等因素影响），Nacos通过服务注册与健康检查机制，使得消费者无需硬编码服务提供者的地址即可实现透明的服务调用。"),
    body("在本课题中，Nacos以Standalone单机模式部署，四个微服务（网关服务、用户服务、商品服务、订单服务）在启动时向Nacos上报自身的地址、端口与健康状态，网关通过Nacos发现下游微服务实例并建立动态路由表。Nacos的配置管理功能为系统提供了统一的配置下发能力，各微服务无需在本地维护冗余的配置文件，实现了配置管理的集中化与环境隔离。"),
    h2("2.4 Redis缓存与Lua脚本机制"),
    body("Redis（Remote Dictionary Server）是一款开源的基于内存的键值存储系统，官方将其定位为"数据结构服务器"。Redis支持包括字符串（String）、哈希（Hash）、列表（List）、集合（Set）与有序集合（Sorted Set）在内的多种数据结构，同时提供了发布订阅、Lua脚本、事务与持久化等高级功能。Redis的单线程事件驱动架构从根本上消除了并发竞争条件，使其成为实现分布式环境下原子性操作的最优选择之一。"),
    body("在本课题的架构中，Redis承担着两个核心角色——多级缓存屏障与原子库存预扣减。在缓存屏障层面，系统利用Spring Cache抽象结合Redisson客户端，对商品详情、热门推荐与筛选条件等高频率读取数据建立多级缓存，将大量查询请求拦截在数据库之上，大幅削减MySQL的I/O开销，同时通过缓存空值策略防止缓存穿透。在库存预扣预层面，系统向Redis注入自定义Lua脚本——由于Redis的单线程特性，整个Lua脚本作为一个不可分割的执行单元在Redis服务端原子化完成，从根本上杜绝了分布式环境下的"超卖"问题。Redisson作为Redis的Java高级客户端，不仅封装了基本的KV操作，还提供了分布式锁、布隆过滤器、原子长整型以及Lua脚本执行等高级分布式工具的Java接口。"),
    h2("2.5 RabbitMQ消息队列"),
    body("RabbitMQ是基于AMQP（Advanced Message Queuing Protocol）协议实现的开源消息代理软件，采用Erlang语言编写，具备高可靠性、灵活的路由规则与完善的集群支持。RabbitMQ的核心模型由生产者（Producer）、交换机（Exchange）、队列（Queue）和消费者（Consumer）四类角色组成：生产者将消息发送至指定的交换机，交换机根据绑定的路由规则将消息分发到相应的队列，消费者从队列中异步拉取或被动接收消息进行处理。"),
    body("在本课题的高并发下单场景中，RabbitMQ承担着异步削峰与请求解耦的关键角色。当秒杀请求经过Sentinel的流量评估和Redis的原子预扣后，订单服务并不直接在MySQL中创建订单记录，而是将订单信息封装为消息投递至RabbitMQ交换机，随后立即向客户端响应"下单处理中"。订单服务的MQ消费者以可控的平缓速率从队列中拉取消息，在本地事务的保护下完成订单入库与库存物理扣减。这一机制将瞬时的峰值写入请求彻底转化为平滑的后台处理流，使得MySQL在面对前端高并发冲击时能够保持连接的稳定。此外，通过配置队列的Dead Letter Exchange（死信交换机），系统可在订单超时未被正常处理时将消息路由至死信队列，触发库存回滚的补偿逻辑。"),
    h2("2.6 Sentinel流量防护"),
    body("Sentinel是阿里巴巴开源的面向分布式服务架构的流量控制与系统防护组件。其核心功能包括：流量控制（根据QPS、并发线程数等指标对服务调用进行限流）、熔断降级（当调用链路中某个资源出现不稳定状态时，快速切断对该资源的调用，防止级联故障扩散）、系统自适应保护（根据系统整体Load、CPU使用率、平均RT等指标自主调整流量入口）以及热点参数限流（针对携带特定参数值的请求进行精细化的限流控制）。"),
    body("Sentinel的流量控制算法基于滑动窗口的计数统计，能够在秒级精度下对通过的请求量进行实时估算。当QPS超过设定的阈值时，Sentinel根据配置的流控效果（快速失败、Warm Up预热、排队等待）执行相应的流量整形策略。在本课题中，Sentinel被集成于Spring Cloud Gateway网关层，针对高并发下单接口设定独立的QPS限流规则。当超过系统物理处理阈值时，限流策略立即触发，向客户端返回包含友好提示信息的JSON响应（HTTP 429状态码），有效防止并发流量穿透缓存层直接冲击数据库。"),
    h2("2.7 Vue.js 3前端框架与Element Plus"),
    body("Vue.js是由尤雨溪开发的渐进式JavaScript框架，其3.x版本在保留2.x版本响应式数据绑定与声明式渲染核心优势的基础上，通过引入基于Proxy的响应式系统、Composition API组合式编程范式以及TypeScript原生支持，大幅提升了框架的性能表现与开发体验。Vue 3的虚拟DOM采用静态标记优化与Block Tree结构，能够在编译阶段识别出静态节点并跳过其Diff过程，显著提升了大列表页面的渲染性能。Element Plus是基于Vue 3的桌面端UI组件库，提供了包括表单、表格、对话框、导航菜单、卡片等在内的近百个高质量组件，是Vue 3生态系统中最广泛使用的中后台UI解决方案之一。"),
    body("在本课题的前端实现中，基于Vue 3的Composition API结合TypeScript类型系统进行组件化开发，使用Pinia作为全局状态管理库替代Vuex，使用Vue Router 4实现页面路由与导航守卫。整个前端工程通过Vite构建工具进行开发与打包，享受其基于ESBuild的极速冷启动和热模块替换（HMR）能力。"),
    h2("2.8 Docker容器化部署技术"),
    body("Docker是一种基于Linux容器技术的轻量级虚拟化解决方案，其核心设计基于操作系统级别的进程隔离而非传统的硬件虚拟化，因此相比传统虚拟机具有更低的资源开销与更快的启动速度。Docker通过镜像将应用及其运行时依赖捆绑为不可变的部署单元，确保应用在开发、测试与生产环境中的一致性运行。Docker Compose是Docker官方提供的多容器编排工具，通过YAML配置文件定义多个容器的启动顺序、网络拓扑、存储卷挂载以及资源约束。"),
    body("在本课题的部署方案中，MySQL、Redis、RabbitMQ与Nacos等中间件通过Docker Compose进行一键编排启动，每个容器配置了严格的资源使用上限（CPU与内存限制），以避免在单机环境下个别中间件因资源争抢而影响整体系统的稳定性。"),
    h2("2.9 大语言模型与Prompt工程"),
    body("大语言模型（Large Language Model, LLM）是基于Transformer架构、通过海量文本语料进行预训练的大型神经网络模型。LLM在自然语言理解（NLU）和自然语言生成（NLG）方面展现出了卓越的能力——它能够在不进行任务特定微调的情况下，通过"上下文学习"（In-Context Learning）完成翻译、摘要、推理、代码生成等多种复杂的语言任务。"),
    body("Prompt工程是高效使用大语言模型的核心技术手段。其基本思想是通过精心编写的指令文本（Prompt），引导LLM按照特定格式和逻辑完成期望的推理任务。一个高质量的Prompt通常由四部分组成：角色设定（System Prompt，明确LLM在对话中的身份与职责）、少样本示例（Few-shot Example，为LLM提供1~3个输入输出范例作为格式与逻辑的参照）、结构化上下文（Context，将数据库中的领域数据以结构化的形式（如JSON）嵌入Prompt中）以及用户问题（User Question）。"),
    body("在本课题AI导购模块的实现中，系统构建了专门面向手机导购场景的结构化Prompt模板。模板的角色设定将LLM定位为"拥有多年从业经验的手机产品评测师"，要求其严格依据提供的商品数据做出推荐，不允许虚构不存在的机型；少样本示例展示了预期的推荐格式与行文语气；结构化数据块包含了智能导购微服务从MySQL数据库中查询获得的价格区间候选机型的完整参数列表；用户原始的自然语言需求则直接作为User Question透传给模型。为进一步优化用户体验，系统采用Server-Sent Events（SSE）协议实现LLM响应的流式传输，实现了类似ChatGPT的逐字返回效果。"),
    body("OpenRouter是一个统一的LLM API网关平台，它提供了对多种主流大模型（包括Claude系列、GPT系列等）的统一API访问入口。通过OpenRouter API，开发者无需分别对接不同模型厂商的API规范，而可以通过统一的请求格式调度多个模型，同时对API密钥进行集中管理。在本课题中，系统通过Spring Boot的WebClient向OpenRouter发送POST请求，以SSE流式响应的方式接收LLM的实时推理输出，并逐字推送至前端更新聊天界面。"),
    pageBreak(),
  ];
}

// ============================================================
// 构建文档
// ============================================================
async function build() {
  const bodySection = {
    properties: {
      page: { size: { width: 11906, height: 16838 }, margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } },
    },
    headers: { default: new Header({ children: [new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 60 }, border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: "000000", space: 4 } }, children: [new TextRun({ text: "淮阴工学院毕业设计（论文）", font: FONT_BODY, size: 18 })] })] }) },
    footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: "第 ", font: FONT_BODY, size: 18 }), new TextRun({ children: [PageNumber.CURRENT], font: FONT_BODY, size: 18 }), new TextRun({ text: " 页", font: FONT_BODY, size: 18 })] })] }) },
    children: [
      ...buildAbstractCN(),
      ...buildAbstractEN(),
      ...buildTOC(),
      ...buildChapter1(),
      ...buildChapter2(),
    ],
  };

  const doc = new Document({
    styles: {
      default: { document: { run: { font: FONT_BODY, size: SIZE_BODY } } },
      paragraphStyles: [
        { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: SIZE_HEADING1, bold: true, font: FONT_HEADING }, paragraph: { spacing: { before: 240, after: 120, line: LINE_SPACING }, outlineLevel: 0 } },
        { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: SIZE_HEADING2, bold: true, font: FONT_HEADING }, paragraph: { spacing: { before: 180, after: 100, line: LINE_SPACING }, outlineLevel: 1 } },
        { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: SIZE_HEADING3, bold: true, font: FONT_HEADING }, paragraph: { spacing: { before: 120, after: 60, line: LINE_SPACING }, outlineLevel: 2 } },
      ],
    },
    sections: [
      { properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } } }, children: buildCover() },
      bodySection,
    ],
  });

  const buffer = await Packer.toBuffer(doc);
  fs.writeFileSync("/home/jill/project/biyesheji/毕业论文.docx", buffer);
  console.log("Batch 2 done: 毕业论文.docx (cover + abstracts + TOC + Ch1 + Ch2)");
  console.log("Sections: Cover, AbstractCN, AbstractEN, TOC, Ch1(绪论), Ch2(相关理论与技术)");
}

build().catch(err => { console.error(err); process.exit(1); });
