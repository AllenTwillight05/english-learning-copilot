#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const root = new URL(".", import.meta.url).pathname;

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
loadDotEnv(path.join(root, ".env"));

const CFG = {
  endpoint: (process.env.SJTU_AI_ENDPOINT || "").replace(/\/$/, ""),
  apiKey: process.env.SJTU_AI_API_KEY || "",
  model: process.env.SJTU_AI_MODEL || "deepseek-chat",
  temperature: 0.3,
  maxTokens: 200,
};

function readJson(fp) { return JSON.parse(fs.readFileSync(fp, "utf8")); }
function fmtDialogue(d) { return d.map(t => `${t.speaker}: ${t.text}`).join("\n"); }
function fmtList(items) { return Array.isArray(items) ? items.map(i => `- ${i}`).join("\n") : ""; }
function fmtExprHelp(items) {
  if (!Array.isArray(items)) return "";
  return items.map(i => {
    const triggers = Array.isArray(i.triggers) ? `Triggers: ${i.triggers.join(", ")}\n` : "";
    return `Intent: ${i.intent}\n${triggers}Suggested phrase: ${i.phrase}\nExplanation: ${i.explanation || ""}`;
  }).join("\n\n");
}
function fmtNamedDialogues(ds) {
  if (!Array.isArray(ds)) return "";
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
  const scenarioPath = path.join(root, "scenarios", `${scenarioId}.json`);
  const ieltsPromptMap = {
    "IELTS-P1-practice": "IELTS-P1-practice-system.md",
    "IELTS-P2-practice": "IELTS-P2-practice-system.md",
    "IELTS-P3-practice": "IELTS-P3-practice-system.md",
    "IELTS-mock-test": "IELTS-mock-test-system.md",
  };
  const isIelts = !!ieltsPromptMap[scenarioId];
  const commonPath = path.join(root, "common", isIelts ? "ielts-core.md" : "roleplay-core.md");
  const promptPath = path.join(root, "prompts", ieltsPromptMap[scenarioId] || `${scenarioId}-system.md`);
  if (!fs.existsSync(scenarioPath)) return null;
  const scenario = readJson(scenarioPath);
  const common = fs.readFileSync(commonPath, "utf8");
  const prompt = fs.readFileSync(promptPath, "utf8");
  const systemPrompt = renderTemplate(`${common}\n\n${prompt}`, scenario);
  return { scenario, systemPrompt };
}

async function callAPI(messages) {
  const resp = await fetch(`${CFG.endpoint}/chat/completions`, {
    method: "POST",
    headers: { "Authorization": `Bearer ${CFG.apiKey}`, "Content-Type": "application/json" },
    body: JSON.stringify({ model: CFG.model, messages, temperature: CFG.temperature, max_tokens: CFG.maxTokens })
  });
  const text = await resp.text();
  if (!resp.ok) throw new Error(`API ${resp.status}: ${text.substring(0,100)}`);
  const body = JSON.parse(text);
  return body?.choices?.[0]?.message?.content?.trim() || "";
}

async function runConversation(sid, label, userInputs) {
  const bundle = loadBundle(sid);
  if (!bundle) { console.log(`SKIP: ${sid}`); return; }
  const { scenario, systemPrompt } = bundle;
  const messages = [{ role: "system", content: systemPrompt }];
  const out = [`### ${label}`];
  out.push(`角色: ${scenario.agentRole}，级别: ${scenario.level}`);
  out.push(``);

  // Opening
  const opening = scenario.openingMessage;
  messages.push({ role: "assistant", content: opening });
  out.push(`**Agent:** ${opening}`);

  for (const input of userInputs) {
    out.push(``);
    out.push(`**You:** ${input}`);
    messages.push({ role: "user", content: input });

    try {
      const reply = await callAPI(messages);
      messages.push({ role: "assistant", content: reply });
      out.push(`**Agent:** ${reply}`);
    } catch (err) {
      out.push(`**Agent:** [错误: ${err.message}]`);
    }

    await new Promise(r => setTimeout(r, 12000));
  }

  out.push(``);
  out.push(`---`);
  out.push(``);
  console.log(out.join("\n"));
}

async function main() {
  console.log("");

  // === IELTS-P1-practice ===
  await runConversation("IELTS-P1-practice", "IELTS Part 1 Practice — 正常回答 -> 简短 -> 中文混英文 -> 要评分", [
    "I'd like to practice the topic of Hometown.",
    "My hometown is Suzhou. It's famous for its classical gardens and canals.",
    "The pace of life is slow.",
    "我平时喜欢打篮球和看书，周末会和朋友一起出去。",
    "Actually, I prefer reading novels, especially science fiction.",
    "Can you give me a score?",
  ]);

  // === IELTS-P2-practice ===
  await runConversation("IELTS-P2-practice", "IELTS Part 2 Practice — 选择话题 -> 说独白 -> 不会说 -> 要帮助", [
    "I want to practice Describe a person you admire.",
    "The person I admire most is my high school physics teacher. He was very patient and always encouraged us to ask questions.",
    "I don't know how to continue. Can you help me?",
    "OK let me try again. He once stayed after class to help me with a difficult problem, and that really inspired me.",
    "Can you give me some feedback?",
  ]);

  // === IELTS-P3-practice ===
  await runConversation("IELTS-P3-practice", "IELTS Part 3 Practice — 深度讨论 -> 中文 -> 观点表达", [
    "Let's talk about education.",
    "I think teachers need to be patient and inspiring. My history teacher used to tell stories instead of reading the textbook, and that made me love the subject.",
    "我觉得科技对教育影响很大，学生现在可以上网查资料。",
    "But I think there are also downsides. Students get distracted by their phones easily.",
    "Do you think AI will replace teachers one day?",
  ]);

  // === IELTS-mock-test ===
  await runConversation("IELTS-mock-test", "IELTS Full Mock Test — 完整走一遍 Part 1+2+3", [
    "I want to start with Part 1, topic Hometown.",
    "My hometown is Chengdu. It's the capital of Sichuan province, famous for its spicy food and relaxed lifestyle.",
    "I think the best thing about Chengdu is the food. There are so many different dishes to try.",
    "Yes, it has changed a lot. New metro lines and shopping malls have been built everywhere.",
    "I'm ready for Part 2.",
    "A memorable event was my first trip to the beach when I was ten. I saw the ocean for the first time and it was amazing.",
    "I think I'm done with Part 2. Can we move to Part 3?",
    "I think good teachers should be knowledgeable, patient and able to inspire students.",
    "Technology has both positive and negative effects. Students can access information easily, but they also get distracted.",
    "I think AI can assist teachers, but it cannot replace the human connection and inspiration that a real teacher provides.",
    "I'd like to finish and get my score.",
  ]);
}

main().catch(e => { console.error(e); process.exitCode = 1; });
