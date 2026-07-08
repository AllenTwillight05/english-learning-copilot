import { Button, Flex, Tag, Typography } from "antd";

const { Text } = Typography;

export function VocabularyLevelCard({ title, description, icon, tag }) {
  return (
    <Button className="practice-level-button" htmlType="button">
      <span className="practice-level-button__icon">{icon}</span>
      <span className="practice-level-button__body">
        <Flex justify="space-between" align="center" gap={12}>
          <Text strong>{title}</Text>
          <Tag bordered={false} className="soft-tag">
            {tag}
          </Tag>
        </Flex>
        <span className="helper-text">{description}</span>
      </span>
    </Button>
  );
}
