import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { SpeakingPage } from "../../../src/pages/SpeakingPage";
import { speakingScenariosMock } from "../../../src/services/mockData";
import { renderWithProviders } from "../utils/renderWithProviders";

describe("SpeakingPage", () => {
  it("shows six scenario cards and navigates to the selected scenario", async () => {
    renderWithProviders(<SpeakingPage />, {
      path: "/speaking",
      route: "/speaking"
    });

    for (const scenario of speakingScenariosMock) {
      expect(await screen.findByRole("button", { name: new RegExp(scenario.title) })).toBeInTheDocument();
    }

    await userEvent.click(screen.getByRole("button", { name: /商务会谈/ }));
    expect(await screen.findByText("详情页占位")).toBeInTheDocument();
  });
});
