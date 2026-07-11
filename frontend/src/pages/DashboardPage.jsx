import { useCallback } from "react";
import { Typography } from "antd";
import { useNavigate } from "react-router-dom";
import { DashboardCommunityLearning } from "../components/Dashboard/DashboardCommunityLearning";
import { DashboardHeroCard } from "../components/Dashboard/DashboardHeroCard";
import { DashboardModuleGrid } from "../components/Dashboard/DashboardModuleGrid";
import { AsyncPage } from "../components/common/AsyncPage";
import { MetricIcon } from "../components/common/MetricIcon";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Title, Text } = Typography;

const weeklyOverviewItems = [
  { key: "speakingDuration", label: "本周开口时长", icon: "microphone" },
  { key: "pronunciationAccuracy", label: "发音准确率", icon: "waveform" },
  { key: "learningDays", label: "学习天数", icon: "streak" },
  { key: "vocabularyLearned", label: "本周学习词汇", icon: "vocabulary" },
  { key: "grammarPracticed", label: "本周学习语法", icon: "grammar" }
];

export function DashboardPage() {
  const navigate = useNavigate();
  const { dashboard, profile } = useAppServices();

  const loader = useCallback(async () => {
    const [
      recommendedTask,
      studyPlan,
      weeklyOverview,
      communityLearningTrends,
      profileSnapshot
    ] = await Promise.all([
      dashboard.getRecommendedTask(),
      dashboard.getStudyPlan(),
      dashboard.getWeeklyOverview(),
      dashboard.getCommunityLearningTrends(),
      profile.getSnapshot()
    ]);

    return {
      recommendedTask,
      studyPlan,
      weeklyOverview,
      communityLearningTrends,
      profileSnapshot
    };
  }, [dashboard, profile]);

  const { data, loading, error } = useAsyncData(loader, [loader]);

  return (
    <AsyncPage loading={loading} error={error}>
      {data ? (
        <div className="page-stack">
          <DashboardHeroCard
            recommendedTask={data.recommendedTask}
            studyPlan={data.studyPlan}
            onStartSpeaking={() => navigate("/speaking")}
            onViewPlan={() => navigate("/profile")}
          />

          <DashboardModuleGrid onNavigate={navigate} />

          <section className="glass-panel">
            <Title level={4}>本周概览</Title>
            <div className="metrics-grid dashboard-weekly-grid">
              {weeklyOverviewItems.map((stat) => (
                <div className="metric-chip" key={stat.key}>
                  <div className="stat-tile__icon">
                    <MetricIcon icon={stat.icon} />
                  </div>
                  <Text type="secondary">{stat.label}</Text>
                    <Title level={3}>{data.weeklyOverview[stat.key]}</Title>
                </div>
              ))}
            </div>
          </section>

          <DashboardCommunityLearning
            learningTrends={data.communityLearningTrends}
          />
        </div>
      ) : null}
    </AsyncPage>
  );
}
