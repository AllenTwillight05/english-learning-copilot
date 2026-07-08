import { TrophyOutlined } from "@ant-design/icons";
import { Button } from "antd";

export function VocabularyWordbookButton() {
  return (
    <Button className="wordbook-button" type="primary" icon={<TrophyOutlined />}>
      单词本
    </Button>
  );
}
