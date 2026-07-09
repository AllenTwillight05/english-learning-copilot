import { SoundOutlined } from "@ant-design/icons";
import { Button, Space, Typography } from "antd";

const { Title, Text } = Typography;

export function AudioToChineseQuestion({
  question,
  selectedAnswer,
  answered,
  onAnswer,
  onPlayAudio
}) {
  return (
    <div className="practice-question">
      <Title level={3}>听音辨义</Title>
      <Button
        className="audio-prompt-button"
        htmlType="button"
        icon={<SoundOutlined />}
        onClick={onPlayAudio}
        size="large"
        type="primary"
      >
        播放单词发音
      </Button>
      <Text type="secondary">听发音后，选择对应的中文释义。</Text>
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
