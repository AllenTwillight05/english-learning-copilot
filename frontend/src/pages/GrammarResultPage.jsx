import { BookOutlined, ReloadOutlined } from "@ant-design/icons";
import { Button, Flex, Typography } from "antd";
import { useLocation, useNavigate } from "react-router-dom";
import { GrammarRatingDonut } from "../components/Grammar/GrammarRatingDonut";

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

export function GrammarResultPage() {
  const navigate = useNavigate();
  const { state } = useLocation();
  const summary = state?.summary ?? emptySummary;
  const category = state?.category ?? "";

  return (
    <div className="page-stack">
      <section className="glass-panel result-hero">
        <div>
          <Text className="eyebrow">Grammar Result</Text>
          <Title>练习总结</Title>
          <Text type="secondary">本次语法练习完成情况与掌握程度评分分布。</Text>
        </div>
        <Flex gap={12} wrap>
          <Button
            htmlType="button"
            icon={<ReloadOutlined />}
            onClick={() => navigate(`/grammar/practice/${encodeURIComponent(category)}`)}
          >
            再练一次
          </Button>
          <Button
            htmlType="button"
            icon={<BookOutlined />}
            onClick={() => navigate("/grammar")}
            type="primary"
          >
            返回语法主页
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
        <Title level={3}>掌握程度评分占比</Title>
        <GrammarRatingDonut ratings={summary.ratings} />
      </section>
    </div>
  );
}
