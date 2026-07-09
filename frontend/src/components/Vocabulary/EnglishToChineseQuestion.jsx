import { Button, Space, Typography } from "antd";

const { Title, Text } = Typography;

export function EnglishToChineseQuestion({ question, selectedAnswer, answered, onAnswer }) {
  return (
    <div className="practice-question">
      <Title level={2}>{question.word}</Title>
      <Text type="secondary">选择最符合这个英文单词的中文释义。</Text>
      <Space direction="vertical" size={12} className="full-width">
        {question.chineseOptions.map((option) => (
          <Button
            className="answer-option"
            disabled={answered}
            htmlType="button"
            key={option}
            onClick={() => onAnswer(option)}
            type={selectedAnswer === option ? "primary" : "default"}
          >
            {option}
          </Button>
        ))}
      </Space>
    </div>
  );
}
