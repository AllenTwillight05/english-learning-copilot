import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { SpeakingPage } from "../../../src/pages/SpeakingPage";
import { renderWithProviders } from "../utils/renderWithProviders";

describe("SpeakingPage", () => {
  it("shows daily and IELTS entry cards and opens the selected practice area", async () => {
    renderWithProviders(<SpeakingPage />, {
      path: "/speaking",
      route: "/speaking"
    });

    expect(await screen.findByRole("button", { name: /日常口语/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /雅思口语/ })).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: /日常口语/ }));
    expect(await screen.findByText("日常口语页占位")).toBeInTheDocument();
  });
});
