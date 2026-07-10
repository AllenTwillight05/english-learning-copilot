import { useState } from "react";
import { StarFilled, StarOutlined } from "@ant-design/icons";
import { App, Button, Flex, Space, Tag, Typography } from "antd";

const { Paragraph, Text, Title } = Typography;

const ratingOptions = [
  { key: "again", label: "重来", shortcut: "1" },
  { key: "hard", label: "困难", shortcut: "2" },
  { key: "good", label: "良好", shortcut: "3" },
  { key: "easy", label: "简单", shortcut: "4" }
];

export function GrammarExplanationCard({ correctAnswer, explanation, isCorrect, onRate }) {
  const { message } = App.useApp();
  const [favorited, setFavorited] = useState(false);

  function handleToggleFavorite() {
    setFavorited((current) => {
      const next = !current;
      message.success(next ? "已收藏语法题" : "已取消收藏");
      return next;
    });
  }

  return (
    <article className="vocabulary-word-card grammar-explanation-card">
      <Flex justify="space-between" align="center" gap={12} wrap>
        <div>
          <Text className="eyebrow">Answer Review</Text>
          <Title level={3}>{isCorrect ? "回答正确" : "回答错误"}</Title>
        </div>
        <Flex align="center" gap={10}>
          <Tag bordered={false} className={isCorrect ? "soft-tag" : "soft-tag soft-tag--dark"}>
            正确答案 {correctAnswer.letter}
          </Tag>
          <Button
            aria-label={favorited ? "取消收藏" : "收藏语法题"}
            className={favorited ? "favorite-button favorite-button--active" : "favorite-button"}
            htmlType="button"
            icon={favorited ? <StarFilled /> : <StarOutlined />}
            onClick={handleToggleFavorite}
            shape="circle"
          />
        </Flex>
      </Flex>

      <div className="word-card-section">
        <Text strong>答案</Text>
        <Paragraph className="word-multiline">
          {correctAnswer.letter}. {correctAnswer.text}
        </Paragraph>
      </div>

      <div className="word-card-section">
        <Text strong>解析</Text>
        <Paragraph className="word-multiline word-definition">{explanation}</Paragraph>
      </div>

      <Space direction="vertical" size={10} className="full-width">
        <Text strong>本题掌握程度</Text>
        <div className="rating-grid">
          {ratingOptions.map((rating) => (
            <Button
              className={`rating-button rating-button--${rating.key}`}
              htmlType="button"
              key={rating.key}
              onClick={() => onRate(rating.label)}
            >
              <span>{rating.label}</span>
              <Text className="rating-button__shortcut">按 {rating.shortcut}</Text>
            </Button>
          ))}
        </div>
      </Space>
    </article>
  );
}
