import { FireFilled, ReadOutlined } from "@ant-design/icons";
import { Progress, Typography } from "antd";

const { Title, Text } = Typography;

function PlanProgressItem({ label, completed, total, strokeColor }) {
  const percent = total > 0 ? Math.round((completed / total) * 100) : 0;

  return (
    <div className="dashboard-study-plan__item">
      <div className="dashboard-study-plan__row">
        <Text strong>{label}</Text>
        <Text className="dashboard-study-plan__count">
          {completed}/{total}
        </Text>
      </div>
      <Progress
        percent={percent}
        showInfo={false}
        strokeColor={strokeColor}
        trailColor="rgba(17, 17, 17, 0.08)"
      />
    </div>
  );
}

export function DashboardStudyPlanNote({ plan }) {
  return (
    <aside className="dashboard-study-plan">
      <div className="dashboard-study-plan__header">
        <div className="dashboard-study-plan__mark">
          <ReadOutlined />
        </div>
        <div>
          <Title level={4}>今日练习概览</Title>
        </div>
      </div>

      <div className="dashboard-study-plan__list">
        <PlanProgressItem
          label="口语"
          completed={plan.speaking.completed}
          total={plan.speaking.total}
          strokeColor="#c08a1d"
        />
        <PlanProgressItem
          label="词汇"
          completed={plan.vocabulary.completed}
          total={plan.vocabulary.total}
          strokeColor="#0f766e"
        />
        <PlanProgressItem
          label="语法"
          completed={plan.grammar.completed}
          total={plan.grammar.total}
          strokeColor="#2356c4"
        />
      </div>

      <div className="dashboard-study-plan__streak">
        <span className="dashboard-study-plan__fire">
          <FireFilled />
        </span>
        <Text strong>坚持 {plan.streakDays} 天</Text>
      </div>
    </aside>
  );
}
