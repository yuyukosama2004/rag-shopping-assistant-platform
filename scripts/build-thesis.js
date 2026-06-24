const fs = require("fs");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, LevelFormat,
  TableOfContents, HeadingLevel, BorderStyle, WidthType, ShadingType,
  PageNumber, PageBreak, TabStopType, TabStopPosition
} = require("docx");

// ============================================================
// 论文元数据
// ============================================================
const SCHOOL = "淮 安 大 学";
const TITLE = "毕业设计（论文）";
const THESIS_TITLE = "基于Spring Cloud微服务架构的手机电商平台设计与实现";
const STUDENT_NAME = "汪俣珩";
const STUDENT_ID = "112204150143";
const COLLEGE = "计算机与软件工程学院";
const MAJOR = "软件工程";
const CLASS_NAME = "软件1221";
const INTERNAL_ADVISOR = "王媛媛";
const INTERNAL_TITLE = "副教授";
const EXTERNAL_ADVISOR = "汪涛";

// ============================================================
// 工具函数
// ============================================================
const FONT = "宋体";
const FONT_HEADING = "黑体";
const SIZE_BODY = 24; // 12pt = 24 half-points
const SIZE_H1 = 32;   // 16pt
const SIZE_H2 = 28;   // 14pt
const SIZE_H3 = 26;   // 13pt
const SIZE_SMALL = 21; // 10.5pt
const SIZE_TITLE = 44; // 22pt
const SIZE_SCHOOL = 52; // 26pt

const A4_WIDTH = 11906;
const A4_HEIGHT = 16838;
const MARGIN = 1440; // 1 inch
const CONTENT_WIDTH = A4_WIDTH - 2 * MARGIN; // 9026

function bodyPara(text, opts = {}) {
  return new Paragraph({
    spacing: { line: 360, after: 120 },
    indent: opts.noIndent ? undefined : { firstLine: 480 },
    alignment: opts.align || AlignmentType.JUSTIFIED,
    ...opts,
    children: [new TextRun({ text, font: FONT, size: SIZE_BODY, ...opts.run })]
  });
}

function bodyParaRuns(runs, opts = {}) {
  return new Paragraph({
    spacing: { line: 360, after: 120 },
    indent: opts.noIndent ? undefined : { firstLine: 480 },
    alignment: opts.align || AlignmentType.JUSTIFIED,
    ...opts,
    children: runs
  });
}

function heading(level, text) {
  const sizes = { 1: SIZE_H1, 2: SIZE_H2, 3: SIZE_H3 };
  const hl = { 1: HeadingLevel.HEADING_1, 2: HeadingLevel.HEADING_2, 3: HeadingLevel.HEADING_3 };
  return new Paragraph({
    heading: hl[level],
    spacing: { before: 240, after: 120 },
    alignment: level === 1 ? AlignmentType.CENTER : AlignmentType.LEFT,
    children: [new TextRun({ text, font: FONT_HEADING, size: sizes[level], bold: true })]
  });
}

function emptyLine() {
  return new Paragraph({ spacing: { line: 360 }, children: [new TextRun({ text: "", font: FONT, size: SIZE_BODY })] });
}

function pageBreak() {
  return new Paragraph({ children: [new PageBreak()] });
}

function placeholder(text) {
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 120, after: 120 },
    children: [new TextRun({ text, font: FONT, size: SIZE_SMALL, italics: true, color: "888888" })]
  });
}

// ============================================================
// 分节属性
// ============================================================
function sectionProps({ pageNumbers } = {}) {
  return {
    page: {
      size: { width: A4_WIDTH, height: A4_HEIGHT },
      margin: { top: MARGIN, bottom: MARGIN, left: MARGIN + 200, right: MARGIN + 200 }
    },
    headers: {
      default: new Header({
        children: [new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ text: `${SCHOOL}毕业设计（论文）`, font: FONT, size: SIZE_SMALL, color: "555555" })]
        })]
      })
    },
    footers: {
      default: new Footer({
        children: [new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [
            new TextRun({ text: "第 ", font: FONT, size: SIZE_SMALL }),
            new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: SIZE_SMALL }),
            new TextRun({ text: " 页", font: FONT, size: SIZE_SMALL })
          ]
        })]
      })
    },
  };
}

// ============================================================
// 内容构建
// ============================================================

