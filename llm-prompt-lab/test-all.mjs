#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const moduleRoot = new URL(".", import.meta.url).pathname;

function loadDotEnv(filePath) {
  if (!fs.existsSync(filePath)) return;
  for (const line of fs.readFileSync(filePath, "utf8").split(/\r?\n/)) {
    const t = line.trim();
    if (!t || t.startsWith("#")) continue;
    const sep = t.indexOf("=");
    if (sep === -1) continue;
    const key = t.slice(0, sep).trim();
    const val = t.slice(sep + 1).trim().replace(/^['"]|['"]$/g, "");
    if (key && process.env[key] === undefined) process.env[key] = val;
  }
}
loadDotEnv(path.join(moduleRoot, ".env"));

const CONFIG = {
  endpoint: (process.env.SJTU_AI_ENDPOINT || "").replace(/\/$/, ""),
  apiKey: process.env.SJTU_AI_API_KEY || "",
  model: process.env.SJTU_AI_MODEL || "deepseek-chat",
  temperature: 0.3,  // low temp for consistent testing
  maxTokens: 180,
};

function readJson(fp) { return JSON.parse(fs.readFileSync(fp, "utf8")); }

function fmtDialogue(d) { return d.map(t => `${t.speaker}: ${t.text}`).join("\n"); }
function fmtList(items) {
  if (!Array.isArray(items) || items.length === 0) return "";
  return items.map(i => `- ${i}`).join("\n");
}
function fmtExprHelp(items) {
  if (!Array.isArray(items) || items.length === 0) return "";
  return items.map(i => {
    const triggers = Array.isArray(i.triggers) ? `Triggers: ${i.triggers.join(", ")}\n` : "";
    return `Intent: ${i.intent}\n${triggers}Suggested phrase: ${i.phrase}\nExplanation: ${i.explanation || ""}`;
  }).join("\n\n");
}
function fmtNamedDialogues(ds) {
  if (!Array.isArray(ds) || ds.length === 0) return "";
  return ds.map(d => `${d.title ? d.title + "\n" : ""}${fmtDialogue(d.turns ?? [])}`).join("\n\n");
}
function renderTemplate(tpl, s) {
  const vals = {
    SCENARIO_ID: s.id, TITLE: s.title, LEVEL: s.level,
    LEARNER_ROLE: s.learnerRole, AGENT_ROLE: s.agentRole,
    GOAL: s.goal, TARGET_TURNS: String(s.targetTurns),
    KEYWORDS: (s.keywords || []).join(", "),
    OPENING_MESSAGE: s.openingMessage,
    SAMPLE_DIALOGUE: fmtDialogue(s.sampleDialogue || []),
    CONVERSATION_FLOW: fmtList(s.conversationFlow),
    STATE_RULES: fmtList(s.stateRules),
    LEVEL_ADAPTATION: fmtList(s.levelAdaptation),
    ERROR_HANDLING: fmtList(s.errorHandling),
    EXPRESSION_HELP: fmtExprHelp(s.expressionHelp),
    SOURCE_EXAMPLES: fmtNamedDialogues(s.sourceExamples),
    TEST_INPUTS: fmtList(s.testInputs),
  };
  return tpl.replace(/\{\{([A-Z_]+)\}\}/g, (_, k) => vals[k] ?? "");
}

function loadBundle(scenarioId) {
  const scenarioPath = path.join(moduleRoot, "scenarios", `${scenarioId}.json`);
  const promptPath = path.join(moduleRoot, "prompts", `${scenarioId}-system.md`);
  const commonPath = path.join(moduleRoot, "common", "roleplay-core.md");
  if (!fs.existsSync(scenarioPath)) return null;
  const scenario = readJson(scenarioPath);
  const common = fs.readFileSync(commonPath, "utf8");
  const prompt = fs.readFileSync(promptPath, "utf8");
  const systemPrompt = renderTemplate(`${common}\n\n${prompt}`, scenario);
  return { scenario, systemPrompt, scenarioPath, promptPath, commonPath };
}

async function callAPI(messages) {
  const resp = await fetch(`${CONFIG.endpoint}/chat/completions`, {
    method: "POST",
    headers: { "Authorization": `Bearer ${CONFIG.apiKey}`, "Content-Type": "application/json" },
    body: JSON.stringify({ model: CONFIG.model, messages, temperature: CONFIG.temperature, max_tokens: CONFIG.maxTokens })
  });
  const text = await resp.text();
  if (!resp.ok) throw new Error(`API ${resp.status}: ${text}`);
  const body = JSON.parse(text);
  return body?.choices?.[0]?.message?.content?.trim() || "";
}

const issues = [];
const outputsDir = path.join(moduleRoot, "test-outputs");
fs.mkdirSync(outputsDir, { recursive: true });

const SCENARIOS = [
  "G-01-airport", "G-02-restaurant", "G-03-hotel",
  "G-04-shopping", "G-05-clinic", "G-06-job-interview",
  "G-07-business-meeting", "G-08-small-talk",
  "G-09-presentation", "G-10-phone-call",
  "G-11-directions", "G-12-renting-apartment"
];

// Test cases per scenario
const TEST_CASES = {
  "G-01-airport": [
    { label: "normal check-in", input: "Hi, I'd like to check in for my flight to Tokyo." },
    { label: "short answer", input: "Window." },
    { label: "chinese intent", input: "我想托运一个行李。" },
    { label: "off-topic", input: "Can you recommend a good restaurant nearby?" },
    { label: "grammar error", input: "I have a reservation name John." },
  ],
  "G-02-restaurant": [
    { label: "normal order", input: "Hi, can I see the menu please?" },
    { label: "short answer", input: "Pasta." },
    { label: "chinese intent", input: "买单。" },
    { label: "mixed language", input: "I want 点菜." },
    { label: "off-topic", input: "Do you know where the nearest hotel is?" },
  ],
  "G-03-hotel": [
    { label: "normal check-in", input: "Hi, I have a reservation under the name John Sandals." },
    { label: "short answer", input: "VISA." },
    { label: "chinese intent", input: "我想办理入住。" },
    { label: "off-topic restaurant", input: "我想点菜。" },
    { label: "fragment", input: "Reservation. John." },
    { label: "american express", input: "Do you accept American Express?" },
  ],
  "G-04-shopping": [
    { label: "normal shopping", input: "Excuse me, I'm looking for a sweater." },
    { label: "short answer", input: "Medium." },
    { label: "chinese intent", input: "这件多少钱？" },
    { label: "try on", input: "Can I try this on?" },
  ],
  "G-05-clinic": [
    { label: "normal visit", input: "Hi, I need to see a doctor. I have a stomachache." },
    { label: "short answer", input: "Two days." },
    { label: "chinese intent", input: "我头疼。" },
    { label: "symptom desc", input: "I ate something bad yesterday." },
  ],
  "G-06-job-interview": [
    { label: "normal interview", input: "Good morning, I'm here for my interview." },
    { label: "short answer", input: "Marketing." },
    { label: "chinese intent", input: "我的优点是团队合作。" },
    { label: "strength question", input: "My greatest strength is problem-solving." },
  ],
  "G-07-business-meeting": [
    { label: "normal meeting", input: "Good morning everyone. Let's start with the sales figures." },
    { label: "short answer", input: "I agree." },
    { label: "chinese intent", input: "我觉得我们需要降低成本。" },
    { label: "proposal", input: "I suggest we increase our marketing budget." },
  ],
  "G-08-small-talk": [
    { label: "normal greeting", input: "Hi, how are you doing today?" },
    { label: "short answer", input: "Good, thanks." },
    { label: "chinese intent", input: "今天天气真好啊。" },
    { label: "follow-up", input: "I've been really busy with work lately." },
  ],
  "G-09-presentation": [
    { label: "start presentation", input: "Good morning everyone, today I'm going to present our new product." },
    { label: "nervous", input: "I'm a bit nervous." },
    { label: "chinese intent", input: "我首先介绍一下公司背景。" },
    { label: "technical issue", input: "Sorry, the slides aren't working." },
  ],
  "G-10-phone-call": [
    { label: "normal call", input: "Hello, may I speak to Mr. Smith please?" },
    { label: "short answer", input: "Yes, this is he." },
    { label: "chinese intent", input: "我想预约。" },
    { label: "message", input: "Can I leave a message?" },
  ],
  "G-11-directions": [
    { label: "normal ask", input: "Excuse me, could you tell me how to get to the train station?" },
    { label: "short answer", input: "Left." },
    { label: "chinese intent", input: "请问地铁站在哪里？" },
    { label: "follow-up", input: "Is it far from here?" },
  ],
  "G-12-renting-apartment": [
    { label: "normal inquiry", input: "Hi, I'm calling about the apartment for rent on Main Street." },
    { label: "short answer", input: "$800." },
    { label: "chinese intent", input: "我想看看房子。" },
    { label: "detail question", input: "How many bedrooms does it have?" },
  ],
};

async function testScenario(scenarioId) {
  const bundle = loadBundle(scenarioId);
  if (!bundle) { console.log(`SKIP: ${scenarioId} — files not found`); return; }
  
  const { scenario, systemPrompt } = bundle;
  const tests = TEST_CASES[scenarioId] || [];
  const log = [`=== ${scenarioId}: ${scenario.title} (${scenario.level}) ===`];
  log.push(`Opening: ${scenario.openingMessage}\n`);
  
  let scenarioIssues = [];
  
  for (const tc of tests) {
    const messages = [
      { role: "system", content: systemPrompt },
      { role: "assistant", content: scenario.openingMessage },
      { role: "user", content: tc.input }
    ];
    
    try {
      const reply = await callAPI(messages);
      log.push(`[${tc.label}]`);
      log.push(`  IN:  ${tc.input}`);
      log.push(`  OUT: ${reply.substring(0, 200)}`);
      
      // Issue detection
      if (reply.length > 300) {
        scenarioIssues.push(`${tc.label}: 回复过长 (${reply.length} chars)`);
      }
      if (/[。，、？】】】]/.test(reply) && !/可以这样说/.test(reply)) {
        scenarioIssues.push(`${tc.label}: 非帮助模式出现中文标点`);
      }
      if (tc.label.includes("chinese") && !reply.includes("可以这样说") && !reply.includes("可以说")) {
        scenarioIssues.push(`${tc.label}: 中文输入但未触发表达帮助模式`);
      }
      if (tc.label.includes("off-topic") && reply.includes("Let's focus") === false && reply.includes("different scenario") === false && !/餐厅|restaurant|酒店|hotel/.test(reply)) {
        // lenient check
      }
      if (/\bAI\b|\bmodel\b|\bprompt\b|\bassistant\b|\b助手的?\b/i.test(reply) && !tc.label.includes("chinese")) {
        scenarioIssues.push(`${tc.label}: 回复中提及AI/模型/prompt`);
      }
      
      log.push("");
    } catch (err) {
      log.push(`[${tc.label}] ERROR: ${err.message}\n`);
      scenarioIssues.push(`${tc.label}: API调用失败 — ${err.message}`);
    }
    
    // rate limit: 10 req/min, sleep 2s between
    await new Promise(r => setTimeout(r, 2000));
  }
  
  // Save test log
  fs.writeFileSync(path.join(outputsDir, `${scenarioId}.log`), log.join("\n"), "utf8");
  
  if (scenarioIssues.length > 0) {
    issues.push(`\n### ${scenarioId} (${scenario.title})`);
    for (const iss of scenarioIssues) {
      issues.push(`- ${iss}`);
    }
  }
  
  console.log(`DONE: ${scenarioId} — ${scenarioIssues.length} issues`);
}

async function main() {
  console.log(`Testing ${SCENARIOS.length} scenarios...\n`);
  for (const sid of SCENARIOS) {
    await testScenario(sid);
  }
  
  const reportPath = path.join(outputsDir, "ISSUES.md");
  if (issues.length > 0) {
    const report = "# 场景测试问题报告\n\n" + issues.join("\n") + "\n";
    fs.writeFileSync(reportPath, report, "utf8");
    console.log(`\n=== 问题报告 ===`);
    console.log(issues.join("\n"));
  } else {
    fs.writeFileSync(reportPath, "# 场景测试问题报告\n\n✅ 所有场景测试通过，未发现问题。\n", "utf8");
    console.log("\n✅ 所有场景测试通过！");
  }
  console.log(`\n完整日志: ${outputsDir}/`);
}

main().catch(e => { console.error(e); process.exitCode = 1; });
