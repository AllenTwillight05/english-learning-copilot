import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { SpeakingConversationPage } from "../../../src/pages/SpeakingConversationPage";
import { SpeakingFeedbackPage } from "../../../src/pages/SpeakingFeedbackPage";
import { SpeakingScenarioDetailPage } from "../../../src/pages/SpeakingScenarioDetailPage";
import { renderWithProviders } from "../utils/renderWithProviders";

const incompleteScenarios = [
  {
    id: "incomplete",
    title: "缺字段场景",
    level: "A1",
    accent: "基础表达",
    duration: "5 min",
    summary: "用于测试缺失字段。",
    tone: "blue",
    goal: "确认页面不会因为缺字段崩溃。"
  }
];

function createScenarioServices(scenarios) {
  return {
    speaking: {
      listScenarios: () => Promise.resolve(structuredClone(scenarios)),
      getScenario: (scenarioId) => {
        const scenario = scenarios.find((item) => item.id === scenarioId);
        return scenario
          ? Promise.resolve(structuredClone(scenario))
          : Promise.reject(new Error("Speaking scenario was not found."));
      }
    }
  };
}

describe("speaking exceptional states", () => {
  it("shows the missing scenario state when the scenario id does not exist", async () => {
    renderWithProviders(<SpeakingScenarioDetailPage />, {
      route: "/speaking/not-found"
    });

    expect(await screen.findByText("没有找到这个情景")).toBeInTheDocument();
  });

  it("shows a visible warning and falls back to prompts when localStorage replay JSON is malformed", async () => {
    window.localStorage.setItem("speaking-history:business-opening", "{bad json");

    renderWithProviders(<SpeakingFeedbackPage />, {
      path: "/speaking/:scenarioId/feedback",
      route: "/speaking/business-opening/feedback"
    });

    expect(await screen.findByText("评分结果")).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("历史回放记录损坏");
    expect(screen.getByText("Could we confirm whether Friday is still realistic for delivery?")).toBeInTheDocument();
  });

  it("degrades optional fields but blocks conversation when prompts are missing", async () => {
    const services = {
      ...createScenarioServices(incompleteScenarios)
    };

    renderWithProviders(<SpeakingScenarioDetailPage />, {
      route: "/speaking/incomplete",
      services
    });
    expect(await screen.findByText("缺字段场景")).toBeInTheDocument();
    expect(screen.getByText("暂无关键词")).toBeInTheDocument();
    expect(screen.getByText("对话示例")).toBeInTheDocument();
    expect(screen.getByText("对话脚本数据缺失，暂时无法进入会话练习。请返回口语页选择其他情景，或稍后重试。")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /进入会话/ })).toBeDisabled();
  });

  it("shows an explicit error state and disables dependent actions when prompts are missing", async () => {
    const services = {
      ...createScenarioServices(incompleteScenarios)
    };

    renderWithProviders(<SpeakingConversationPage />, {
      path: "/speaking/:scenarioId/conversation",
      route: "/speaking/incomplete/conversation",
      services
    });
    expect(await screen.findByText("对话脚本数据缺失，当前会话无法开始录音或交卷。请返回情景详情页重新选择。")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /开始录音/ })).toBeDisabled();
    expect(screen.getByRole("button", { name: /交卷/ })).toBeDisabled();

    await userEvent.click(screen.getByRole("button", { name: /开始录音/ }));
    expect(screen.getByText("当前为第 0 轮")).toBeInTheDocument();
    expect(window.localStorage.getItem("speaking-history:incomplete")).toBeNull();
  });

  it("shows an explicit feedback error instead of a misleading zero score when feedback is missing", async () => {
    const services = {
      ...createScenarioServices(incompleteScenarios)
    };

    renderWithProviders(<SpeakingFeedbackPage />, {
      path: "/speaking/:scenarioId/feedback",
      route: "/speaking/incomplete/feedback",
      services
    });
    expect(await screen.findByText("评分结果")).toBeInTheDocument();
    expect(screen.getByText("评分数据缺失，暂时无法展示本次练习结果。请返回会话页重新练习，或稍后重试。")).toBeInTheDocument();
    expect(screen.queryByText("N/A")).not.toBeInTheDocument();
  });
});
