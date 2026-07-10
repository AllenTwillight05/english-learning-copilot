import { Segmented } from "antd";

export function GrammarNotebookTabs({ activeTab, counts, onChange }) {
  return (
    <Segmented
      className="wordbook-tabs"
      onChange={onChange}
      options={[
        { label: `错题 ${counts.wrong}`, value: "wrong" },
        { label: `收藏 ${counts.favorited}`, value: "favorited" }
      ]}
      value={activeTab}
    />
  );
}