// --- 封面 ---
function buildCover() {
  const coverSection = {
    properties: {
      page: {
        size: { width: A4_WIDTH, height: A4_HEIGHT },
        margin: { top: 1200, bottom: 1200, left: 1800, right: 1800 }
      }
    },
    children: []
  };

  coverSection.children.push(emptyLine());
  coverSection.children.push(emptyLine());
  coverSection.children.push(new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 600 },
    children: [new TextRun({ text: SCHOOL, font: FONT_HEADING, size: SIZE_SCHOOL, bold: true })]
  }));
  coverSection.children.push(new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 600 },
    children: [new TextRun({ text: TITLE, font: FONT_HEADING, size: SIZE_TITLE, bold: true })]
  }));
  coverSection.children.push(emptyLine());
  coverSection.children.push(emptyLine());

  const infoTable = new Table({
    width: { size: 7000, type: WidthType.DXA },
    columnWidths: [2000, 5000],
    rows: [
      coverRow("学生姓名：", STUDENT_NAME, "学    号：", STUDENT_ID),
      coverRow("学    院：", COLLEGE, "", ""),
      coverRow("专    业：", MAJOR, "", ""),
      coverRow("设计（论文）题目：", THESIS_TITLE.substring(0, 18), "", ""),
      coverRow("", THESIS_TITLE.substring(18), "", ""),
      coverRow("校内指导老师：", INTERNAL_ADVISOR, INTERNAL_TITLE, ""),
      coverRow("校外指导老师：", EXTERNAL_ADVISOR, "", ""),
    ]
  });
  coverSection.children.push(infoTable);
  coverSection.children.push(emptyLine());
  coverSection.children.push(emptyLine());
  coverSection.children.push(new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "2026年5月", font: FONT, size: SIZE_H2 })]
  }));

  return coverSection;
}

function coverRow(label1, val1, label2, val2) {
  const cells = [];
  cells.push(new TableCell({
    width: { size: 2000, type: WidthType.DXA },
    children: [new Paragraph({ children: [new TextRun({ text: label1, font: FONT, size: SIZE_BODY })] })]
  }));
  if (label2) {
    cells.push(new TableCell({
      width: { size: 2000, type: WidthType.DXA },
      children: [new Paragraph({ children: [new TextRun({ text: val1, font: FONT, size: SIZE_BODY })] })]
    }));
    cells.push(new TableCell({
      width: { size: 1400, type: WidthType.DXA },
      children: [new Paragraph({ children: [new TextRun({ text: label2, font: FONT, size: SIZE_BODY })] })]
    }));
    cells.push(new TableCell({
      width: { size: 1600, type: WidthType.DXA },
      children: [new Paragraph({ children: [new TextRun({ text: val2, font: FONT, size: SIZE_BODY })] })]
    }));
  } else {
    cells.push(new TableCell({
      width: { size: 5000, type: WidthType.DXA },
      children: [new Paragraph({ children: [new TextRun({ text: val1, font: FONT, size: SIZE_BODY })] })]
    }));
  }
  return new TableRow({ children: cells });
}

// --- 中文摘要 ---
function buildChineseAbstract() {
  const children = [];
  children.push(emptyLine());
  children.push(new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 360, after: 360 },
    children: [new TextRun({ text: "毕业设计（论文）中文摘要", font: FONT_HEADING, size: SIZE_H2, bold: true })]
  }));
  children.push(bodyPara("随着数字化经济的持续快速发展与移动支付的全面普及，电子商务在现代商业中的战略地位日益凸显，尤其是智能手机类数码产品的线上零售呈现出爆发式增长态势。手机电商平台面临的业务场景具有鲜明的特殊性：一方面，手机作为高客单价、强关注度的商品，在新品首发、秒杀大促等特定活动期间，往往会在极短的时间内引发海量的并发访问请求；另一方面，业务链路涵盖了复杂的商品规格检索、库存精准锁定、分布式订单生成与第三方支付回调等高度耦合的环节。这种激增的瞬时流量不仅考验着平台的网络带宽，更对系统后端的业务处理吞吐量、跨服务数据强一致性以及整体架构的容灾可用性提出了极其严苛的要求。"));
  children.push(bodyPara("本课题立足于B2C手机电商平台的实际业务流转逻辑，针对传统单体架构在面对瞬时高并发流量时存在的代码严重耦合、扩展性差、单点故障率高等固有限制，采用Spring Cloud Alibaba微服务生态作为底层架构，结合Vue.js前端框架、Redis分布式缓存、RabbitMQ消息队列以及Sentinel流量治理组件，设计并实现了一套能够抵御瞬时高并发冲击的高可用分布式手机电商平台。系统采用前后端分离的架构模式，将核心交易链路拆分为用户、商品、订单三大独立的微服务模块，通过"Redis Lua原子预扣库存 + RabbitMQ异步削峰 + 本地事务落库"的柔性下单工作流程，在有限硬件资源约束下实现了核心链路的高吞吐量保障。同时，系统创新性地引入大语言模型（LLM）技术，通过接入OpenRouter API构建了基于自然语言理解的场景化AI智能导购模块，实现了从传统的"标签/规则匹配"向"语义理解与个性化推荐"的跨越。"));
  children.push(bodyPara("系统测试结果表明，本平台在功能完整性和性能指标方面均达到了预期设计目标，核心下单接口在单机部署条件下可实现秒级响应，Sentinel限流组件有效保障了系统在过载场景下的服务存活率。本课题的研究成果不仅为中小型电商企业提供了一套低成本、可落地的微服务架构方案，也为资源受限环境下的微服务适度拆分与轻量化部署提供了具有参考价值的工程实践经验。"));
  children.push(emptyLine());
  children.push(new Paragraph({
    spacing: { line: 360 },
    children: [
      new TextRun({ text: "关键词 ", font: FONT_HEADING, size: SIZE_BODY, bold: true }),
      new TextRun({ text: "微服务架构；手机电商平台；Spring Cloud Alibaba；高并发；人工智能导购；容器化", font: FONT, size: SIZE_BODY })
    ]
  }));
  return children;
}

