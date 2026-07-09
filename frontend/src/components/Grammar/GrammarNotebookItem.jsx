import { StarFilled, StarOutlined } from "@ant-design/icons";
import { Button, Flex, Tag, Typography } from "antd";

const { Paragraph, Text, Title } = Typography;
const optionLetters = ["A", "B", "C", "D", "E"];

export function GrammarNotebookItem({ question, onToggleFavorite }) {
  return (
    <article className="wordbook-item">
      <Flex justify="space-between" align="start" gap={16} wrap>
        <div>
          <Title level={4}>{question.question_text}</Title>
          <Text type="secondary">{question.grammar_category}</Text>
        </div>
        <Flex align="center" gap={10}>
          <Tag bordered={false} className="soft-tag">
            答案 {question.answer}
          </Tag>
          <Button
            aria-label={question.favorited ? "取消收藏" : "收藏语法题"}
            className={question.favorited ? "favorite-button favorite-button--active" : "favorite-button"}
            htmlType="button"
            icon={question.favorited ? <StarFilled /> : <StarOutlined />}
            onClick={() => onToggleFavorite(question.id)}
            shape="circle"
          />
        </Flex>
      </Flex>

      <div className="grammar-notebook-options">
        {question.options.map((option, index) => {
          const optionLetter = optionLetters[index];
          const isCorrect = optionLetter === question.answer;

          return (
            <div
              className={isCorrect ? "grammar-notebook-option grammar-notebook-option--correct" : "grammar-notebook-option"}
              key={`${optionLetter}-${option}`}
            >
              <Text strong>{optionLetter}.</Text>
              <Text>{option}</Text>
            </div>
          );
        })}
      </div>

      <Paragraph className="wordbook-item__definition">{question.explanation}</Paragraph>
    </article>
  );
}
