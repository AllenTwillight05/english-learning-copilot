import { useCallback, useEffect, useMemo, useState } from "react";
import { ArrowLeftOutlined } from "@ant-design/icons";
import { Button, Empty, Flex, Typography } from "antd";
import { useNavigate } from "react-router-dom";
import { VocabularyWordbookItem } from "../components/Vocabulary/VocabularyWordbookItem";
import { VocabularyWordbookTabs } from "../components/Vocabulary/VocabularyWordbookTabs";
import { AsyncPage } from "../components/common/AsyncPage";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Title, Text } = Typography;

export function VocabularyWordbookPage() {
  const navigate = useNavigate();
  const { vocabulary } = useAppServices();
  const loader = useCallback(() => vocabulary.getVocabularyWordbookWords(), [vocabulary]);
  const { data, loading, error } = useAsyncData(loader, [loader]);
  const [activeTab, setActiveTab] = useState("learned");
  const [words, setWords] = useState([]);

  useEffect(() => {
    if (data) {
      setWords(data);
    }
  }, [data]);

  const counts = useMemo(
    () => ({
      learned: words.length,
      favorited: words.filter((word) => word.favorited).length
    }),
    [words]
  );

  const visibleWords = activeTab === "favorited"
    ? words.filter((word) => word.favorited)
    : words;

  function handleToggleFavorite(wordId) {
    setWords((current) =>
      current.map((word) =>
        word.id === wordId ? { ...word, favorited: !word.favorited } : word
      )
    );
  }

  function handlePlayAudio(word) {
    if (word.us_audio) {
      new Audio(word.us_audio).play();
      return;
    }

    const utterance = new SpeechSynthesisUtterance(word.word);
    utterance.lang = "en-US";
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(utterance);
  }

  return (
    <AsyncPage loading={loading} error={error}>
      {data ? (
        <div className="page-stack">
          <section className="glass-panel wordbook-hero">
            <div>
              <Text className="eyebrow">Wordbook</Text>
              <Title>单词本</Title>
              <Text type="secondary">查看已学单词和收藏单词。</Text>
            </div>
            <Button
              htmlType="button"
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate("/vocabulary")}
            >
              返回词汇主页
            </Button>
          </section>

          <section className="glass-panel">
            <VocabularyWordbookTabs
              activeTab={activeTab}
              counts={counts}
              onChange={setActiveTab}
            />
          </section>

          <section className="wordbook-list">
            {visibleWords.length ? (
              visibleWords.map((word) => (
                <VocabularyWordbookItem
                  key={word.id}
                  onPlayAudio={handlePlayAudio}
                  onToggleFavorite={handleToggleFavorite}
                  word={word}
                />
              ))
            ) : (
              <div className="glass-panel">
                <Empty description="暂无收藏单词" />
              </div>
            )}
          </section>
        </div>
      ) : null}
    </AsyncPage>
  );
}
