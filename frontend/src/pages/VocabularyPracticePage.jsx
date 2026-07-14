import { useCallback, useEffect, useMemo, useState } from "react";
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  StopOutlined,
  SwapOutlined
} from "@ant-design/icons";
import { Button, Flex, Space, Tag, Typography } from "antd";
import { useNavigate, useParams } from "react-router-dom";
import { AudioToChineseQuestion } from "../components/Vocabulary/AudioToChineseQuestion";
import { ChineseToEnglishQuestion } from "../components/Vocabulary/ChineseToEnglishQuestion";
import { EnglishToChineseQuestion } from "../components/Vocabulary/EnglishToChineseQuestion";
import { VocabularyWordCard } from "../components/Vocabulary/VocabularyWordCard";
import { AsyncPage } from "../components/common/AsyncPage";
import { useAsyncData } from "../hooks/useAsyncData";
import { useAppServices } from "../services/ServiceContext";

const { Title, Text } = Typography;

const questionTypes = ["englishToChinese", "chineseToEnglish", "audioToChinese"];

const questionTypeLabels = {
  audioToChinese: "语音选中文",
  chineseToEnglish: "中文选英文",
  englishToChinese: "英文选中文"
};

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

function getCorrectAnswer(question, questionType) {
  if (questionType === "chineseToEnglish") {
    return question.word;
  }

  return question.briefTranslation;
}

function renderQuestion({ question, questionType, selectedAnswer, answered, onAnswer, onPlayAudio }) {
  const props = {
    answered,
    onAnswer,
    question,
    selectedAnswer
  };

  if (questionType === "chineseToEnglish") {
    return <ChineseToEnglishQuestion {...props} />;
  }

  if (questionType === "audioToChinese") {
    return <AudioToChineseQuestion {...props} onPlayAudio={onPlayAudio} />;
  }

  return <EnglishToChineseQuestion {...props} />;
}

export function VocabularyPracticePage() {
  const navigate = useNavigate();
  const { level = "starter" } = useParams();
  const { vocabulary } = useAppServices();
  const isReview = level === "review";
  const loader = useCallback(
    () =>
      isReview
        ? vocabulary.getReviewVocabulary()
        : vocabulary.getVocabularyPracticeWords({ level }),
    [isReview, level, vocabulary]
  );
  const { data: practiceWords, loading, error } = useAsyncData(loader, [loader]);
  const [questionIndex, setQuestionIndex] = useState(0);
  const [questionTypeIndex, setQuestionTypeIndex] = useState(0);
  const [selectedAnswer, setSelectedAnswer] = useState(null);
  const [selectedRating, setSelectedRating] = useState(null);
  const [answerRecords, setAnswerRecords] = useState([]);

  const currentQuestion = practiceWords?.[questionIndex];
  const questionType = questionTypes[questionTypeIndex];
  const correctAnswer = useMemo(
    () => (currentQuestion ? getCorrectAnswer(currentQuestion, questionType) : ""),
    [currentQuestion, questionType]
  );
  const answered = selectedAnswer !== null;
  const isCorrect = answered && selectedAnswer === correctAnswer;

  useEffect(() => {
    setQuestionIndex(0);
    setAnswerRecords([]);
    resetAnswerState();
  }, [level]);

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
  }, [answered, isCorrect, questionType, selectedAnswer, currentQuestion, practiceWords]);

  function resetAnswerState() {
    setSelectedAnswer(null);
    setSelectedRating(null);
  }

  function handleSwitchQuestionType() {
    setQuestionTypeIndex((current) => (current + 1) % questionTypes.length);
    resetAnswerState();
  }

  function buildCurrentRecord(rating = selectedRating) {
    if (!answered || !currentQuestion) {
      return null;
    }

    return {
      correct: isCorrect,
      questionType,
      rating,
      selectedAnswer,
      wordId: currentQuestion.id,
      word: currentQuestion.word
    };
  }

  function saveCurrentAnswer(rating) {
    const record = buildCurrentRecord(rating);

    if (!record) {
      return;
    }

    setAnswerRecords((current) => [...current, record]);
  }

  function handleRateCurrentQuestion(rating) {
    setSelectedRating(rating);
    saveCurrentAnswer(rating);
    setQuestionIndex((current) => (current + 1) % practiceWords.length);
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

    navigate("/vocabulary/result", {
      state: {
        level,
        summary: buildSummary(finalRecords)
      }
    });
  }

  function handlePlayAudio() {
    if (!window.speechSynthesis) {
      return;
    }

    if (currentQuestion.us_audio) {
      new Audio(currentQuestion.us_audio).play();
      return;
    }

    const utterance = new SpeechSynthesisUtterance(currentQuestion.word);
    utterance.lang = "en-US";
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(utterance);
  }

  return (
    <AsyncPage loading={loading} error={error}>
      {practiceWords?.length ? (
        <div className="page-stack">
          <section className="practice-shell glass-panel">
            <Flex justify="space-between" align="start" gap={16} wrap>
              <div>
                <Space align="center" wrap>
                  <Tag bordered={false} className="soft-tag soft-tag--dark">
                    {isReview ? "Review" : level}
                  </Tag>
                  <Tag bordered={false} className="soft-tag">
                    {questionIndex + 1} / {practiceWords.length}
                  </Tag>
                </Space>
                <Title level={2}>{isReview ? "词汇复习" : "词汇练习"}</Title>
              </div>
              <Button htmlType="button" icon={<SwapOutlined />} onClick={handleSwitchQuestionType}>
                {questionTypeLabels[questionType]}
              </Button>
            </Flex>

            {renderQuestion({
              answered,
              onAnswer: setSelectedAnswer,
              onPlayAudio: handlePlayAudio,
              question: currentQuestion,
              questionType,
              selectedAnswer
            })}

            {answered ? (
              <div className={isCorrect ? "answer-feedback answer-feedback--correct" : "answer-feedback answer-feedback--wrong"}>
                {isCorrect ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
                <Text strong>{isCorrect ? "回答正确" : `回答错误，正确答案是：${correctAnswer}`}</Text>
              </div>
            ) : null}
          </section>

          {answered ? (
            <VocabularyWordCard
              onRate={handleRateCurrentQuestion}
              selectedRating={selectedRating}
              word={currentQuestion}
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
            <Title level={3}>暂无可练习词汇</Title>
            <Button htmlType="button" onClick={() => navigate("/vocabulary")}>
              返回词汇主页
            </Button>
          </section>
        </div>
      )}
    </AsyncPage>
  );
}
