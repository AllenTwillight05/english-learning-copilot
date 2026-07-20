import { useCallback, useMemo, useState } from "react";
import { BookOutlined, HistoryOutlined, LoginOutlined, SearchOutlined, UserOutlined } from "@ant-design/icons";
import { Button, Empty, Flex, Input, Result, Space, Tag, Typography } from "antd";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { GrammarMasteryPanel } from "../components/Grammar/GrammarMasteryPanel";
import { GrammarProgressRing } from "../components/Grammar/GrammarProgressRing";
import { GrammarTopicGrid } from "../components/Grammar/GrammarTopicGrid";
import { AsyncPage } from "../components/common/AsyncPage";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Title, Paragraph } = Typography;

function syncTodayPendingStat(overview, progress) {
  const stats = overview.stats.map((stat, index) => {
    if (index !== overview.stats.length - 1) {
      return stat;
    }

    return {
      ...stat,
      value: `${progress.remaining} 题`
    };
  });

  return {
    ...overview,
    stats
  };
}

export function GrammarPage() {
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const { grammar } = useAppServices();
  const [searchText, setSearchText] = useState("");
  const loader = useCallback(async () => {
    if (auth.loading || !auth.isAuthenticated) {
      return null;
    }

    const [overview, progress, topics] = await Promise.all([
      grammar.getOverview(),
      grammar.getProgress(),
      grammar.getTopics()
    ]);

    return {
      overview: syncTodayPendingStat(overview, progress),
      progress,
      topics
    };
  }, [auth.isAuthenticated, auth.loading, grammar]);
  const { data, loading, error } = useAsyncData(loader, [loader]);
  const filteredTopics = useMemo(() => {
    const keyword = searchText.trim().toLowerCase();

    if (!keyword) {
      return data?.topics ?? [];
    }

    return (data?.topics ?? []).filter((topic) => topic.title.toLowerCase().includes(keyword));
  }, [data?.topics, searchText]);

  if (auth.loading) {
    return <AsyncPage loading error={null} />;
  }

  if (!auth.isAuthenticated) {
    return (
      <div className="page-stack">
        <section className="glass-panel profile-empty-state">
          <Result
            icon={<UserOutlined />}
            title="个人页面需要登录"
            subTitle="登录后可以查看你的学习计划、能力进度和最近反馈。"
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
        <div className="page-stack grammar-page">
          <section className="vocabulary-hero glass-panel">
            <div className="vocabulary-hero__copy">
              <Space align="center" wrap>
                <Tag bordered={false} className="soft-tag soft-tag--dark">
                  Grammar Practice
                </Tag>
              </Space>
              <Title>语法练习</Title>
              <Paragraph>按语法知识点进入专项训练，先从选择题题库开始建立正确率反馈。</Paragraph>
            </div>

            <GrammarProgressRing
              completed={data.progress.completed}
              remaining={data.progress.remaining}
              total={data.progress.total}
            />

            <GrammarMasteryPanel overview={data.overview} />
          </section>

          <section className="glass-panel grammar-search-panel">
            <Flex align="center" className="grammar-search-row" gap={10} justify="space-between" wrap>
              <Input
                allowClear
                onChange={(event) => setSearchText(event.target.value)}
                placeholder="搜索语法类型"
                prefix={<SearchOutlined />}
                size="large"
                value={searchText}
              />
              <Space wrap>
                <Button
                  htmlType="button"
                  icon={<HistoryOutlined />}
                  onClick={() => navigate("/grammar/practice/review")}
                  size="large"
                >
                  复习
                </Button>
                <Button
                  htmlType="button"
                  icon={<BookOutlined />}
                  onClick={() => navigate("/grammar/notebook")}
                  size="large"
                >
                  练习本
                </Button>
              </Space>
            </Flex>
          </section>

          {filteredTopics.length ? (
            <GrammarTopicGrid
              onStart={(topic) => navigate(`/grammar/practice/${encodeURIComponent(topic.title)}`)}
              topics={filteredTopics}
            />
          ) : (
            <section className="glass-panel">
              <Empty description="没有匹配的语法类型" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            </section>
          )}
        </div>
      ) : null}
    </AsyncPage>
  );
}
