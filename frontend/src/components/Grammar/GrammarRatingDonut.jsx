import { Typography } from "antd";

const { Text, Title } = Typography;

const ratingSegments = [
  { key: "again", label: "重来", color: "#b42318" },
  { key: "hard", label: "困难", color: "#c25f00" },
  { key: "good", label: "良好", color: "#0f766e" },
  { key: "easy", label: "简单", color: "#2356c4" }
];

export function GrammarRatingDonut({ ratings }) {
  const total = ratingSegments.reduce((sum, segment) => sum + (ratings[segment.label] ?? 0), 0);
  let currentPercent = 0;
  const gradientStops = ratingSegments.map((segment) => {
    const value = ratings[segment.label] ?? 0;
    const percent = total > 0 ? (value / total) * 100 : 0;
    const start = currentPercent;
    currentPercent += percent;
    return `${segment.color} ${start}% ${currentPercent}%`;
  });

  const donutBackground =
    total > 0 ? `conic-gradient(${gradientStops.join(", ")})` : "rgba(17, 17, 17, 0.08)";

  return (
    <div className="rating-donut-panel">
      <div className="rating-donut" style={{ background: donutBackground }}>
        <div className="rating-donut__center">
          <Title level={3}>{total}</Title>
          <Text type="secondary">次评分</Text>
        </div>
      </div>
      <div className="rating-donut__legend">
        {ratingSegments.map((segment) => {
          const value = ratings[segment.label] ?? 0;
          const percent = total > 0 ? Math.round((value / total) * 100) : 0;

          return (
            <div className="rating-donut__legend-row" key={segment.key}>
              <span className="rating-donut__dot" style={{ background: segment.color }} />
              <Text>{segment.label}</Text>
              <Text type="secondary">
                {value} 次 / {percent}%
              </Text>
            </div>
          );
        })}
      </div>
    </div>
  );
}
