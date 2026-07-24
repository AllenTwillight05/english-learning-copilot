import { AxeBuilder } from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

const scenarioTitles = [
  "商务会谈",
  "机场值机与改签",
  "晚餐社交破冰",
  "医院就诊沟通",
  "商场退换货",
  "咖啡厅点单与社交"
];

async function stubBrowserAudio(page) {
  await page.addInitScript(() => {
    window.__spokenTexts = [];
    Object.defineProperty(window, "speechSynthesis", {
      configurable: true,
      value: {
        speak: (utterance) => {
          window.__spokenTexts.push(utterance.text);
          window.setTimeout(() => utterance.onend?.(), 0);
        },
        cancel: () => {}
      }
    });
    Object.defineProperty(window, "SpeechSynthesisUtterance", {
      configurable: true,
      value: class SpeechSynthesisUtterance {
        constructor(text) {
          this.text = text;
          this.onend = null;
          this.onerror = null;
        }
      }
    });
    Object.defineProperty(window, "Audio", {
      configurable: true,
      value: class AudioMock {
        play() {
          return Promise.resolve();
        }
        pause() {}
      }
    });
    const tracks = [{ stop: () => {} }];
    const stream = { getTracks: () => tracks };
    Object.defineProperty(window, "MediaRecorder", {
      configurable: true,
      value: class MediaRecorderMock {
        constructor(_stream, options = {}) {
          this.state = "inactive";
          this.mimeType = options.mimeType || "audio/webm";
          this.ondataavailable = null;
          this.onstop = null;
          this.onerror = null;
        }

        static isTypeSupported() {
          return true;
        }

        start() {
          this.state = "recording";
        }

        stop() {
          this.state = "inactive";
          this.ondataavailable?.({ data: new Blob(["mock-recording"], { type: this.mimeType }) });
          window.setTimeout(() => this.onstop?.(), 0);
        }
      }
    });
    Object.defineProperty(navigator, "mediaDevices", {
      configurable: true,
      value: { getUserMedia: async () => stream }
    });
  });
}

async function openReplay(page) {
  await page.getByRole("button", { name: /查看回放/ }).click();
  const dialog = page.getByRole("dialog", { name: /会话回放/ });
  await expect(dialog).toBeVisible();
  return dialog;
}

test.beforeEach(async ({ page }) => {
  await stubBrowserAudio(page);
});

test("shows six speaking scenarios and navigates from a card", async ({ page }) => {
  await page.goto("/speaking");

  await expect(page.getByRole("heading", { name: "口语练习" })).toBeVisible();
  for (const title of scenarioTitles) {
    await expect(page.getByRole("button", { name: new RegExp(title) })).toBeVisible();
  }

  await page.getByRole("button", { name: /商务会谈/ }).click();
  await expect(page).toHaveURL(/\/speaking\/business-opening$/);
  await expect(page.getByRole("heading", { name: "商务会谈" })).toBeVisible();
});

test("completes the real browser speaking practice path and stores replay history", async ({ page }) => {
  await page.goto("/speaking");
  await page.getByRole("button", { name: /商务会谈/ }).click();
  await page.getByRole("button", { name: /进入会话/ }).click();

  await expect(page).toHaveURL(/\/speaking\/business-opening\/conversation$/);
  await expect(page.getByRole("button", { name: /交卷/ })).toBeDisabled();

  const playbackButtons = page.getByRole("button", { name: "播放此句音频" });
  await expect(playbackButtons).toHaveCount(4);
  await playbackButtons.nth(0).click();
  await expect
    .poll(() => page.evaluate(() => window.__spokenTexts.at(-1)))
    .toBe("Good morning. Could you briefly introduce today's agenda?");

  for (let count = 1; count <= 3; count += 1) {
    await page.getByRole("button", { name: /开始录音/ }).click();
    await expect(page.getByRole("button", { name: /停止录音/ })).toBeEnabled();
    await page.getByRole("button", { name: /停止录音/ }).click();
    await expect(page.getByText(`当前为第 ${count} 轮`)).toBeVisible();
  }

  await expect(page.getByRole("button", { name: /交卷/ })).toBeEnabled();
  await page.getByRole("button", { name: /交卷/ }).click();
  await expect(page).toHaveURL(/\/speaking\/business-opening\/feedback$/);
  await expect(page.getByRole("heading", { name: "评分结果" })).toBeVisible();

  const savedReplay = await page.evaluate(() => window.localStorage.getItem("speaking-history:business-opening"));
  expect(savedReplay).toContain("Good morning. Could you briefly introduce today's agenda?");

  const dialog = await openReplay(page);
  await expect(dialog.getByText("Good morning. Could you briefly introduce today's agenda?")).toBeVisible();
  await expect(dialog.getByRole("button", { name: /暂停|开始/ })).toBeVisible();

  await dialog.getByRole("button", { name: "Close" }).click();
  await expect(dialog).toBeHidden();

  await page.goto("/speaking/business-opening");
  await expect(page.getByRole("button", { name: /历史记录/ })).toBeEnabled();
  await page.getByRole("button", { name: /历史记录/ }).click();
  await expect(page).toHaveURL(/\/speaking\/business-opening\/feedback$/);
});

