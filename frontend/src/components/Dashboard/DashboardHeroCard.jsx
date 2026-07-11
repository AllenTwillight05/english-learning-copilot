import { CalendarOutlined, PlayCircleOutlined } from "@ant-design/icons";
import { Button, Typography } from "antd";
import { DashboardStudyPlanNote } from "./DashboardStudyPlanNote";
import { DashboardTaskNote } from "./DashboardTaskNote";

const { Title, Text } = Typography;

export function DashboardHeroCard({
  recommendedTask,
  studyPlan,
  onStartSpeaking,
  onViewPlan
}) {
  return (
    <section className="glass-panel dashboard-hero-card">
      <div className="dashboard-hero-card__content">
        <Title className="dashboard-hero-card__title">
          英语口语学习助手
        </Title>
        <Text className="dashboard-hero-card__subtitle">
          每天完成一次真实表达，稳步提升开口能力。
        </Text>

        <div className="dashboard-hero-card__actions">
          <Button
            type="primary"
            size="large"
            icon={<PlayCircleOutlined />}
            onClick={onStartSpeaking}
          >
            开始口语练习
          </Button>
          <Button size="large" icon={<CalendarOutlined />} onClick={onViewPlan}>
            查看个人主页
          </Button>
        </div>
      </div>

      <DashboardTaskNote
        task={recommendedTask}
        onStart={onStartSpeaking}
      />

      <DashboardStudyPlanNote plan={studyPlan} />
    </section>
  );
}
