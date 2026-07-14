import { render } from "@testing-library/react";
import { ConfigProvider } from "antd";
import zhCN from "antd/locale/zh_CN";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { appTheme } from "../../../src/app/theme";
import { AppServicesProvider } from "../../../src/services/ServiceContext";
import { speakingScenariosMock } from "../../../src/services/mockData";

function createTestServices(scenarios = speakingScenariosMock) {
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

export function renderWithProviders(
  ui,
  {
    route = "/speaking/business-opening",
    path = "/speaking/:scenarioId",
    services = createTestServices()
  } = {}
) {
  return render(
    <ConfigProvider locale={zhCN} theme={appTheme}>
      <AppServicesProvider services={services}>
        <MemoryRouter initialEntries={[route]}>
          <Routes>
            <Route path={path} element={ui} />
            <Route path="/speaking/:scenarioId/feedback" element={<div>反馈页占位</div>} />
            <Route path="/speaking/:scenarioId/conversation" element={<div>会话页占位</div>} />
            <Route path="/speaking/:scenarioId" element={<div>详情页占位</div>} />
            <Route path="/speaking" element={<div>口语页占位</div>} />
          </Routes>
        </MemoryRouter>
      </AppServicesProvider>
    </ConfigProvider>
  );
}
