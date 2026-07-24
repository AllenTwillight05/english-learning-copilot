#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import readline from "node:readline/promises";
import { stdin as input, stdout as output } from "node:process";

const moduleRoot = path.dirname(new URL(import.meta.url).pathname);

function parseArgs(argv) {
  const args = {
    scenario: "G-03-hotel",
    temperature: undefined,
    maxTokens: undefined
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    const next = argv[index + 1];
    if (arg === "--scenario" && next) {
      args.scenario = next;
      index += 1;
    } else if (arg === "--temperature" && next) {
      args.temperature = Number(next);
      index += 1;
    } else if (arg === "--max-tokens" && next) {
      args.maxTokens = Number(next);
      index += 1;
    } else if (arg === "--print-system") {
      args.printSystem = true;
    } else if (arg === "--help" || arg === "-h") {
      args.help = true;
    }
  }

  return args;
}

function loadDotEnv(filePath) {
  if (!fs.existsSync(filePath)) {
    return;
  }

  const content = fs.readFileSync(filePath, "utf8");
  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      continue;
    }

    const separator = trimmed.indexOf("=");
    if (separator === -1) {
      continue;
    }

    const key = trimmed.slice(0, separator).trim();
    const rawValue = trimmed.slice(separator + 1).trim();
    const value = rawValue.replace(/^['"]|['"]$/g, "");
    if (key && process.env[key] === undefined) {
      process.env[key] = value;
    }
  }
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function formatDialogue(dialogue) {
  return dialogue
    .map((turn) => `${turn.speaker}: ${turn.text}`)
    .join("\n");
}

function formatStringList(items) {
  if (!Array.isArray(items) || items.length === 0) {
    return "Not specified.";
  }
  return items.map((item) => `- ${item}`).join("\n");
}

function formatNamedDialogues(dialogues) {
  if (!Array.isArray(dialogues) || dialogues.length === 0) {
    return "Not specified.";
  }

  return dialogues
    .map((dialogue) => {
      const title = dialogue.title ? `${dialogue.title}\n` : "";
      return `${title}${formatDialogue(dialogue.turns ?? [])}`;
    })
    .join("\n\n");
}

function formatExpressionHelp(items) {
  if (!Array.isArray(items) || items.length === 0) {
    return "Not specified.";
  }

  return items
    .map((item) => {
      const triggers = Array.isArray(item.triggers) && item.triggers.length > 0
        ? `Triggers: ${item.triggers.join(", ")}\n`
        : "";
      const explanation = item.explanation ? `Explanation: ${item.explanation}\n` : "";
      return [
        `Intent: ${item.intent}`,
        triggers.trimEnd(),
        `Suggested phrase: ${item.phrase}`,
        explanation.trimEnd()
      ].filter(Boolean).join("\n");
    })
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
    KEYWORDS: scenario.keywords.join(", "),
    OPENING_MESSAGE: scenario.openingMessage,
    SAMPLE_DIALOGUE: formatDialogue(scenario.sampleDialogue),
    CONVERSATION_FLOW: formatStringList(scenario.conversationFlow),
    STATE_RULES: formatStringList(scenario.stateRules),
    LEVEL_ADAPTATION: formatStringList(scenario.levelAdaptation),
    ERROR_HANDLING: formatStringList(scenario.errorHandling),
    EXPRESSION_HELP: formatExpressionHelp(scenario.expressionHelp),
    SOURCE_EXAMPLES: formatNamedDialogues(scenario.sourceExamples),
    TEST_INPUTS: formatStringList(scenario.testInputs)
  };

  return template.replace(/\{\{([A-Z_]+)\}\}/g, (_match, key) => values[key] ?? "");
}

function resolvePromptPaths(scenarioId) {
  const ieltsPromptFiles = {
    "IELTS-P1-practice": "IELTS-P1-practice-system.md",
    "IELTS-P2-practice": "IELTS-P2-practice-system.md",
    "IELTS-P3-practice": "IELTS-P3-practice-system.md",
    "IELTS-mock-test": "IELTS-mock-test-system.md"
  };

  if (ieltsPromptFiles[scenarioId]) {
    return {
      commonPromptPath: path.join(moduleRoot, "common", "ielts-core.md"),
      promptPath: path.join(moduleRoot, "prompts", ieltsPromptFiles[scenarioId])
    };
  }

  return {
    commonPromptPath: path.join(moduleRoot, "common", "roleplay-core.md"),
    promptPath: path.join(moduleRoot, "prompts", `${scenarioId}-system.md`)
  };
}

function loadScenarioBundle(scenarioId) {
  const scenarioPath = path.join(moduleRoot, "scenarios", `${scenarioId}.json`);
  const { commonPromptPath, promptPath } = resolvePromptPaths(scenarioId);

  if (!fs.existsSync(scenarioPath)) {
    throw new Error(`Scenario file not found: ${scenarioPath}`);
  }
  if (!fs.existsSync(commonPromptPath)) {
    throw new Error(`Common prompt file not found: ${commonPromptPath}`);
  }
  if (!fs.existsSync(promptPath)) {
    throw new Error(`Prompt file not found: ${promptPath}`);
  }

  const scenario = readJson(scenarioPath);
  const commonPromptTemplate = fs.readFileSync(commonPromptPath, "utf8");
  const promptTemplate = fs.readFileSync(promptPath, "utf8");
  const systemPrompt = renderTemplate(`${commonPromptTemplate}\n\n${promptTemplate}`, scenario);

  return {
    scenario,
    commonPromptPath,
    promptPath,
    scenarioPath,
    systemPrompt
  };
}

