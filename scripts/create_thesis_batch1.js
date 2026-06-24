const fs = require("fs");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, HeadingLevel, BorderStyle, WidthType,
  ShadingType, PageBreak, PageNumber, TableOfContents, TabStopType, TabStopPosition
} = require("docx");

// ============================================================
// 辅助函数
// ============================================================
const FONT_BODY = "宋体";
const FONT_HEADING = "黑体";
const FONT_EN = "Times New Roman";
const SIZE_BODY = 24;   // 小四 12pt = 24 half-pts
const SIZE_HEADING1 = 32; // 三号 16pt = 32 half-pts
const SIZE_HEADING2 = 28; // 四号 14pt = 28 half-pts
const SIZE_HEADING3 = 24; // 小四 12pt
const SIZE_TITLE = 36;    // 小二号 18pt
const SIZE_COVER_SCHOOL = 52; // 小初 26pt
const LINE_SPACING = 360; // 1.5倍行距 (240*1.5=360)

const border = { style: BorderStyle.SINGLE, size: 1, color: "000000" };
const borders = { top: border, bottom: border, left: border, right: border };
const noBorder = { style: BorderStyle.NONE, size: 0 };
const noBorders = { top: noBorder, bottom: noBorder, left: noBorder, right: noBorder };

function bodyPara(text, options = {}) {
  return new Paragraph({
    spacing: { line: LINE_SPACING, before: 0, after: 0 },
    indent: options.noIndent ? undefined : { firstLine: 480 }, // 2字符缩进
    alignment: options.alignment || AlignmentType.JUSTIFIED,
    ...options.extra,
    children: [
      new TextRun({
        text: text || "",
        font: options.font || FONT_BODY,
        size: options.size || SIZE_BODY,
        bold: options.bold || false,
      }),
    ],
  });
}

function heading1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { line: LINE_SPACING, before: 240, after: 120 },
    children: [new TextRun({ text, font: FONT_HEADING, size: SIZE_HEADING1, bold: true })],
  });
}

function heading2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { line: LINE_SPACING, before: 180, after: 100 },
    children: [new TextRun({ text, font: FONT_HEADING, size: SIZE_HEADING2, bold: true })],
  });
}

function heading3(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    spacing: { line: LINE_SPACING, before: 120, after: 60 },
    children: [new TextRun({ text, font: FONT_HEADING, size: SIZE_HEADING3, bold: true })],
  });
}

function emptyPara() {
  return new Paragraph({ spacing: { line: LINE_SPACING }, children: [new TextRun({ text: "", font: FONT_BODY, size: SIZE_BODY })] });
}

function imagePlaceholder(caption) {
  return [
    emptyPara(),
    new Paragraph({
      spacing: { line: 300 }, alignment: AlignmentType.CENTER,
      children: [new TextRun({ text: `[ 此处插入${caption} ]`, font: FONT_BODY, size: 20, italics: true, color: "888888" })],
    }),
    new Paragraph({
      spacing: { line: 240, after: 120 }, alignment: AlignmentType.CENTER,
      children: [new TextRun({ text: caption, font: FONT_HEADING, size: 20, bold: true })],
    }),
    emptyPara(),
  ];
}

