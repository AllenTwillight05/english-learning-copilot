#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const root = new URL(".", import.meta.url).pathname;
const outDir = path.join(root, "test-outputs");

function loadDotEnv(filePath) {
  if (!fs.existsSync(filePath)) return;
  for (const line of fs.readFileSync(filePath, "utf8").split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const separator = trimmed.indexOf("=");
    if (separator === -1) continue;
    const key = trimmed.slice(0, separator).trim();
    const value = trimmed.slice(separator + 1).trim().replace(/^['"]|['"]$/g, "");
    if (key && process.env[key] === undefined) process.env[key] = value;
  }
}

loadDotEnv(path.join(root, ".env"));

const config = {
  endpoint: (process.env.SJTU_AI_ENDPOINT || "").replace(/\/$/, ""),
  apiKey: process.env.SJTU_AI_API_KEY || "",
  model: process.env.SJTU_AI_MODEL || "deepseek-chat",
  temperature: 0.3,
  maxTokens: 220
};

const IELTS_PROMPTS = {
  "IELTS-P1-practice": "IELTS-P1-practice-system.md",
  "IELTS-P2-practice": "IELTS-P2-practice-system.md",
  "IELTS-P3-practice": "IELTS-P3-practice-system.md",
  "IELTS-mock-test": "IELTS-mock-test-system.md"
};

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function formatDialogue(dialogue) {
  return dialogue.map((turn) => `${turn.speaker}: ${turn.text}`).join("\n");
}

function formatList(items) {
  if (!Array.isArray(items) || items.length === 0) return "";
  return items.map((item) => `- ${item}`).join("\n");
}

function formatExpressionHelp(items) {
  if (!Array.isArray(items) || items.length === 0) return "";
  return items.map((item) => {
    const triggers = Array.isArray(item.triggers) && item.triggers.length > 0
      ? `Triggers: ${item.triggers.join(", ")}\n`
      : "";
    return `Intent: ${item.intent}\n${triggers}Suggested phrase: ${item.phrase}\nExplanation: ${item.explanation || ""}`;
  }).join("\n\n");
}

function formatNamedDialogues(dialogues) {
  if (!Array.isArray(dialogues) || dialogues.length === 0) return "";
  return dialogues
    .map((dialogue) => `${dialogue.title ? `${dialogue.title}\n` : ""}${formatDialogue(dialogue.turns ?? [])}`)
    .join("\n\n");
}

function renderTemplate(template, scenario) {
  const values = {
    SCENARIO_ID: scenario.id,
    TITLE: scenario.title,
    LEVEL: scenario.level,
    LEARNER_ROLE: scenario.learnerRole,
    AGENT_ROLE: scenario.agentRole,
    GOAL: scenario.goal,
    TARGET_TURNS: String(scenario.targetTurns),
    KEYWORDS: (scenario.keywords || []).join(", "),
    OPENING_MESSAGE: scenario.openingMessage,
    SAMPLE_DIALOGUE: formatDialogue(scenario.sampleDialogue || []),
    CONVERSATION_FLOW: formatList(scenario.conversationFlow),
    STATE_RULES: formatList(scenario.stateRules),
    LEVEL_ADAPTATION: formatList(scenario.levelAdaptation),
    ERROR_HANDLING: formatList(scenario.errorHandling),
    EXPRESSION_HELP: formatExpressionHelp(scenario.expressionHelp),
    SOURCE_EXAMPLES: formatNamedDialogues(scenario.sourceExamples),
    TEST_INPUTS: formatList(scenario.testInputs)
  };
  return template.replace(/\{\{([A-Z_]+)\}\}/g, (_match, key) => values[key] ?? "");
}

function loadBundle(scenarioId) {
  const promptFileName = IELTS_PROMPTS[scenarioId];
  if (!promptFileName) {
    throw new Error(`Unknown IELTS scenario: ${scenarioId}`);
  }

  const scenarioPath = path.join(root, "scenarios", `${scenarioId}.json`);
  const promptPath = path.join(root, "prompts", promptFileName);
  const commonPath = path.join(root, "common", "ielts-core.md");

  const scenario = readJson(scenarioPath);
  const commonPrompt = fs.readFileSync(commonPath, "utf8");
  const scenarioPrompt = fs.readFileSync(promptPath, "utf8");
  const systemPrompt = renderTemplate(`${commonPrompt}\n\n${scenarioPrompt}`, scenario);
  return { scenario, systemPrompt };
}

async function callApi(messages) {
  if (!config.apiKey) {
    throw new Error("Missing SJTU_AI_API_KEY.");
  }

  const response = await fetch(`${config.endpoint}/chat/completions`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${config.apiKey}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      model: config.model,
      messages,
      temperature: config.temperature,
      max_tokens: config.maxTokens
    })
  });

  const text = await response.text();
  if (!response.ok) {
    throw new Error(`API ${response.status}: ${text.substring(0, 160)}`);
  }
  const body = JSON.parse(text);
  return body?.choices?.[0]?.message?.content?.trim() || "";
}

