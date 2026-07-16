import { afterEach, describe, expect, it, vi } from "vitest";
import { createHttpServices } from "../../../src/services/httpServices";

describe("vocabulary HTTP service", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("sends the selected practice level as a query parameter", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => []
    });
    vi.stubGlobal("fetch", fetchMock);

    const services = createHttpServices("http://localhost:8080");
    await services.vocabulary.getVocabularyPracticeWords({ level: "advanced" });

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/vocabulary/practice-words?level=advanced",
      expect.objectContaining({ headers: expect.any(Object) })
    );
  });
});