// ============================================================
// 封面
// ============================================================
function buildCover() {
  const coverChildren = [];
  // 空行推到底部
  for (let i = 0; i < 8; i++) coverChildren.push(emptyPara());

  // 学校名
  coverChildren.push(new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { line: 600 },
    children: [
      new TextRun({ text: "淮", font: FONT_HEADING, size: SIZE_COVER_SCHOOL, bold: true }),
      new TextRun({ text: "  阴  工  学  院", font: FONT_HEADING, size: SIZE_COVER_SCHOOL, bold: true }),
    ],
  }));

  coverChildren.push(emptyPara());
  coverChildren.push(new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { line: 500 },
    children: [new TextRun({ text: "毕业设计（论文）", font: FONT_HEADING, size: SIZE_TITLE, bold: true })],
  }));

  coverChildren.push(emptyPara());
  coverChildren.push(emptyPara());

  // 信息表格
  const infoData = [
    ["学生姓名：", "汪俣珩", "学    号：", "112204150143"],
    ["学    院：", "计算机与软件工程学院", "", ""],
    ["专    业：", "软件工程（软件1221）", "", ""],
    ["设计（论文）题目：", "基于Spring Cloud微服务架构的", "", ""],
    ["", "手机电商平台设计与实现", "", ""],
    ["校内指导老师：", "王媛媛  副教授", "", ""],
    ["校外指导老师：", "汪涛", "", ""],
  ];

  const coverTable = new Table({
    width: { size: 8000, type: WidthType.DXA },
    columnWidths: [2200, 3500, 1000, 1300],
    rows: infoData.map(row => new TableRow({
      children: row.map((cell, idx) => new TableCell({
        borders: noBorders,
        width: { size: [2200, 3500, 1000, 1300][idx], type: WidthType.DXA },
        margins: { top: 40, bottom: 40, left: 60, right: 60 },
        verticalAlign: "center",
        children: [new Paragraph({
          alignment: (idx === 0 || idx === 2) ? AlignmentType.RIGHT : AlignmentType.LEFT,
          children: [new TextRun({ text: cell, font: FONT_BODY, size: SIZE_BODY })],
        })],
      })),
    })),
  });

  coverChildren.push(new Paragraph({ alignment: AlignmentType.CENTER, children: [coverTable] }));

  // 日期
  coverChildren.push(emptyPara());
  coverChildren.push(emptyPara());
  coverChildren.push(new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { line: 500 },
    children: [new TextRun({ text: "2026年5月", font: FONT_BODY, size: SIZE_BODY })],
  }));

  // 分页
  coverChildren.push(new Paragraph({ children: [new PageBreak()] }));
  return coverChildren;
}

// ============================================================
// 中文摘要
// ============================================================
function buildChineseAbstract() {
  const abstractTitle = "毕业设计（论文）中文摘要";
  const abstractBody = `随着数字化经济的蓬勃发展与移动支付的全面普及，电子商务在现代商业体系中的战略地位日益凸显。手机电商平台作为垂直电商的典型代表，面临的业务场景具有鲜明特殊性：手机作为高客单价、强关注度的商品，在新品首发、秒杀大促等特定活动期间，往往在极短时间内引发海量并发访问请求，同时业务链路涵盖复杂的商品规格检索、库存精准锁定、分布式订单生成与支付回调等高度耦合的环节，这对系统的并发处理能力、数据一致性保障以及整体架构的容灾可用性提出了极其严苛的要求。

本课题立足于上述现实挑战，旨在构建一个能够抵御瞬时高并发冲击、保障核心交易链路稳定运行的高可用分布式手机电商平台。系统基于Spring Cloud Alibaba微服务生态体系进行整体架构设计，采用前后端分离的开发模式。后端采用Spring Boot框架与Spring Cloud Gateway网关作为统一入口，以Nacos为服务注册与配置中心；前端基于Vue.js 3框架结合Element Plus组件库构建响应式用户界面。针对手机电商高并发场景下的核心痛点，系统深度集成了Redis分布式缓存与RabbitMQ消息队列，通过Lua脚本实现原子性的库存预扣减，利用消息队列异步削峰将峰值写入流量转化为平缓的后台消费流，从而有效保障了MySQL数据持久层的稳定性。在流量防护层面，引入Sentinel流量哨兵在网关层实施细粒度的QPS限流与熔断降级策略，确保系统在资源红线下的存活能力。此外，系统创新性地接入OpenRouter API调用大语言模型（LLM），构建了对话式的场景化AI智能导购模块，能够理解用户的自然语言非结构化需求，结合数据库中的结构化商品参数模板通过Prompt工程输出个性化推荐，真正实现了业务形态由"人找货"向"智能交互匹配"的跨越。系统全部服务采用Docker Compose进行容器化编排部署，通过严格的物理资源上限约束实现了轻量化、高可用的运行环境。

系统测试结果表明，该平台功能完整、运行稳定，在高并发下单场景下表现出良好的吞吐量与容错能力，AI导购模块能够根据用户描述的预算及功能偏好给出具有参考价值的机型推荐，达到了预期的设计目标。`;

  return [
    new Paragraph({
      alignment: AlignmentType.CENTER, spacing: { line: 400, before: 200, after: 200 },
      children: [new TextRun({ text: abstractTitle, font: FONT_HEADING, size: SIZE_HEADING2, bold: true })],
    }),
    emptyPara(),
    bodyPara(abstractBody),
    emptyPara(),
    new Paragraph({
      spacing: { line: LINE_SPACING, before: 200 },
      indent: { firstLine: 480 },
      children: [
        new TextRun({ text: "关键词：", font: FONT_HEADING, size: SIZE_BODY, bold: true }),
        new TextRun({ text: "Spring Cloud，微服务，电商平台，高并发，智能导购，Redis", font: FONT_BODY, size: SIZE_BODY }),
      ],
    }),
    new Paragraph({ children: [new PageBreak()] }),
  ];
}

