import { Button, Space, Tag, Typography } from "antd";

const { Title, Text } = Typography;

const optionLetters = ["A", "B", "C", "D", "E"];

export function GrammarQuestionCard({ question, answered, selectedAnswer, onAnswer }) {
  return (
    <section className="practice-question">
      <Space align="center" wrap>
        <Tag bordered={false} className="soft-tag">
          {question.grammar_category}
        </Tag>
      </Space>
      <Title level={3}>{question.question_text}</Title>
      <Space direction="vertical" size={10} className="full-width">
        {question.options.map((option, index) => {
          const optionLetter = optionLetters[index];
          const selected = selectedAnswer === optionLetter;

          return (
            <Button
              className="answer-option"
              disabled={answered}
              htmlType="button"
              key={`${optionLetter}-${option}`}
              onClick={() => onAnswer(optionLetter)}
              type={selected ? "primary" : "default"}
            >
              <Text strong>{optionLetter}.</Text>
              <span>{option}</span>
            </Button>
          );
        })}
      </Space>
    </section>
  );
}
