import { useCallback } from "react";
import {
  BookOutlined,
  FireOutlined,
  LoginOutlined,
  HistoryOutlined,
  RocketOutlined,
  StarOutlined,
  UserOutlined
} from "@ant-design/icons";
import { Button, Result, Space, Tag, Typography } from "antd";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { MemoryRetentionPanel } from "../components/Vocabulary/MemoryRetentionPanel";
import { PracticeProgressRing } from "../components/Vocabulary/PracticeProgressRing";
import { VocabularyLevelCard } from "../components/Vocabulary/VocabularyLevelCard";
import { VocabularyWordbookButton } from "../components/Vocabulary/VocabularyWordbookButton";
import { AsyncPage } from "../components/common/AsyncPage";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Title, Paragraph } = Typography;

function syncTodayPendingStat(memory, practiceProgress) {
  const stats = memory.stats.map((stat, index) => {
    if (index !== memory.stats.length - 1) {
      return stat;
    }

    return {
      ...stat,
      value: `${practiceProgress.remaining} 词`
    };
  });

  return {
    ...memory,
    stats
  };
}

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
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const { vocabulary } = useAppServices();
  const loader = useCallback(async () => {
    if (auth.loading || !auth.isAuthenticated) {
      return null;
    }

    const [memory, practiceProgress] = await Promise.all([
      vocabulary.getVocabularyMemory(),
      vocabulary.getVocabularyPracticeProgress()
    ]);

    return {
      memory: syncTodayPendingStat(memory, practiceProgress),
      practiceProgress
    };
  }, [auth.isAuthenticated, auth.loading, vocabulary]);
  const { data, loading, error } = useAsyncData(loader, [loader]);

  if (auth.loading) {
    return <AsyncPage loading error={null} />;
  }

  if (!auth.isAuthenticated) {
    return (
      <div className="page-stack">
        <section className="glass-panel profile-empty-state">
          <Result
            icon={<UserOutlined />}
            title="请先登录"
            extra={
              <Button
                type="primary"
                icon={<LoginOutlined />}
                onClick={() => navigate("/login", { state: { from: location } })}
              >
                请先登录
              </Button>
            }
          />
        </section>
      </div>
    );
  }

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

            <PracticeProgressRing
              completed={data.practiceProgress.completed}
              remaining={data.practiceProgress.remaining}
              total={data.practiceProgress.total}
            />

            <MemoryRetentionPanel overview={data.memory} />
          </section>

          <section className="vocabulary-action-grid">
            {practiceLevels.map((level) => (
              <VocabularyLevelCard
                description={level.description}
                icon={level.icon}
                key={level.key}
                onClick={() => navigate(`/vocabulary/practice/${level.key}`)}
                tag={level.tag}
                title={level.title}
              />
            ))}
            <VocabularyLevelCard
              description="复习系统安排的到期词汇，巩固记忆并更新掌握情况。"
              icon={<HistoryOutlined />}
              onClick={() => navigate("/vocabulary/practice/review")}
              tag="Review"
              title="复习"
            />
            <VocabularyWordbookButton onClick={() => navigate("/vocabulary/wordbook")} />
          </section>
        </div>
      ) : null}
    </AsyncPage>
  );
}