// ============================================================
// 英文摘要
// ============================================================
function buildEnglishAbstract() {
  const englishTitle = "毕业设计（论文）外文摘要";
  const engTitle = "Design and Implementation of Mobile E-commerce Platform Based on Spring Cloud Microservice Architecture";
  const engBody = `With the vigorous development of the digital economy and the widespread adoption of mobile payment, e-commerce has become increasingly prominent in modern business systems. As a typical representative of vertical e-commerce, mobile phone e-commerce platforms face distinctive challenges: mobile phones, as high-value and high-attention commodities, often trigger massive concurrent access requests within extremely short periods during new product launches and flash sales. The business chain involves complex processes such as product specification retrieval, precise inventory locking, distributed order generation, and third-party payment callbacks, which impose stringent requirements on system concurrency processing capability, data consistency assurance, and overall architecture fault tolerance.

This project addresses these practical challenges by constructing a highly available distributed mobile e-commerce platform capable of withstanding instantaneous high-concurrency shocks and ensuring stable operation of the core transaction chain. The system adopts a microservice architecture based on the Spring Cloud Alibaba ecosystem, employing a front-end and back-end separation development model. The back-end utilizes the Spring Boot framework with Spring Cloud Gateway as the unified entry point and Nacos as the service registration and configuration center. The front-end is built on the Vue.js 3 framework combined with the Element Plus component library to create a responsive user interface. To address the core pain points of high-concurrency mobile e-commerce scenarios, the system deeply integrates Redis distributed caching and RabbitMQ message queues, implementing atomic inventory pre-deduction through Lua scripts and utilizing message queue asynchronous peak-shaving to convert peak write traffic into smooth background consumption flows, thereby effectively ensuring the stability of the MySQL data persistence layer. In terms of traffic protection, Sentinel is introduced to implement fine-grained QPS rate limiting and circuit-breaking degradation strategies at the gateway layer, ensuring system survivability under resource constraints. Furthermore, the system innovatively integrates the OpenRouter API to invoke large language models (LLMs), constructing a conversational scenario-based AI shopping assistant module capable of understanding users' unstructured natural language requirements, combining structured product parameter templates from the database through prompt engineering to deliver personalized recommendations. All services are deployed through Docker Compose container orchestration with strict physical resource upper-limit constraints to achieve a lightweight, highly available runtime environment.

System testing results demonstrate that the platform is functionally complete and operates stably, exhibiting good throughput and fault tolerance in high-concurrency order scenarios. The AI shopping assistant module can provide valuable model recommendations based on user-described budgets and functional preferences, achieving the expected design objectives.`;

  return [
    new Paragraph({
      alignment: AlignmentType.CENTER, spacing: { line: 400, before: 200, after: 100 },
      children: [new TextRun({ text: englishTitle, font: FONT_HEADING, size: SIZE_HEADING2, bold: true })],
    }),
    emptyPara(),
    new Paragraph({
      alignment: AlignmentType.CENTER, spacing: { line: 360, after: 120 },
      children: [new TextRun({ text: engTitle, font: FONT_EN, size: SIZE_BODY, bold: true })],
    }),
    emptyPara(),
    bodyPara(engBody, { noIndent: false, font: FONT_EN }),
    emptyPara(),
    new Paragraph({
      spacing: { line: LINE_SPACING, before: 200 },
      indent: { firstLine: 480 },
      children: [
        new TextRun({ text: "Keywords: ", font: FONT_EN, size: SIZE_BODY, bold: true }),
        new TextRun({ text: "Spring Cloud, Microservices, E-commerce Platform, High Concurrency, AI Shopping Assistant, Redis", font: FONT_EN, size: SIZE_BODY }),
      ],
    }),
    new Paragraph({ children: [new PageBreak()] }),
  ];
}

