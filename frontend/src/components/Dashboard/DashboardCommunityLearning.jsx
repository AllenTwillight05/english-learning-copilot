import { useState } from "react";
import { TrophyFilled } from "@ant-design/icons";
import { Segmented, Typography } from "antd";

const { Title, Text } = Typography;

const categoryOptions = [
  { label: "口语", value: "speaking" },
  { label: "词汇", value: "vocabulary" },
  { label: "语法", value: "grammar" }
];

function getItemKey(item, category) {
  if (category === "vocabulary") {
    return item.word;
  }

  return item.topic;
}

function renderLearningItem(item, category) {
  if (category === "vocabulary") {
    return (
      <div className="dashboard-community__copy">
        <Text strong>{item.word}</Text>
        <span className="helper-text">{item.briefTranslation}</span>
      </div>
    );
  }

  if (category === "grammar") {
    return (
      <div className="dashboard-community__copy">
        <Text strong>{item.topic}</Text>
      </div>
    );
  }

  return (
    <div className="dashboard-community__copy">
      <Text strong>{item.topic}</Text>
      <span className="helper-text">{item.description}</span>
    </div>
  );
}

export function DashboardCommunityLearning({ learningTrends }) {
  const [activeCategory, setActiveCategory] = useState("speaking");
  const activeItems = learningTrends[activeCategory] ?? [];

  return (
    <section className="glass-panel dashboard-community">
      <div className="dashboard-community__header">
        <Title level={4}>大家都在学</Title>
        <Segmented
          options={categoryOptions}
          value={activeCategory}
          onChange={setActiveCategory}
        />
      </div>

      <div className="dashboard-community__list">
        {activeItems.map((item, index) => (
          <article
            className="dashboard-community__item"
            key={getItemKey(item, activeCategory)}
          >
            <span
              className={`dashboard-community__rank dashboard-community__rank--${index + 1}`}
            >
              {index < 3 ? <TrophyFilled /> : index + 1}
            </span>
            {renderLearningItem(item, activeCategory)}
          </article>
        ))}
      </div>
    </section>
  );
}