// --- 英文摘要 ---
function buildEnglishAbstract() {
  const children = [];
  children.push(pageBreak());
  children.push(emptyLine());
  children.push(new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 360, after: 360 },
    children: [new TextRun({ text: "毕业设计（论文）外文摘要", font: FONT_HEADING, size: SIZE_H2, bold: true })]
  }));
  children.push(new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 240 },
    children: [new TextRun({ text: "Design and Implementation of Mobile Phone E-commerce Platform Based on Spring Cloud Microservice Architecture", font: "Times New Roman", size: SIZE_H2, bold: true })]
  }));
  children.push(new Paragraph({
    spacing: { before: 120 },
    children: [new TextRun({ text: "Abstract", font: "Times New Roman", size: SIZE_BODY, bold: true })]
  }));
  children.push(bodyPara("With the sustained and rapid development of the digital economy and the widespread adoption of mobile payments, e-commerce has assumed an increasingly prominent strategic position in modern commerce, particularly in the online retail of smartphones, which has demonstrated explosive growth. Mobile phone e-commerce platforms face distinctive business challenges: as high-value, high-attention commodities, smartphones often trigger massive concurrent access requests within extremely short timeframes during new product launches and flash sale events. Meanwhile, the business chain encompasses highly coupled processes such as complex product specification retrieval, precise inventory locking, distributed order generation, and third-party payment callbacks.", { noIndent: true, run: { font: "Times New Roman" } }));
  children.push(bodyPara("This project addresses the practical business flow of B2C mobile phone e-commerce platforms. To overcome the inherent limitations of traditional monolithic architectures—namely severe code coupling, poor scalability, and high single-point failure rates—when facing instantaneous high-concurrency traffic, this system adopts the Spring Cloud Alibaba microservice ecosystem as its underlying architecture. Combined with the Vue.js frontend framework, Redis distributed caching, RabbitMQ message queues, and Sentinel traffic governance components, a high-availability distributed mobile phone e-commerce platform capable of withstanding instantaneous high-concurrency surges has been designed and implemented. The system employs a frontend-backend separation architecture, splitting the core transaction chain into three independent microservices: user, product, and order. Through a flexible order placement workflow featuring \"Redis Lua atomic stock pre-deduction + RabbitMQ asynchronous traffic shaping + local transactional database persistence,\" high throughput of the core business chain is achieved under constrained hardware resources. Additionally, the system innovatively integrates Large Language Model (LLM) technology via the OpenRouter API to construct a scenario-based AI intelligent shopping assistant module grounded in natural language understanding, achieving a leap from traditional \"tag/rule matching\" to \"semantic understanding and personalized recommendation.\"", { noIndent: true, run: { font: "Times New Roman" } }));
  children.push(bodyPara("System testing results demonstrate that the platform meets all expected design goals in terms of functional completeness and performance metrics. The core order placement interface achieves sub-second response times under single-machine deployment conditions, and the Sentinel rate-limiting component effectively ensures service survival rates under overload scenarios. The research outcomes of this project not only provide a low-cost, deployable microservice architecture solution for small and medium-sized e-commerce enterprises but also offer valuable engineering practice references for moderate microservice decomposition and lightweight deployment in resource-constrained environments.", { noIndent: true, run: { font: "Times New Roman" } }));
  children.push(emptyLine());
  children.push(new Paragraph({
    spacing: { line: 360 },
    children: [
      new TextRun({ text: "Keywords ", font: "Times New Roman", size: SIZE_BODY, bold: true }),
      new TextRun({ text: "Microservice Architecture; Mobile Phone E-commerce Platform; Spring Cloud Alibaba; High Concurrency; AI Shopping Assistant; Containerization", font: "Times New Roman", size: SIZE_BODY })
    ]
  }));
  return children;
}
