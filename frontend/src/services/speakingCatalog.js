export const DAILY_SPEAKING_SCENARIOS = [
  {
    id: "G-01-airport",
    title: "机场值机与改签",
    level: "A2",
    accent: "出行必备",
    duration: "12 min",
    summary: "训练值机、托运行李、选座和改签沟通。",
    tone: "gold",
    goal: "完成值机、座位选择和请求改签的基础沟通。",
    keywords: ["check in", "boarding pass", "aisle seat", "reschedule"]
  },
  {
    id: "G-02-restaurant",
    title: "餐厅点餐",
    level: "A2",
    accent: "生活口语",
    duration: "12 min",
    summary: "练习订位、点餐、忌口说明和结账。",
    tone: "orange",
    goal: "在餐厅中自然完成从入座到结账的完整交流。",
    keywords: ["reservation", "recommend", "allergy", "bill"]
  },
  {
    id: "G-03-hotel",
    title: "酒店入住",
    level: "A2",
    accent: "旅行服务",
    duration: "13 min",
    summary: "练习入住、房型确认、设施咨询和投诉处理。",
    tone: "blue",
    goal: "清楚表达入住需求，并能处理常见酒店服务问题。",
    keywords: ["check in", "reservation", "amenities", "reception"]
  },
  {
    id: "G-04-shopping",
    title: "购物与退换货",
    level: "A2",
    accent: "购物技能",
    duration: "13 min",
    summary: "训练询价、试穿、折扣、退货和换货表达。",
    tone: "violet",
    goal: "在商店中礼貌询问商品信息并处理售后沟通。",
    keywords: ["size", "discount", "receipt", "exchange"]
  },
  {
    id: "G-05-clinic",
    title: "医院就诊",
    level: "B1",
    accent: "医疗常用语",
    duration: "16 min",
    summary: "练习挂号、描述症状、理解医嘱和取药。",
    tone: "rose",
    goal: "清晰描述症状，配合完成基础问诊流程。",
    keywords: ["symptom", "prescription", "pharmacy", "appointment"]
  },
  {
    id: "G-06-job-interview",
    title: "求职面试",
    level: "B1",
    accent: "职场表达",
    duration: "18 min",
    summary: "练习自我介绍、经历说明、优势表达和反问。",
    tone: "mint",
    goal: "用简洁自然的英语回答常见面试问题。",
    keywords: ["experience", "strength", "challenge", "role"]
  },
  {
    id: "G-07-business-meeting",
    title: "商务会议",
    level: "B2",
    accent: "商务沟通",
    duration: "18 min",
    summary: "训练开场、确认议程、表达意见和推进决策。",
    tone: "blue",
    goal: "在会议中有条理地表达观点并回应他人意见。",
    keywords: ["agenda", "clarify", "timeline", "follow up"]
  },
  {
    id: "G-08-small-talk",
    title: "日常寒暄",
    level: "B1",
    accent: "自然对话",
    duration: "15 min",
    summary: "训练破冰、接话、追问和自然结束话题。",
    tone: "mint",
    goal: "让轻松社交对话更自然、更连贯。",
    keywords: ["small talk", "by the way", "sounds great", "weekend"]
  },
  {
    id: "G-09-presentation",
    title: "演讲展示",
    level: "B2",
    accent: "公开表达",
    duration: "18 min",
    summary: "练习开场、结构提示、数据说明和问答回应。",
    tone: "violet",
    goal: "用清晰结构完成短展示并自然回答追问。",
    keywords: ["overview", "highlight", "data", "question"]
  },
  {
    id: "G-10-phone-call",
    title: "电话沟通",
    level: "B1",
    accent: "远程沟通",
    duration: "14 min",
    summary: "练习说明来意、确认信息、留言和改约。",
    tone: "gold",
    goal: "在电话中清楚表达目的并确认关键信息。",
    keywords: ["calling about", "confirm", "message", "available"]
  },
  {
    id: "G-11-directions",
    title: "问路与指路",
    level: "A2",
    accent: "旅行必备",
    duration: "11 min",
    summary: "练习问路、确认路线、交通方式和到达时间。",
    tone: "orange",
    goal: "听懂并表达基础路线信息。",
    keywords: ["turn left", "cross", "station", "minutes"]
  },
  {
    id: "G-12-renting-apartment",
    title: "租房沟通",
    level: "B1",
    accent: "生活事务",
    duration: "17 min",
    summary: "练习看房、租金、押金、合同和维修沟通。",
    tone: "rose",
    goal: "围绕租房条件和合同细节进行有效沟通。",
    keywords: ["rent", "deposit", "lease", "maintenance"]
  }
];

export const IELTS_SCENARIOS = {
  part1: {
    scenarioId: "IELTS-P1-practice",
    title: "Part 1",
    subtitle: "短问短答",
    level: "IELTS",
    duration: "4-5 min",
    tone: "blue",
    topics: [
      "Hometown",
      "Work or Study",
      "Accommodation",
      "Daily Routine",
      "Free Time",
      "Technology",
      "Weather",
      "Friends"
    ]
  },
  part2: {
    scenarioId: "IELTS-P2-practice",
    title: "Part 2",
    subtitle: "Cue Card",
    level: "IELTS",
    duration: "3-4 min",
    tone: "gold",
    topics: [
      "Describe a person who inspired you",
      "Describe a memorable journey",
      "Describe a useful skill you learned",
      "Describe a place you would like to visit",
      "Describe a time you helped someone",
      "Describe an important decision"
    ]
  },
  part3: {
    scenarioId: "IELTS-P3-practice",
    title: "Part 3",
    subtitle: "深入讨论",
    level: "IELTS",
    duration: "4-5 min",
    tone: "mint",
    topics: [
      "Education and society",
      "Technology and communication",
      "Work and career choices",
      "Cities and public services",
      "Culture and traditions",
      "Environment and lifestyle"
    ]
  },
  mock: {
    scenarioId: "IELTS-mock-test",
    title: "Mock Test",
    subtitle: "完整模拟",
    level: "IELTS",
    duration: "11-14 min",
    tone: "violet"
  }
};

export function createIeltsScenario(partKey) {
  const part = IELTS_SCENARIOS[partKey];

  return {
    id: part.scenarioId,
    title: `雅思口语 ${part.title}`,
    level: part.level,
    accent: part.subtitle,
    duration: part.duration,
    summary: partKey === "mock"
      ? "按真实雅思口语考试节奏完成 Part 1、Part 2、Part 3。"
      : "围绕所选话题进行雅思口语专项训练。",
    tone: part.tone,
    goal: partKey === "mock"
      ? "模拟完整考试流程，训练连贯度、内容展开和临场回答。"
      : "围绕指定话题给出自然、完整、符合雅思口语节奏的回答。",
    keywords: partKey === "mock" ? ["fluency", "coherence", "vocabulary"] : ["IELTS", part.title, part.subtitle],
    prompts: [
      {
        role: "coach",
        text: partKey === "mock"
          ? "This is your IELTS speaking mock test. Let's begin with Part 1."
          : `Let's practise IELTS Speaking ${part.title}.`
      }
    ],
    targetTurns: partKey === "mock" ? 12 : 6
  };
}
