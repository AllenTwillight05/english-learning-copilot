#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const moduleRoot = new URL(".", import.meta.url).pathname;
const outputsDir = path.join(moduleRoot, "test-outputs");

function loadDotEnv(fp) {
  if (!fs.existsSync(fp)) return;
  for (const line of fs.readFileSync(fp, "utf8").split(/\r?\n/)) {
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
  temperature: 0.3,
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
  return { scenario, systemPrompt };
}

async function callAPI(messages) {
  const resp = await fetch(`${CONFIG.endpoint}/chat/completions`, {
    method: "POST",
    headers: { "Authorization": `Bearer ${CONFIG.apiKey}`, "Content-Type": "application/json" },
    body: JSON.stringify({ model: CONFIG.model, messages, temperature: CONFIG.temperature, max_tokens: CONFIG.maxTokens })
  });
  const text = await resp.text();
  if (!resp.ok) throw new Error(`API ${resp.status}: ${text.substring(0,100)}`);
  const body = JSON.parse(text);
  return body?.choices?.[0]?.message?.content?.trim() || "";
}

// Scenarios needing retest with their test cases
const RETESTS = {
  "G-03-hotel": [
    "Hi, I have a reservation under the name John Sandals.",
    "VISA.",
    "我想办理入住。",
    "我想点菜。",
    "Reservation. John.",
    "Do you accept American Express?",
  ],
  "G-04-shopping": [
    "Excuse me, I'm looking for a sweater.",
    "Medium.",
    "这件多少钱？",
    "Can I try this on?",
  ],
  "G-07-business-meeting": [
    "I suggest we increase our marketing budget.",
  ],
  "G-08-small-talk": [
    "Hi, how are you doing today?",
    "Good, thanks.",
    "今天天气真好啊。",
    "I've been really busy with work lately.",
  ],
  "G-09-presentation": [
    "Good morning everyone, today I'm going to present our new product.",
    "I'm a bit nervous.",
    "我首先介绍一下公司背景。",
    "Sorry, the slides aren't working.",
  ],
  "G-10-phone-call": [
    "Hello, may I speak to Mr. Smith please?",
    "Yes, this is he.",
    "我想预约。",
    "Can I leave a message?",
  ],
  "G-11-directions": [
    "Is it far from here?",
  ],
  "G-12-renting-apartment": [
    "How many bedrooms does it have?",
  ],
};

const labels = [
  "normal", "short answer", "chinese intent", "off-topic",
  "fragment", "american express", "", "", ""
];

async function main() {
  const issues = [];

  for (const [sid, inputs] of Object.entries(RETESTS)) {
    const bundle = loadBundle(sid);
    if (!bundle) { console.log(`SKIP: ${sid}`); continue; }
    const { scenario, systemPrompt } = bundle;
    const log = [`=== ${sid}: ${scenario.title} (retest) ===`];

    for (let i = 0; i < inputs.length; i++) {
      const input = inputs[i];
      const label = labels[i] || `test${i+1}`;
      const messages = [
        { role: "system", content: systemPrompt },
        { role: "assistant", content: scenario.openingMessage },
        { role: "user", content: input }
      ];

      try {
        const reply = await callAPI(messages);
        log.push(`[${label}]`);
        log.push(`  IN:  ${input}`);
        log.push(`  OUT: ${reply.substring(0, 200)}`);

        if (reply.length > 300) issues.push(`${sid}/${label}: 回复过长 (${reply.length})`);
        if (/[。，、？]/.test(reply) && !/可以这样说/.test(reply)) issues.push(`${sid}/${label}: 非帮助模式出现中文标点`);
        if (input.match(/[\u4e00-\u9fff]/) && !reply.includes("可以这样说") && !/restaurant|hotel|scenario/.test(reply.toLowerCase()) && reply.includes("Chinese")) {
          // only flag if it has Chinese content from a Chinese input but no help mode
        }
        if (/\bAI\b|\bmodel\b|\bprompt\b/i.test(reply)) issues.push(`${sid}/${label}: 提及AI/模型`);
        log.push("");
      } catch (err) {
        log.push(`[${label}] ERROR: ${err.message}\n`);
        issues.push(`${sid}/${label}: API失败 — ${err.message}`);
      }

      // 7s delay to stay under 10 req/min
      await new Promise(r => setTimeout(r, 7000));
    }

    // Append to existing log or create new
    const logPath = path.join(outputsDir, `${sid}.log`);
    const existing = fs.existsSync(logPath) ? fs.readFileSync(logPath, "utf8") + "\n" : "";
    fs.writeFileSync(logPath, existing + log.join("\n"), "utf8");
    console.log(`DONE: ${sid}`);
  }

  if (issues.length > 0) {
    console.log("\n=== Issues ===");
    issues.forEach(i => console.log(`  ${i}`));
  } else {
    console.log("\n✅ 所有补测通过，无问题");
  }
}

main().catch(e => { console.error(e); process.exitCode = 1; });
