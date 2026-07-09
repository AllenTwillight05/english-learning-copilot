import { SoundOutlined, StarFilled, StarOutlined } from "@ant-design/icons";
import { Button, Flex, Tag, Typography } from "antd";

const { Paragraph, Text, Title } = Typography;

export function VocabularyWordbookItem({ word, onToggleFavorite, onPlayAudio }) {
  return (
    <article className="wordbook-item">
      <Flex justify="space-between" align="start" gap={16} wrap>
        <div>
          <Title level={4}>{word.word}</Title>
          <Text type="secondary">{word.phonetic}</Text>
        </div>
        <Flex align="center" gap={10}>
          <Tag bordered={false} className="soft-tag">
            {word.tag}
          </Tag>
          <Button
            aria-label={word.favorited ? "取消收藏" : "收藏单词"}
            className={word.favorited ? "favorite-button favorite-button--active" : "favorite-button"}
            htmlType="button"
            icon={word.favorited ? <StarFilled /> : <StarOutlined />}
            onClick={() => onToggleFavorite(word.id)}
            shape="circle"
          />
        </Flex>
      </Flex>
      <Paragraph className="wordbook-item__translation">{word.briefTranslation}</Paragraph>
      <Paragraph className="wordbook-item__definition">{word.definition}</Paragraph>
      <Button
        htmlType="button"
        icon={<SoundOutlined />}
        onClick={() => onPlayAudio(word)}
      >
        播放发音
      </Button>
    </article>
  );
}
