import { Progress, Typography } from "antd";

const { Text, Title } = Typography;

export function GrammarProgressRing({ completed, total, remaining }) {
  const percent = total > 0 ? Math.round((completed / total) * 100) : 0;
  const pending = remaining ?? Math.max(total - completed, 0);

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
        <br />
        <Text type="secondary">今日待练：{pending} 题</Text>
      </div>
    </div>
  );
}
