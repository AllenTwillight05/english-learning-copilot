import { useCallback } from "react";
import { BookOutlined, FireOutlined, RocketOutlined, StarOutlined } from "@ant-design/icons";
import { Space, Tag, Typography } from "antd";
import { MemoryRetentionPanel } from "../components/Vocabulary/MemoryRetentionPanel";
import { VocabularyLevelCard } from "../components/Vocabulary/VocabularyLevelCard";
import { VocabularyWordbookButton } from "../components/Vocabulary/VocabularyWordbookButton";
import { AsyncPage } from "../components/common/AsyncPage";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Title, Paragraph } = Typography;

const practiceLevels = [
  {
    key: "starter",
    title: "入门",
    description: "高频基础词，适合建立每日练习节奏。",
    icon: <StarOutlined />,
    tag: "Beginner"
  },
  {
    key: "basic",
    title: "基础",
    description: "覆盖常用表达，巩固听说读写中的核心词汇。",
    icon: <BookOutlined />,
    tag: "A1-A2"
  },
  {
    key: "intermediate",
    title: "中级",
    description: "强化语境辨析，提升阅读和表达准确度。",
    icon: <FireOutlined />,
    tag: "B1"
  },
  {
    key: "advanced",
    title: "进阶",
    description: "面向复杂表达，训练更精准的词义判断。",
    icon: <RocketOutlined />,
    tag: "B2+"
  }
];

export function VocabularyPage() {
  const { vocabulary } = useAppServices();
  const loader = useCallback(() => vocabulary.getVocabularyMemory(), [vocabulary]);
  const { data, loading, error } = useAsyncData(loader, [loader]);

  return (
    <AsyncPage loading={loading} error={error}>
      {data ? (
        <div className="page-stack">
          <section className="vocabulary-hero glass-panel">
            <div className="vocabulary-hero__copy">
              <Space align="center" wrap>
                <Tag bordered={false} className="soft-tag soft-tag--dark">
                  Vocabulary Practice
                </Tag>
              </Space>
              <Title>词汇练习</Title>
              <Paragraph>
                按难度进入一组词汇训练。
              </Paragraph>
            </div>

            <MemoryRetentionPanel overview={data} />
          </section>

          <section className="vocabulary-action-grid">
            {practiceLevels.map((level) => (
              <VocabularyLevelCard
                description={level.description}
                icon={level.icon}
                key={level.key}
                tag={level.tag}
                title={level.title}
              />
            ))}
            <VocabularyWordbookButton />
          </section>
        </div>
      ) : null}
    </AsyncPage>
  );
}