const TESTS = {
  "IELTS-P1-practice": [
    ["choose topic", "Hometown."],
    ["normal answer", "I'm from Chengdu. I like it because life is comfortable and the food is excellent."],
    ["chinese support", "我想说生活节奏比较慢，但是交通越来越堵。"],
    ["grammar issue", "The traffic become bad."],
    ["score request", "结束，给我评分。"]
  ],
  "IELTS-P2-practice": [
    ["choose cue card", "A person I admire."],
    ["full answer", "The person I admire most is my high school physics teacher. He was patient and always encouraged us to ask questions. When I failed an exam, he helped me understand my mistakes, and that made me more confident."],
    ["chinese support", "我想说我敬佩我的高中物理老师。"],
    ["struggling", "I don't know how to tell the story."],
    ["score request", "结束，给我评分。"]
  ],
  "IELTS-P3-practice": [
    ["choose topic", "Education."],
    ["normal answer", "I think a good teacher should be patient and able to inspire students, because many students need encouragement as well as knowledge."],
    ["chinese support", "我想说现在教育太应试了。"],
    ["short answer", "Technology is good but also bad."],
    ["score request", "结束，给我评分。"]
  ],
  "IELTS-mock-test": [
    ["part 1 answer", "I'm a student."],
    ["part 1 detail", "I chose computer science because it is useful and I enjoy solving problems."],
    ["chinese during test", "我想说我平时喜欢打篮球。"],
    ["score request", "结束，给我评分。"]
  ]
};

function detectIssues(scenarioId, label, input, reply) {
  const issues = [];
  const isMock = scenarioId === "IELTS-mock-test";
  const hasChineseInput = /[\u4e00-\u9fff]/.test(input);
  const hasChineseReply = /[\u4e00-\u9fff]/.test(reply);
  const lowerReply = reply.toLowerCase();

  if (reply.length > 420) {
    issues.push(`${scenarioId}/${label}: reply too long (${reply.length} chars)`);
  }
  if (/[*#`]|^\s*[-\d]+[.)]/m.test(reply)) {
    issues.push(`${scenarioId}/${label}: markdown or list-like formatting`);
  }
  if (/\bAI\b|\bmodel\b|\bprompt\b|\bsystem rule\b/i.test(reply)) {
    issues.push(`${scenarioId}/${label}: mentions AI/model/prompt/system rules`);
  }
  if (/\bFeedback:|\bQuestion:|\bCue card:|\bScore:/i.test(reply)) {
    issues.push(`${scenarioId}/${label}: uses learner-facing label`);
  }
  if (isMock && !label.includes("score") && /band|score|feedback|try saying|you could say|可以这样说/i.test(reply)) {
    issues.push(`${scenarioId}/${label}: mock test coached or scored during test`);
  }
  if (!isMock && hasChineseInput && !hasChineseReply && !/try|you could say|natural answer|in english/i.test(lowerReply)) {
    issues.push(`${scenarioId}/${label}: Chinese input did not trigger visible support`);
  }

  return issues;
}

async function run() {
  fs.mkdirSync(outDir, { recursive: true });
  const issues = [];

  for (const [scenarioId, cases] of Object.entries(TESTS)) {
    const { scenario, systemPrompt } = loadBundle(scenarioId);
    const log = [`=== ${scenarioId}: ${scenario.title} ===`, `Opening: ${scenario.openingMessage}`, ""];

    for (const [label, input] of cases) {
      const messages = [
        { role: "system", content: systemPrompt },
        { role: "assistant", content: scenario.openingMessage },
        { role: "user", content: input }
      ];

      try {
        const reply = await callApi(messages);
        log.push(`[${label}]`);
        log.push(`  IN:  ${input.substring(0, 100)}`);
        log.push(`  OUT: ${reply.substring(0, 300)}`);
        issues.push(...detectIssues(scenarioId, label, input, reply));
        log.push("");
      } catch (error) {
        log.push(`[${label}] ERROR: ${error.message}`, "");
        issues.push(`${scenarioId}/${label}: API failed - ${error.message}`);
      }

      await new Promise((resolve) => setTimeout(resolve, 8000));
    }

    fs.writeFileSync(path.join(outDir, `${scenarioId}.log`), log.join("\n"), "utf8");
    console.log(`DONE: ${scenarioId}`);
  }

  if (issues.length > 0) {
    const report = `# IELTS Test Issues\n\n${issues.map((issue) => `- ${issue}`).join("\n")}\n`;
    fs.writeFileSync(path.join(outDir, "IELTS-ISSUES.md"), report, "utf8");
    console.log("\n=== Issues ===");
    for (const issue of issues) {
      console.log(`  ${issue}`);
    }
  } else {
    fs.writeFileSync(path.join(outDir, "IELTS-ISSUES.md"), "# IELTS Test Issues\n\nAll IELTS scenario checks passed.\n", "utf8");
    console.log("\nAll IELTS scenario checks passed.");
  }
}

run().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
