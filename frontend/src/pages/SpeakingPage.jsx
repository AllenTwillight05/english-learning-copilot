import { useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { ScenarioGrid } from "../components/Speaking/ScenarioGrid";
import { AsyncPage } from "../components/common/AsyncPage";
import { PageSectionHeader } from "../components/common/PageSectionHeader";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

export function SpeakingPage() {
  const navigate = useNavigate();
  const { speaking } = useAppServices();
  const loader = useCallback(() => speaking.getCatalog(), [speaking]);
  const { data, loading, error } = useAsyncData(loader, [loader]);

  const scenarios = useMemo(() => data?.scenarios ?? [], [data]);

  return (
    <AsyncPage loading={loading} error={error}>
      {data ? (
        <div className="page-stack">
          <section className="glass-panel">
            <PageSectionHeader
              eyebrow=""
              title="口语练习"
              description="选择一个真实情景模块，进入详情页后再开始会话练习。"
            />
          </section>

          <ScenarioGrid
            scenarios={scenarios}
            onSelect={(scenario) => navigate(`/speaking/${scenario.id}`)}
          />
        </div>
      ) : null}
    </AsyncPage>
  );
}
