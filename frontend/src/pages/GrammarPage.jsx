import { useCallback, useMemo, useState } from "react";
import { BookOutlined, HistoryOutlined, SearchOutlined } from "@ant-design/icons";
import { Button, Empty, Flex, Input, Space, Tag, Typography } from "antd";
import { useNavigate } from "react-router-dom";
import { GrammarMasteryPanel } from "../components/Grammar/GrammarMasteryPanel";
import { GrammarProgressRing } from "../components/Grammar/GrammarProgressRing";
import { GrammarTopicGrid } from "../components/Grammar/GrammarTopicGrid";
import { AsyncPage } from "../components/common/AsyncPage";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Title, Paragraph } = Typography;

export function GrammarPage() {
  const navigate = useNavigate();
  const { grammar } = useAppServices();
  const [searchText, setSearchText] = useState("");
  const loader = useCallback(async () => {
    const [overview, progress, topics] = await Promise.all([
      grammar.getOverview(),
      grammar.getProgress(),
      grammar.getTopics()
    ]);

    return { overview, progress, topics };
  }, [grammar]);
  const { data, loading, error } = useAsyncData(loader, [loader]);
  const filteredTopics = useMemo(() => {
    const keyword = searchText.trim().toLowerCase();

    if (!keyword) {
      return data?.topics ?? [];
    }

    return (data?.topics ?? []).filter((topic) => topic.title.toLowerCase().includes(keyword));
  }, [data?.topics, searchText]);

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
