import { CheckCircleFilled, SettingOutlined } from "@ant-design/icons";
import { Button, List, Tag, Typography } from "antd";
import { PageSectionHeader } from "../common/PageSectionHeader";

const { Text } = Typography;

export function LearningPlanPanel({ plan, onEdit }) {
  return (
    <section className="glass-panel">
      <PageSectionHeader
        eyebrow="Learning Plan"
        title="今日学习计划"
        extra={
          <Button
            htmlType="button"
            icon={<SettingOutlined />}
            onClick={onEdit}
            size="small"
            type="primary"
          >
            设置计划
          </Button>
        }
      />
      <List
        dataSource={plan.items}
        renderItem={(item) => (
          <List.Item className="plain-list-item">
            <div className="plan-row">
              <div>
                <Text strong>{item.task}</Text>
                <div className="plan-meta">
                  {item.time} - {item.meta}
                </div>
              </div>
              {item.done ? <CheckCircleFilled className="success-icon" /> : null}
            </div>
          </List.Item>
        )}
      />
      {plan.allDone ? (
        <Tag bordered={false} className="soft-tag">
          今日已完成
        </Tag>
      ) : null}
    </section>
  );
}