function listScenarios() {
  const scenariosDir = path.join(moduleRoot, "scenarios");
  return fs.readdirSync(scenariosDir)
    .filter((fileName) => fileName.endsWith(".json"))
    .map((fileName) => {
      const scenario = readJson(path.join(scenariosDir, fileName));
      return {
        id: scenario.id,
        title: scenario.title,
        level: scenario.level
      };
    })
    .filter((scenario) => {
      const visibleIeltsEntrypoints = new Set([
        "IELTS-P1-practice",
        "IELTS-P2-practice",
        "IELTS-P3-practice",
        "IELTS-mock-test"
      ]);
      if (visibleIeltsEntrypoints.has(scenario.id)) {
        return true;
      }
      return !scenario.id.startsWith("IELTS");
    })
    .sort((left, right) => left.id.localeCompare(right.id));
}

function buildConfig(args) {
  loadDotEnv(path.join(moduleRoot, ".env"));

  return {
    endpoint: (process.env.SJTU_AI_ENDPOINT || "https://models.sjtu.edu.cn/api/v1").replace(/\/$/, ""),
    apiKey: process.env.SJTU_AI_API_KEY || "",
    model: process.env.SJTU_AI_MODEL || "deepseek-chat",
    temperature: Number.isFinite(args.temperature)
      ? args.temperature
      : Number(process.env.SJTU_AI_TEMPERATURE || 0.7),
    maxTokens: Number.isFinite(args.maxTokens)
      ? args.maxTokens
      : Number(process.env.SJTU_AI_MAX_TOKENS || 180)
  };
}

async function callChatCompletions(config, messages) {
  if (!config.apiKey) {
    throw new Error("Missing SJTU_AI_API_KEY. Set it in llm-prompt-lab/.env or export it in your shell.");
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
  let body = null;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = null;
  }

  if (!response.ok) {
    const message = body?.error?.message || body?.message || text || response.statusText;
    throw new Error(`API request failed (${response.status}): ${message}`);
  }

  const content = body?.choices?.[0]?.message?.content?.trim();
  if (!content) {
    throw new Error(`API response did not include choices[0].message.content: ${text}`);
  }
  return content;
}

function printHelp() {
  console.log(`
Commands:
  /help     Show this help.
  /scenarios List available scenarios.
  /system   Print the rendered system prompt.
  /reload   Reload scenario JSON and system prompt from disk.
  /reset    Clear conversation history.
  /save     Save transcript under transcripts/.
  /exit     Exit.
`);
}

function saveTranscript(bundle, history) {
  const outputDir = path.join(moduleRoot, "transcripts");
  fs.mkdirSync(outputDir, { recursive: true });
  const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
  const filePath = path.join(outputDir, `${bundle.scenario.id}-${timestamp}.json`);
  fs.writeFileSync(filePath, JSON.stringify({
    scenario: bundle.scenario,
    savedAt: new Date().toISOString(),
    messages: history
  }, null, 2));
  return filePath;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  if (args.help) {
    console.log("Usage: node chat.mjs [--scenario G-03-hotel] [--temperature 0.7] [--max-tokens 180] [--print-system]");
    printHelp();
    return;
  }

  const config = buildConfig(args);
  let bundle = loadScenarioBundle(args.scenario);
  if (args.printSystem) {
    console.log(bundle.systemPrompt);
    return;
  }
  let messages = [{ role: "system", content: bundle.systemPrompt }];
  const transcript = [];

  console.log(`LLM Prompt Lab: ${bundle.scenario.title}`);
  console.log(`Model: ${config.model}`);
  console.log(`Common: ${path.relative(moduleRoot, bundle.commonPromptPath)}`);
  console.log(`Prompt: ${path.relative(moduleRoot, bundle.promptPath)}`);
  console.log(`Opening: ${bundle.scenario.openingMessage}`);
  console.log("Type /help for commands.\n");

  const rl = readline.createInterface({ input, output });

  try {
    while (true) {
      const userInput = (await rl.question("You> ")).trim();
      if (!userInput) {
        continue;
      }

      if (userInput === "/exit" || userInput === "/quit") {
        break;
      }
      if (userInput === "/help") {
        printHelp();
        continue;
      }
      if (userInput === "/scenarios") {
        for (const scenario of listScenarios()) {
          console.log(`${scenario.id}  ${scenario.title}  (${scenario.level})`);
        }
        console.log("");
        continue;
      }
      if (userInput === "/system") {
        console.log(`\n${bundle.systemPrompt}\n`);
        continue;
      }
      if (userInput === "/reload") {
        bundle = loadScenarioBundle(args.scenario);
        messages = [{ role: "system", content: bundle.systemPrompt }, ...messages.filter((msg) => msg.role !== "system")];
        console.log("Reloaded scenario and system prompt.\n");
        continue;
      }
      if (userInput === "/reset") {
        messages = [{ role: "system", content: bundle.systemPrompt }];
        transcript.length = 0;
        console.log("Conversation history cleared.\n");
        continue;
      }
      if (userInput === "/save") {
        const savedPath = saveTranscript(bundle, transcript);
        console.log(`Saved transcript: ${path.relative(moduleRoot, savedPath)}\n`);
        continue;
      }

      messages.push({ role: "user", content: userInput });
      transcript.push({ role: "user", content: userInput });

      try {
        const assistantReply = await callChatCompletions(config, messages);
        messages.push({ role: "assistant", content: assistantReply });
        transcript.push({ role: "assistant", content: assistantReply });
        console.log(`Agent> ${assistantReply}\n`);
      } catch (error) {
        messages.pop();
        transcript.pop();
        console.error(`Error: ${error.message}\n`);
      }
    }
  } finally {
    rl.close();
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
