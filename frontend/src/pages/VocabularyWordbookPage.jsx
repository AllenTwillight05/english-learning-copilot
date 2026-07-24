import { useCallback, useEffect, useMemo, useState } from "react";
import { ArrowLeftOutlined, LoginOutlined, ReadOutlined } from "@ant-design/icons";
import { App, Button, Empty, Flex, Result, Typography } from "antd";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { VocabularyWordbookItem } from "../components/Vocabulary/VocabularyWordbookItem";
import { VocabularyWordbookTabs } from "../components/Vocabulary/VocabularyWordbookTabs";
import { AsyncPage } from "../components/common/AsyncPage";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Title, Text } = Typography;

export function VocabularyWordbookPage() {
  const { message } = App.useApp();
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const { vocabulary } = useAppServices();
  const loader = useCallback(() => {
    if (auth.loading || !auth.isAuthenticated) {
      return Promise.resolve(null);
    }

    return vocabulary.getVocabularyWordbookWords();
  }, [auth.isAuthenticated, auth.loading, vocabulary]);
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

  async function handleToggleFavorite(wordId) {
    const currentWord = words.find((word) => word.id === wordId);

    if (!currentWord) {
      return;
    }

    const previousFavorited = currentWord.favorited;
    const nextFavorited = !previousFavorited;

    setWords((current) =>
      current.map((word) =>
        word.id === wordId ? { ...word, favorited: nextFavorited } : word
      )
    );

    try {
      const result = await vocabulary.toggleVocabularyFavorite({ vocabularyId: wordId });
      const updatedFavorited = result?.favorited ?? nextFavorited;
      setWords((current) =>
        current.map((word) =>
          word.id === wordId ? { ...word, favorited: updatedFavorited } : word
        )
      );
      message.success(updatedFavorited ? "已收藏单词" : "已取消收藏");
    } catch (error) {
      setWords((current) =>
        current.map((word) =>
          word.id === wordId ? { ...word, favorited: previousFavorited } : word
        )
      );
      message.error(error?.status === 401 ? "请先登录后收藏单词" : "收藏状态更新失败");
    }
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

  if (auth.loading) {
    return <AsyncPage loading error={null} />;
  }

  if (!auth.isAuthenticated) {
    return (
      <div className="page-stack">
        <section className="glass-panel profile-empty-state">
          <Result
            icon={<ReadOutlined />}
            title="请先登录"
            extra={
              <Button
                type="primary"
                icon={<LoginOutlined />}
                onClick={() => navigate("/login", { state: { from: location } })}
              >
                请先登录
              </Button>
            }
          />
        </section>
      </div>
    );
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
