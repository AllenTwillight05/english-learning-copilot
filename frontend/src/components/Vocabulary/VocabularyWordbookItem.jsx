import { SoundOutlined, StarFilled, StarOutlined } from "@ant-design/icons";
import { Button, Flex, Tag, Typography } from "antd";
import { VocabularyDefinitionText } from "./VocabularyDefinitionText";

const { Text, Title } = Typography;

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
      <div className="word-card-section">
        <Text strong>中文释义</Text>
        <VocabularyDefinitionText
          className="wordbook-item__translation"
          value={word.translation || word.briefTranslation}
        />
      </div>
      <div className="word-card-section">
        <Text strong>英文释义</Text>
        <VocabularyDefinitionText className="wordbook-item__definition" value={word.definition} />
      </div>
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
