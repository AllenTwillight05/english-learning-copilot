import { ArrowRightOutlined, CoffeeOutlined, ReadOutlined } from "@ant-design/icons";
import { Button, Space, Tag, Typography } from "antd";
import { useNavigate } from "react-router-dom";
import { PageSectionHeader } from "../components/common/PageSectionHeader";

const { Paragraph, Title } = Typography;

const entryCards = [
  {
    id: "daily",
    title: "日常口语",
    description: "12 个 G 系列生活、旅行和职场场景。",
    path: "/speaking/daily",
    icon: <CoffeeOutlined />,
    tone: "gold",
    tags: ["G-01 到 G-12", "情景对话"]
  },
  {
    id: "ielts",
    title: "雅思口语",
    description: "Part 1、Part 2、Part 3 和完整 Mock Test。",
    path: "/speaking/ielts",
    icon: <ReadOutlined />,
    tone: "blue",
    tags: ["IELTS", "Topic / Cue Card"]
  }
];

export function SpeakingPage() {
  const navigate = useNavigate();

  return (
    <div className="page-stack speaking-home">
      <section className="glass-panel">
        <PageSectionHeader
          eyebrow=""
          title="口语练习"
          description="先选择练习方向，再进入具体场景或雅思题型。"
        />
      </section>

      <section className="feature-grid speaking-entry-grid">
        {entryCards.map((entry) => (
          <article
            className="scenario-card scenario-card--button speaking-entry-card"
            key={entry.id}
            onClick={() => navigate(entry.path)}
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                navigate(entry.path);
              }
            }}
            role="button"
            tabIndex={0}
          >
            <div className={`scenario-card__tone scenario-card__tone--${entry.tone}`} />
            <div className="scenario-card__body scenario-card__body--spacious">
              <div className="speaking-entry-card__icon">{entry.icon}</div>
              <div className="scenario-card__header">
                <Title level={3} className="scenario-card__title">
                  {entry.title}
                </Title>
                <Paragraph>{entry.description}</Paragraph>
              </div>
              <Space size={[6, 6]} wrap>
                {entry.tags.map((tag) => (
                  <Tag bordered={false} className="soft-tag" key={tag}>
                    {tag}
                  </Tag>
                ))}
              </Space>
              <Button type="primary" icon={<ArrowRightOutlined />}>
                进入
              </Button>
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}
