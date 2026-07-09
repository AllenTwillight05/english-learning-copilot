import { Flex, Space, Tag, Typography } from "antd";

const { Title, Text, Paragraph } = Typography;

// 口语场景列表只负责渲染练习卡片，不处理数据请求。
export function ScenarioGrid({ scenarios, onSelect }) {
  return (
    <section className="feature-grid">
      {scenarios.map((scenario) => (
        <article
          className="scenario-card scenario-card--button"
          key={scenario.id}
          onClick={() => onSelect(scenario)}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              onSelect(scenario);
            }
          }}
          role="button"
          tabIndex={0}
        >
          <div className={`scenario-card__tone scenario-card__tone--${scenario.tone}`} />
          <div className="scenario-card__body scenario-card__body--spacious">
            <div className="scenario-card__header">
              <Title level={3} className="scenario-card__title">
                {scenario.title}
              </Title>
              <Flex justify="start" align="center">
                <Space size={8} wrap>
                  <Tag bordered={false} className="soft-tag">
                    {scenario.level}
                  </Tag>
                  <Text type="secondary">{scenario.duration}</Text>
                </Space>
              </Flex>
            </div>
            <div className="scenario-card__content">
              <Paragraph>{scenario.summary}</Paragraph>
              <Text type="secondary">{scenario.accent}</Text>
            </div>
            <Space size={[6, 6]} wrap>
              {scenario.keywords?.slice(0, 3).map((keyword) => (
                <Tag bordered={false} className="soft-tag" key={keyword}>
                  {keyword}
                </Tag>
              ))}
            </Space>
          </div>
        </article>
      ))}
    </section>
  );
}
