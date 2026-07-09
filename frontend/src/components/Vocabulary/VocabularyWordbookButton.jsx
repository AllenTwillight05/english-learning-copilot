import { TrophyOutlined } from "@ant-design/icons";
import { Button } from "antd";

export function VocabularyWordbookButton({ onClick }) {
  return (
    <Button className="wordbook-button" type="primary" icon={<TrophyOutlined />} onClick={onClick}>
      单词本
    </Button>
  );
}
