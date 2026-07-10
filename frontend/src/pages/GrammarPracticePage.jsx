import { useCallback, useEffect, useMemo, useState } from "react";
import { CheckCircleOutlined, CloseCircleOutlined, StopOutlined } from "@ant-design/icons";
import { Button, Flex, Space, Tag, Typography } from "antd";
import { useNavigate, useParams } from "react-router-dom";
import { GrammarExplanationCard } from "../components/Grammar/GrammarExplanationCard";
import { GrammarQuestionCard } from "../components/Grammar/GrammarQuestionCard";
import { AsyncPage } from "../components/common/AsyncPage";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Title, Text } = Typography;

const ratingShortcuts = {
  1: "重来",
  2: "困难",
  3: "良好",
  4: "简单"
};

const emptyRatingSummary = {
  重来: 0,
  困难: 0,
  良好: 0,
  简单: 0
};

function getAnswerText(question) {
  const answerIndex = question.answer.charCodeAt(0) - "A".charCodeAt(0);
  return question.options[answerIndex] ?? "";
}

export function GrammarPracticePage() {
  const navigate = useNavigate();
  const { category = "" } = useParams();
  const decodedCategory = decodeURIComponent(category);
  const { grammar } = useAppServices();
  const loader = useCallback(
    () => grammar.getPracticeQuestions({ category: decodedCategory }),
    [decodedCategory, grammar]
  );
  const { data: questions, loading, error } = useAsyncData(loader, [loader]);
  const [questionIndex, setQuestionIndex] = useState(0);
  const [selectedAnswer, setSelectedAnswer] = useState(null);
  const [answerRecords, setAnswerRecords] = useState([]);

  const currentQuestion = questions?.[questionIndex];
  const answered = selectedAnswer !== null;
  const isCorrect = answered && selectedAnswer === currentQuestion?.answer;
  const correctAnswer = useMemo(
    () => ({
      letter: currentQuestion?.answer ?? "",
      text: currentQuestion ? getAnswerText(currentQuestion) : ""
    }),
    [currentQuestion]
  );

  useEffect(() => {
    setQuestionIndex(0);
    setSelectedAnswer(null);
    setAnswerRecords([]);
  }, [decodedCategory]);

  useEffect(() => {
    function handleRatingShortcut(event) {
      const rating = ratingShortcuts[event.key];

      if (!answered || !rating) {
        return;
      }

      event.preventDefault();
      handleRateCurrentQuestion(rating);
    }

    window.addEventListener("keydown", handleRatingShortcut);

    return () => {
      window.removeEventListener("keydown", handleRatingShortcut);
    };
  }, [answered, selectedAnswer, currentQuestion, questions]);

  function resetAnswerState() {
    setSelectedAnswer(null);
  }

  function buildCurrentRecord(rating = null) {
    if (!answered || !currentQuestion) {
      return null;
    }

    return {
      correct: isCorrect,
      grammarCategory: currentQuestion.grammar_category,
      questionId: currentQuestion.id,
      rating,
      selectedAnswer
    };
  }

  function handleRateCurrentQuestion(rating) {
    const record = buildCurrentRecord(rating);

    if (record) {
      setAnswerRecords((current) => [...current, record]);
    }

    setQuestionIndex((current) => (current + 1) % questions.length);
    resetAnswerState();
  }

  function buildSummary(records) {
    return records.reduce(
      (summary, record) => {
        summary.total += 1;

        if (record.correct) {
          summary.correct += 1;
        } else {
          summary.wrong += 1;
        }

        if (record.rating) {
          summary.ratings[record.rating] = (summary.ratings[record.rating] ?? 0) + 1;
        }

        return summary;
      },
      {
        total: 0,
        correct: 0,
        wrong: 0,
        ratings: { ...emptyRatingSummary }
      }
    );
  }

  function handleFinishPractice() {
    const currentRecord = buildCurrentRecord();
    const finalRecords = currentRecord ? [...answerRecords, currentRecord] : answerRecords;

    navigate("/grammar/result", {
      state: {
        category: decodedCategory,
        summary: buildSummary(finalRecords)
      }
    });
  }

  return (
    <AsyncPage loading={loading} error={error}>
      {questions?.length ? (
        <div className="page-stack">
          <section className="practice-shell glass-panel">
            <Flex justify="space-between" align="start" gap={16} wrap>
              <div>
                <Space align="center" wrap>
                  <Tag bordered={false} className="soft-tag soft-tag--dark">
                    {decodedCategory}
                  </Tag>
                  <Tag bordered={false} className="soft-tag">
                    {questionIndex + 1} / {questions.length}
                  </Tag>
                </Space>
                <Title level={2}>语法练习</Title>
              </div>
            </Flex>

            <GrammarQuestionCard
              answered={answered}
              onAnswer={setSelectedAnswer}
              question={currentQuestion}
              selectedAnswer={selectedAnswer}
            />

            {answered ? (
              <div className={isCorrect ? "answer-feedback answer-feedback--correct" : "answer-feedback answer-feedback--wrong"}>
                {isCorrect ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
                <Text strong>
                  {isCorrect ? "回答正确" : `回答错误，正确答案是：${correctAnswer.letter}`}
                </Text>
              </div>
            ) : null}
          </section>

          {answered ? (
            <GrammarExplanationCard
              correctAnswer={correctAnswer}
              explanation={currentQuestion.explanation}
              isCorrect={isCorrect}
              onRate={handleRateCurrentQuestion}
            />
          ) : null}

          <Flex justify="end" gap={12} wrap>
            <Button
              htmlType="button"
              icon={<StopOutlined />}
              onClick={handleFinishPractice}
              size="large"
            >
              结束练习
            </Button>
          </Flex>
        </div>
      ) : (
        <div className="page-stack">
          <section className="glass-panel">
            <Title level={3}>暂无可练习题目</Title>
            <Button htmlType="button" onClick={() => navigate("/grammar")}>
              返回语法主页
            </Button>
          </section>
        </div>
      )}
    </AsyncPage>
  );
}
