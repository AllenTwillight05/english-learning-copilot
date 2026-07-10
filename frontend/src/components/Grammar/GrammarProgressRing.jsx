import { Progress, Typography } from "antd";

const { Text, Title } = Typography;

export function GrammarProgressRing({ completed, total }) {
  const percent = total > 0 ? Math.round((completed / total) * 100) : 0;

  return (
    <div className="practice-progress-ring">
      <Progress
        format={() => (
          <span className="practice-progress-ring__value">
            {completed}/{total}
          </span>
        )}
        percent={percent}
        size={142}
        strokeColor="#3d4f93"
        trailColor="rgba(17, 17, 17, 0.08)"
        type="circle"
      />
      <div>
        <Title level={4}>今日语法进度</Title>
        <Text type="secondary">{total} 题中已完成 {completed} 题</Text>
      </div>
    </div>
  );
}
