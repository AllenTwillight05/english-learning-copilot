import { describe, expect, it } from "vitest";
import { speakingScenariosMock } from "../../../src/services/mockData";

describe("speakingScenariosMock", () => {
  it("contains six complete speaking scenarios", () => {
    expect(speakingScenariosMock).toHaveLength(6);

    speakingScenariosMock.forEach((scenario) => {
      expect(scenario).toEqual(
        expect.objectContaining({
          id: expect.any(String),
          title: expect.any(String),
          level: expect.any(String),
          accent: expect.any(String),
          duration: expect.any(String),
          summary: expect.any(String),
          tone: expect.any(String),
          goal: expect.any(String),
          keywords: expect.any(Array),
          prompts: expect.any(Array),
          feedback: expect.any(Object)
        })
      );
      expect(scenario.keywords.length).toBeGreaterThanOrEqual(3);
      expect(scenario.prompts).toHaveLength(4);
      expect(scenario.prompts.map((prompt) => prompt.role)).toEqual([
        "coach",
        "learner",
        "coach",
        "learner"
      ]);
      expect(scenario.feedback).toEqual(
        expect.objectContaining({
          totalScore: expect.any(Number),
          pronunciation: expect.any(Number),
          fluency: expect.any(Number),
          speed: expect.any(String),
          issueSentences: expect.any(Array),
          suggestions: expect.any(Array)
        })
      );
    });
  });
});
