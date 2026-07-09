import { useState } from "react";
import { SoundOutlined, StarFilled, StarOutlined } from "@ant-design/icons";
import { App, Button, Descriptions, Flex, Space, Tag, Typography } from "antd";

const { Title, Paragraph, Text } = Typography;

const ratingOptions = [
  { key: "again", label: "重来", shortcut: "1" },
  { key: "hard", label: "困难", shortcut: "2" },
  { key: "good", label: "良好", shortcut: "3" },
  { key: "easy", label: "简单", shortcut: "4" }
];

export function VocabularyWordCard({ word, selectedRating, onRate }) {
  const { message } = App.useApp();
  const [favorited, setFavorited] = useState(false);

  function handleToggleFavorite() {
    setFavorited((current) => {
      const next = !current;
      message.success(next ? "已收藏单词" : "已取消收藏");
      return next;
    });
  }

  function handlePlayAudio(audioUrl) {
    if (audioUrl) {
      new Audio(audioUrl).play();
      return;
    }

    const utterance = new SpeechSynthesisUtterance(word.word);
    utterance.lang = "en-US";
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(utterance);
  }

  return (
    <article className="vocabulary-word-card">
      <Flex justify="space-between" align="start" gap={16} wrap>
        <div>
          <Title level={3}>{word.word}</Title>
          <Text type="secondary">{word.phonetic}</Text>
          <Space wrap size={8} className="word-audio-actions">
            <Button
              htmlType="button"
              icon={<SoundOutlined />}
              onClick={() => handlePlayAudio(word.uk_audio)}
              size="small"
            >
              英音
            </Button>
            <Button
              htmlType="button"
              icon={<SoundOutlined />}
              onClick={() => handlePlayAudio(word.us_audio)}
              size="small"
            >
              美音
            </Button>
          </Space>
        </div>
        <Flex align="center" gap={10}>
          <Tag bordered={false} className="soft-tag">
            {word.tag}
          </Tag>
          <Button
            aria-label={favorited ? "取消收藏" : "收藏单词"}
            className={favorited ? "favorite-button favorite-button--active" : "favorite-button"}
            htmlType="button"
            icon={favorited ? <StarFilled /> : <StarOutlined />}
            onClick={handleToggleFavorite}
            shape="circle"
          />
        </Flex>
      </Flex>
      <div className="word-card-section">
        <Text strong>中文释义</Text>
        <Paragraph className="word-multiline">{word.translation}</Paragraph>
      </div>
      <div className="word-card-section">
        <Text strong>英文释义</Text>
        <Paragraph className="word-multiline word-definition">{word.definition}</Paragraph>
      </div>
      <Descriptions className="word-meta" column={{ xs: 1, sm: 2, md: 4 }} size="small">
        <Descriptions.Item label="Collins">{word.collins || "-"}</Descriptions.Item>
        <Descriptions.Item label="Oxford">{word.oxford || "-"}</Descriptions.Item>
        <Descriptions.Item label="BNC">{word.bnc || "-"}</Descriptions.Item>
        <Descriptions.Item label="FRQ">{word.frq || "-"}</Descriptions.Item>
      </Descriptions>
      {word.exchange ? (
        <div className="word-card-section">
          <Text strong>词形变化</Text>
          <Paragraph className="word-exchange">{word.exchange}</Paragraph>
        </div>
      ) : null}
      <div className="rating-grid">
        {ratingOptions.map((rating) => (
          <Button
            className={`rating-button rating-button--${rating.key}${
              selectedRating === rating.label ? " rating-button--selected" : ""
            }`}
            htmlType="button"
            key={rating.key}
            onClick={() => onRate(rating.label)}
          >
            <span>{rating.label}</span>
            <Text className="rating-button__shortcut">按 {rating.shortcut}</Text>
          </Button>
        ))}
      </div>
    </article>
  );
}
