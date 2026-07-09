import { useCallback, useEffect, useMemo, useState } from "react";
import { ArrowLeftOutlined } from "@ant-design/icons";
import { Button, Empty, Flex, Typography } from "antd";
import { useNavigate } from "react-router-dom";
import { GrammarNotebookItem } from "../components/Grammar/GrammarNotebookItem";
import { GrammarNotebookTabs } from "../components/Grammar/GrammarNotebookTabs";
import { AsyncPage } from "../components/common/AsyncPage";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Title, Text } = Typography;

export function GrammarNotebookPage() {
  const navigate = useNavigate();
  const { grammar } = useAppServices();
  const loader = useCallback(() => grammar.getNotebookQuestions(), [grammar]);
  const { data, loading, error } = useAsyncData(loader, [loader]);
  const [activeTab, setActiveTab] = useState("wrong");
  const [questions, setQuestions] = useState([]);

  useEffect(() => {
    if (data) {
      setQuestions(data);
    }
  }, [data]);

  const counts = useMemo(
    () => ({
      wrong: questions.filter((question) => question.wrong).length,
      favorited: questions.filter((question) => question.favorited).length
    }),
    [questions]
  );

  const visibleQuestions = activeTab === "favorited"
    ? questions.filter((question) => question.favorited)
    : questions.filter((question) => question.wrong);

  function handleToggleFavorite(questionId) {
    setQuestions((current) =>
      current.map((question) =>
        question.id === questionId
          ? { ...question, favorited: !question.favorited }
          : question
      )
    );
  }

  return (
    <AsyncPage loading={loading} error={error}>
      {data ? (
        <div className="page-stack">
          <section className="glass-panel wordbook-hero">
            <div>
              <Text className="eyebrow">Grammar Notebook</Text>
              <Title>练习本</Title>
              <Text type="secondary">查看语法错题和收藏的题目。</Text>
            </div>
            <Button
              htmlType="button"
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate("/grammar")}
            >
              返回语法主页
            </Button>
          </section>

          <section className="glass-panel">
            <GrammarNotebookTabs
              activeTab={activeTab}
              counts={counts}
              onChange={setActiveTab}
            />
          </section>

          <section className="wordbook-list">
            {visibleQuestions.length ? (
              visibleQuestions.map((question) => (
                <GrammarNotebookItem
                  key={question.id}
                  onToggleFavorite={handleToggleFavorite}
                  question={question}
                />
              ))
            ) : (
              <div className="glass-panel">
                <Empty description={activeTab === "favorited" ? "暂无收藏题" : "暂无错题"} />
              </div>
            )}
          </section>
        </div>
      ) : null}
    </AsyncPage>
  );
}
