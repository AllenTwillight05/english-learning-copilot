import { Button, Flex, Progress, Space, Tag, Typography } from "antd";

const { Title, Paragraph, Text } = Typography;

const cardTones = ["blue", "gold", "mint"];

export function GrammarTopicGrid({ topics, onStart }) {
  return (
    <section className="feature-grid">
      {topics.map((topic, index) => {
        const tone = cardTones[index % cardTones.length];

        return (
          <article className="deck-card grammar-topic-card" key={topic.id}>
            <div className={`scenario-card__tone scenario-card__tone--${tone}`} />
            <div className="grammar-topic-card__body">
              <Flex justify="space-between" align="center" gap={12}>
                <Title level={4}>{topic.title}</Title>
                <Tag bordered={false} className="soft-tag">
                  {topic.tag}
                </Tag>
              </Flex>
              <Paragraph>{topic.summary}</Paragraph>
              <div className="script-preview">
                {topic.examples.map((example) => (
                  <div className="script-line" key={example}>
                    {example}
                  </div>
                ))}
              </div>
              <Progress percent={topic.progress} showInfo={false} />
              <Flex justify="space-between" align="center">
                <Space size={6}>
                  <Text type="secondary">正确率</Text>
                  <Text strong>{topic.progress}%</Text>
                </Space>
                <Button htmlType="button" onClick={() => onStart(topic)}>
                  开始练习
                </Button>
              </Flex>
            </div>
          </article>
        );
      })}
    </section>
  );
}
