import { Typography } from "antd";
import { PageSectionHeader } from "../common/PageSectionHeader";

const { Title, Text } = Typography;

function formatFeedbackTime(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function scoreValue(value) {
  return typeof value === "number" ? value : "-";
}

export function FeedbackPanel({ feedback }) {
  const metrics = [
    { key: "totalScore", label: "总评分", value: scoreValue(feedback.totalScore) },
    { key: "pronunciation", label: "发音准确性", value: scoreValue(feedback.pronunciation) },
    { key: "fluency", label: "流畅度", value: scoreValue(feedback.fluency) },
    { key: "integrity", label: "完整度", value: scoreValue(feedback.integrity) }
  ];
  const subtitle = [feedback.scenarioTitle, formatFeedbackTime(feedback.completedAt)]
    .filter(Boolean)
    .join(" / ");

  return (
    <section className="glass-panel">
      <PageSectionHeader
        eyebrow="Feedback"
        title="最近一次口语反馈"
        description={subtitle}
      />
      <div className="metrics-grid profile-feedback-metrics">
        {metrics.map((metric) => (
          <div className="metric-chip" key={metric.key}>
            <Text type="secondary">{metric.label}</Text>
            <Title level={3}>{metric.value}</Title>
          </div>
        ))}
      </div>
      <div style={{ display: "grid", gap: 6, marginTop: 12 }}>
        <Text strong className="panel-title">问题句子</Text>
        <Text>{feedback.issueSentence || "无"}</Text>
      </div>
    </section>
  );
}
