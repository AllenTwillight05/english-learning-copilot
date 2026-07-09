import { Button, Space, Typography } from "antd";

const { Title, Text } = Typography;

export function ChineseToEnglishQuestion({ question, selectedAnswer, answered, onAnswer }) {
  return (
    <div className="practice-question">
      <Title level={3} className="question-translation">{question.briefTranslation}</Title>
      <Text type="secondary">选择与中文释义对应的英文单词。</Text>
      <Space direction="vertical" size={12} className="full-width">
        {question.englishOptions.map((option) => (
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
