import { Segmented } from "antd";

export function VocabularyWordbookTabs({ activeTab, counts, onChange }) {
  return (
    <Segmented
      className="wordbook-tabs"
      onChange={onChange}
      options={[
        { label: `学过的单词 ${counts.learned}`, value: "learned" },
        { label: `收藏的单词 ${counts.favorited}`, value: "favorited" }
      ]}
      value={activeTab}
    />
  );
}