// ============================================================
// 目录
// ============================================================
function buildTableOfContents() {
  return [
    new Paragraph({
      alignment: AlignmentType.CENTER, spacing: { line: 400, before: 200, after: 200 },
      children: [new TextRun({ text: "目  录", font: FONT_HEADING, size: SIZE_HEADING1, bold: true })],
    }),
    emptyPara(),
    new TableOfContents("目录", { hyperlink: true, headingStyleRange: "1-3" }),
    new Paragraph({ children: [new PageBreak()] }),
  ];
}

// ============================================================
// 构建文档
// ============================================================
async function build() {
  const doc = new Document({
    styles: {
      default: {
        document: { run: { font: FONT_BODY, size: SIZE_BODY } },
      },
      paragraphStyles: [
        {
          id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: SIZE_HEADING1, bold: true, font: FONT_HEADING },
          paragraph: { spacing: { before: 240, after: 120, line: LINE_SPACING }, outlineLevel: 0 },
        },
        {
          id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: SIZE_HEADING2, bold: true, font: FONT_HEADING },
          paragraph: { spacing: { before: 180, after: 100, line: LINE_SPACING }, outlineLevel: 1 },
        },
        {
          id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: SIZE_HEADING3, bold: true, font: FONT_HEADING },
          paragraph: { spacing: { before: 120, after: 60, line: LINE_SPACING }, outlineLevel: 2 },
        },
      ],
    },
    sections: [
      // 封面区域——无页眉
      {
        properties: {
          page: {
            size: { width: 11906, height: 16838 }, // A4
            margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
          },
        },
        children: buildCover(),
      },
      // 摘要 + 目录 —— 单独页眉格式
      {
        properties: {
          page: {
            size: { width: 11906, height: 16838 }, // A4
            margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
          },
        },
        headers: {
          default: new Header({
            children: [new Paragraph({
              alignment: AlignmentType.CENTER,
              spacing: { after: 100 },
              border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: "000000", space: 4 } },
              children: [new TextRun({ text: "淮阴工学院毕业设计（论文）", font: FONT_BODY, size: 18 })],
            })],
          }),
        },
        footers: {
          default: new Footer({
            children: [new Paragraph({
              alignment: AlignmentType.CENTER,
              children: [new TextRun({ text: "第 ", font: FONT_BODY, size: 18 }), new TextRun({ children: [PageNumber.CURRENT], font: FONT_BODY, size: 18 }), new TextRun({ text: " 页", font: FONT_BODY, size: 18 })],
            })],
          }),
        },
        children: [...buildChineseAbstract(), ...buildEnglishAbstract(), ...buildTableOfContents()],
      },
    ],
  });

  const buffer = await Packer.toBuffer(doc);
  fs.writeFileSync("/home/jill/project/biyesheji/毕业论文.docx", buffer);
  console.log("Batch 1 done: 毕业论文.docx created (cover + abstracts + TOC)");
}

build().catch(err => { console.error(err); process.exit(1); });
