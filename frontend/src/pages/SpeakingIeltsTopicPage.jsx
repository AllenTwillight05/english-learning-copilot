import { ArrowLeftOutlined, ArrowRightOutlined } from "@ant-design/icons";
import { Button, Tag, Typography } from "antd";
import { Navigate, useNavigate, useParams } from "react-router-dom";
import { PageSectionHeader } from "../components/common/PageSectionHeader";
import { IELTS_SCENARIOS } from "../services/speakingCatalog";

const { Paragraph, Title } = Typography;

const partKeyByRoute = {
  part1: "part1",
  part2: "part2",
  part3: "part3"
};

export function SpeakingIeltsTopicPage() {
  const navigate = useNavigate();
  const { partId } = useParams();
  const partKey = partKeyByRoute[partId];
  const part = partKey ? IELTS_SCENARIOS[partKey] : null;

  if (!part) {
    return <Navigate to="/speaking/ielts" replace />;
  }

  const itemLabel = partKey === "part2" ? "Cue Card" : "Topic";

  return (
    <div className="page-stack">
      <section className="glass-panel">
        <PageSectionHeader
          eyebrow="IELTS Speaking"
          title={`雅思口语 ${part.title}`}
          description={partKey === "part2" ? "选择一个 cue card 进入练习。" : "选择一个话题进入练习。"}
          extra={
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate("/speaking/ielts")}>
              返回
            </Button>
          }
        />
      </section>

      <section className="feature-grid">
        {part.topics.map((topic, index) => (
          <article
            className="scenario-card scenario-card--button ielts-topic-card"
            key={topic}
            onClick={() =>
              navigate(
                `/speaking/${part.scenarioId}/conversation?topic=${encodeURIComponent(topic)}`,
                {
                  state: {
                    selectedTopic: topic,
                    speakingBackPath: `/speaking/ielts/${partId}`
                  }
                }
              )
            }
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                navigate(
                  `/speaking/${part.scenarioId}/conversation?topic=${encodeURIComponent(topic)}`,
                  {
                    state: {
                      selectedTopic: topic,
                      speakingBackPath: `/speaking/ielts/${partId}`
                    }
                  }
                );
              }
            }}
            role="button"
            tabIndex={0}
          >
            <div className={`scenario-card__tone scenario-card__tone--${part.tone}`} />
            <div className="scenario-card__body scenario-card__body--spacious">
              <Tag bordered={false} className="soft-tag soft-tag--dark">
                {itemLabel} {index + 1}
              </Tag>
              <Title level={3} className="scenario-card__title">
                {topic}
              </Title>
              <Paragraph>{part.title} · {part.subtitle}</Paragraph>
              <Button type="primary" icon={<ArrowRightOutlined />}>
                开始
              </Button>
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}