test("covers AntD modal focus management, scroll lock, animation, and replay controls", async ({ page }) => {
  await page.goto("/speaking/business-opening/feedback");
  await page.evaluate(() => {
    window.localStorage.setItem(
      "speaking-history:business-opening",
      JSON.stringify({
        savedAt: "2026-07-10T00:00:00.000Z",
        messages: [
          { role: "coach", text: "First replay line.", audioUrl: "" },
          { role: "learner", text: "Second replay line.", audioUrl: "" },
          { role: "coach", text: "Third replay line.", audioUrl: "" },
          { role: "learner", text: "Fourth replay line.", audioUrl: "" }
        ]
      })
    );
  });
  await page.reload();

  const trigger = page.getByRole("button", { name: /查看回放/ });
  await trigger.focus();
  await expect(trigger).toBeFocused();
  const dialog = await openReplay(page);

  await expect(dialog).toHaveClass(/ant-zoom/);
  await expect(dialog.getByText("First replay line.")).toBeVisible();
  await expect(dialog.getByRole("button", { name: /暂停|开始/ })).toBeVisible();

  await expect(page.locator("body")).toHaveCSS("overflow", "hidden");
  await page.keyboard.press("Escape");
  await expect(dialog).toBeHidden();
  await expect(trigger).toBeFocused();
});

test("has no critical accessibility violations on speaking pages and replay modal", async ({ page }) => {
  await page.goto("/speaking");
  for (const path of ["/speaking", "/speaking/business-opening", "/speaking/business-opening/conversation"]) {
    await page.goto(path);
    const results = await new AxeBuilder({ page }).analyze();
    expect(results.violations.filter((violation) => violation.impact === "critical")).toEqual([]);
  }

  await page.goto("/speaking/business-opening/feedback");
  await openReplay(page);
  const modalResults = await new AxeBuilder({ page }).include(".ant-modal").analyze();
  expect(modalResults.violations.filter((violation) => violation.impact === "critical")).toEqual([]);
});

test("matches speaking visual snapshots on desktop and mobile", async ({ page }, testInfo) => {
  await page.goto("/speaking");
  await expect(page).toHaveScreenshot(`speaking-entry-${testInfo.project.name}.png`, {
    fullPage: true,
    animations: "disabled"
  });

  await page.goto("/speaking/business-opening/feedback");
  await openReplay(page);
  await expect(page).toHaveScreenshot(`speaking-replay-modal-${testInfo.project.name}.png`, {
    fullPage: true,
    animations: "disabled"
  });
});

test("keeps the speaking layout usable on mobile", async ({ page, isMobile }) => {
  test.skip(!isMobile, "mobile layout coverage runs in the mobile project");

  await page.goto("/speaking");
  await expect(page.getByRole("button", { name: /商务会谈/ })).toBeVisible();
  const firstCardBox = await page.getByRole("button", { name: /商务会谈/ }).boundingBox();
  expect(firstCardBox.width).toBeLessThanOrEqual(390);

  await page.goto("/speaking/business-opening/feedback");
  const dialog = await openReplay(page);
  await expect(dialog.getByRole("button", { name: /暂停|开始/ })).toBeVisible();
  const bodyBox = await page.locator("body").boundingBox();
  const dialogBox = await dialog.boundingBox();
  expect(dialogBox.width).toBeLessThanOrEqual(bodyBox.width);
});
