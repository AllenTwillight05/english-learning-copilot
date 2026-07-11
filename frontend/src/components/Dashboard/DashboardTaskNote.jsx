import { CheckCircleOutlined } from "@ant-design/icons";
import { Button, Typography } from "antd";

const { Title, Text } = Typography;

export function DashboardTaskNote({ task, onStart }) {
  return (
    <aside className="dashboard-task-note">
      <div className="dashboard-task-note__header">
        <div className="dashboard-task-note__mark">
          <CheckCircleOutlined />
        </div>
        <div>
          <Title level={4}>今日推荐口语训练</Title>
        </div>
      </div>

      <div className="dashboard-task-note__topic">
        <Text strong>{task.topic}</Text>
      </div>

      <div className="dashboard-task-note__meta">
        <div>
          <Text strong>建议时长</Text>
          <Text strong>{task.suggestedDuration}</Text>
        </div>
        <div>
          <Text strong>训练强度</Text>
          <Text strong>{task.intensity}</Text>
        </div>
      </div>

      <Button type="primary" block onClick={onStart}>
        进入训练
      </Button>
    </aside>
  );
}
