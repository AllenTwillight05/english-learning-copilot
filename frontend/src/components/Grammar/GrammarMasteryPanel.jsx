import { Flex, Progress, Typography } from "antd";

const { Title, Text } = Typography;

export function GrammarMasteryPanel({ overview }) {
  return (
    <div className="memory-panel">
      <Flex justify="space-between" align="center" gap={12}>
        <Text strong>当前语法掌握率</Text>
        <Title level={2}>{overview.masteryRate}%</Title>
      </Flex>
      <Progress
        percent={overview.masteryRate}
        showInfo={false}
        strokeColor={{
          "0%": "#111111",
          "55%": "#3d4f93",
          "100%": "#0f766e"
        }}
        trailColor="rgba(17, 17, 17, 0.08)"
      />
      <div className="memory-stat-grid">
        {overview.stats.map((stat) => (
          <div className="memory-stat" key={stat.label}>
            <Title level={3}>{stat.value}</Title>
            <Text type="secondary">{stat.label}</Text>
          </div>
        ))}
      </div>
    </div>
  );
}
