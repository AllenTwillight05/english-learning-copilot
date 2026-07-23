import { useCallback, useEffect, useMemo, useState } from "react";
import { ArrowLeftOutlined, LoginOutlined, ReadOutlined } from "@ant-design/icons";
import { App, Button, Empty, Flex, Result, Typography } from "antd";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { GrammarNotebookItem } from "../components/Grammar/GrammarNotebookItem";
import { GrammarNotebookTabs } from "../components/Grammar/GrammarNotebookTabs";
import { AsyncPage } from "../components/common/AsyncPage";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Title, Text } = Typography;

export function GrammarNotebookPage() {
  const { message } = App.useApp();
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const { grammar } = useAppServices();
  const loader = useCallback(() => {
    if (auth.loading || !auth.isAuthenticated) {
      return Promise.resolve(null);
    }

    return grammar.getNotebookQuestions();
  }, [auth.isAuthenticated, auth.loading, grammar]);
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

  async function handleToggleFavorite(questionId) {
    const currentQuestion = questions.find((question) => question.id === questionId);

    if (!currentQuestion) {
      return;
    }

    const previousFavorited = currentQuestion.favorited;
    const nextFavorited = !previousFavorited;

    setQuestions((current) =>
      current.map((question) =>
        question.id === questionId
          ? { ...question, favorited: nextFavorited }
          : question
      )
    );

    try {
      const result = await grammar.toggleGrammarFavorite({ grammarQuestionId: questionId });
      const updatedFavorited = result?.favorited ?? nextFavorited;
      setQuestions((current) =>
        current.map((question) =>
          question.id === questionId
            ? { ...question, favorited: updatedFavorited }
            : question
        )
      );
      message.success(updatedFavorited ? "已收藏语法题" : "已取消收藏");
    } catch (error) {
      setQuestions((current) =>
        current.map((question) =>
          question.id === questionId
            ? { ...question, favorited: previousFavorited }
            : question
        )
      );
      message.error(error?.status === 401 ? "请先登录后收藏语法题" : "收藏状态更新失败");
    }
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
