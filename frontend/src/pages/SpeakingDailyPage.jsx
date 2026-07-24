import { useCallback, useMemo } from "react";
import { ArrowLeftOutlined } from "@ant-design/icons";
import { Button } from "antd";
import { useNavigate } from "react-router-dom";
import { ScenarioGrid } from "../components/Speaking/ScenarioGrid";
import { AsyncPage } from "../components/common/AsyncPage";
import { PageSectionHeader } from "../components/common/PageSectionHeader";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";
import { DAILY_SPEAKING_SCENARIOS } from "../services/speakingCatalog";

function mergeDailyScenarios(apiScenarios) {
  const byId = new Map((apiScenarios ?? []).map((scenario) => [scenario.id, scenario]));

  return DAILY_SPEAKING_SCENARIOS.map((scenario) => ({
    ...scenario,
    ...(byId.get(scenario.id) ?? {})
  }));
}

export function SpeakingDailyPage() {
  const navigate = useNavigate();
  const { speaking } = useAppServices();
  const loader = useCallback(() => speaking.listScenarios(), [speaking]);
  const { data, loading, error } = useAsyncData(loader, [loader]);
  const scenarios = useMemo(() => mergeDailyScenarios(data), [data]);

  return (
    <AsyncPage loading={loading} error={error}>
      {data ? (
        <div className="page-stack">
          <section className="glass-panel">
            <PageSectionHeader
              eyebrow="Daily Speaking"
              title="日常口语"
              description="G 系列场景覆盖出行、生活事务、社交和职场沟通。"
              extra={
                <Button icon={<ArrowLeftOutlined />} onClick={() => navigate("/speaking")}>
                  返回
                </Button>
              }
            />
          </section>

          <ScenarioGrid
            scenarios={scenarios}
            onSelect={(scenario) =>
              navigate(`/speaking/${scenario.id}`, {
                state: { speakingBackPath: "/speaking/daily" }
              })
            }
          />
        </div>
      ) : null}
    </AsyncPage>
  );
}
