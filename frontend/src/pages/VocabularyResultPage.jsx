import { BookOutlined, ReloadOutlined } from "@ant-design/icons";
import { Button, Flex, Typography } from "antd";
import { useLocation, useNavigate } from "react-router-dom";
import { VocabularyRatingDonut } from "../components/Vocabulary/VocabularyRatingDonut";

const { Title, Text } = Typography;

const emptySummary = {
  total: 0,
  correct: 0,
  wrong: 0,
  ratings: {
    重来: 0,
    困难: 0,
    良好: 0,
    简单: 0
  }
};

export function VocabularyResultPage() {
  const navigate = useNavigate();
  const { state } = useLocation();
  const summary = state?.summary ?? emptySummary;
  const level = state?.level ?? "starter";

  return (
    <div className="page-stack">
      <section className="glass-panel result-hero">
        <div>
          <Text className="eyebrow">Practice Result</Text>
          <Title>练习总结</Title>
          <Text type="secondary">本次词汇练习完成情况与记忆评分分布。</Text>
        </div>
        <Flex gap={12} wrap>
          <Button
            htmlType="button"
            icon={<ReloadOutlined />}
            onClick={() => navigate(`/vocabulary/practice/${level}`)}
          >
            再练一次
          </Button>
          <Button
            htmlType="button"
            icon={<BookOutlined />}
            onClick={() => navigate("/vocabulary")}
            type="primary"
          >
            返回词汇主页
          </Button>
        </Flex>
      </section>

      <section className="result-grid">
        <div className="glass-panel result-stat">
          <Text type="secondary">练习总题数</Text>
          <Title level={2}>{summary.total}</Title>
        </div>
        <div className="glass-panel result-stat result-stat--correct">
          <Text type="secondary">答对</Text>
          <Title level={2}>{summary.correct}</Title>
        </div>
        <div className="glass-panel result-stat result-stat--wrong">
          <Text type="secondary">答错</Text>
          <Title level={2}>{summary.wrong}</Title>
        </div>
      </section>

      <section className="glass-panel">
        <Title level={3}>记忆评分占比</Title>
        <VocabularyRatingDonut ratings={summary.ratings} />
      </section>
    </div>
  );
}
