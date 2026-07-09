import { Flex, Progress, Typography } from "antd";

const { Title, Text } = Typography;

export function MemoryRetentionPanel({ overview }) {
  return (
    <div className="memory-panel">
      <Flex justify="space-between" align="center" gap={12}>
        <Text strong>{overview.retentionLabel}</Text>
        <Title level={2}>{overview.retentionRate}%</Title>
      </Flex>
      <Progress
        percent={overview.retentionRate}
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
