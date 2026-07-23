import {
  BulbOutlined,
  CalendarOutlined,
  CloseOutlined,
  LoginOutlined
} from "@ant-design/icons";
import { Button, Progress, Typography } from "antd";
import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";
import { useAsyncData } from "../../hooks/useAsyncData";
import { useAppServices } from "../../services/ServiceContext";
import "./FloatingPetCompanion.css";
import "./FloatingPetWelcome.css";

const { Text, Title } = Typography;
const PLAN_AREAS = ["speaking", "vocabulary", "grammar"];
const EDGE_GAP = 16;
const PET_SIZE = 144;

function getPlanProgress(plan) {
  return PLAN_AREAS.reduce(
    (progress, area) => {
      const item = plan?.[area];

      return {
        completed: progress.completed + (item?.completed ?? 0),
        total: progress.total + (item?.total ?? 0)
      };
    },
    { completed: 0, total: 0 }
  );
}

function getPetStatus({ completed, total, recommendedTask }) {
  if (total > 0 && completed >= total) {
    return {
      image: "/pet/clawd-celebrating.svg",
      alt: "学习伙伴正在庆祝今日计划完成",
      label: "今日全勤",
      title: "太棒了，今天的计划完成啦！",
      message: "我已经为你记下这次坚持。明天也一起继续保持节奏吧。"
    };
  }

  if (completed > 0) {
    return {
      image: "/pet/clawd-happy.svg",
      alt: "学习伙伴开心地陪伴学习",
      label: "正在陪练",
      title: `已完成 ${completed} 个小目标`,
      message: `还差 ${Math.max(total - completed, 0)} 个小目标，${recommendedTask || "我们继续完成今天的练习"}。`
    };
  }

  return {
    image: "/pet/clawd-working-thinking.svg",
    alt: "学习伙伴正在思考今日学习计划",
    label: "正在规划",
    title: "今天从哪里开始？",
    message: recommendedTask
      ? `我建议先练习「${recommendedTask}」，完成后我会为你庆祝。`
      : "完成一个小练习，就能让今天的学习计划动起来。"
  };
}

function getPetAdvice(plan, recommendedTask) {
  if (!plan) {
    return {
      message: "我还在同步今天的学习计划，先去个人页看看吧。",
      actionLabel: "查看个人计划",
      route: "/profile"
    };
  }

  const speaking = plan.speaking;
  const vocabulary = plan.vocabulary;
  const grammar = plan.grammar;

  if ((speaking?.completed ?? 0) < (speaking?.total ?? 0)) {
    return {
      message: recommendedTask?.topic
        ? `今天先练习「${recommendedTask.topic}」，用 10 分钟完成一次开口表达吧。`
        : "今天先完成一轮口语练习，用 10 分钟开口表达吧。",
      actionLabel: "开始口语练习",
      route: "/speaking"
    };
  }

  if ((vocabulary?.completed ?? 0) < (vocabulary?.total ?? 0)) {
    return {
      message: "口语任务已完成，接下来复习几个词汇，让表达更自然。",
      actionLabel: "开始词汇练习",
      route: "/vocabulary"
    };
  }

  if ((grammar?.completed ?? 0) < (grammar?.total ?? 0)) {
    return {
      message: "最后完成一组语法练习，巩固今天的表达基础。",
      actionLabel: "开始语法练习",
      route: "/grammar"
    };
  }

  return {
    message: "今天的计划已经全部完成啦，明天继续保持这个节奏！",
    actionLabel: "查看个人计划",
    route: "/profile"
  };
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

export function FloatingPetCompanion() {
  const navigate = useNavigate();
  const auth = useAuth();
  const { dashboard } = useAppServices();
  const [position, setPosition] = useState(null);
  const [isHovered, setIsHovered] = useState(false);
  const [isPinned, setIsPinned] = useState(false);
  const [advice, setAdvice] = useState(null);
  const [planVersion, setPlanVersion] = useState(0);
  const dragRef = useRef(null);
  const suppressClickRef = useRef(false);

  const loader = useCallback(() => {
    if (auth.loading || !auth.isAuthenticated) {
      return Promise.resolve(null);
    }

    return Promise.all([dashboard.getStudyPlan(), dashboard.getRecommendedTask()]).then(
      ([plan, recommendedTask]) => ({ plan, recommendedTask })
    );
  }, [auth.isAuthenticated, auth.loading, dashboard, planVersion]);
  const { data } = useAsyncData(loader, [loader]);
  const { completed, total } = getPlanProgress(data?.plan);
  const percentage = total > 0 ? Math.round((completed / total) * 100) : 0;
  const learningPet = getPetStatus({
    completed,
    total,
    recommendedTask: data?.recommendedTask?.topic
  });
  const pet = auth.isAuthenticated
    ? learningPet
    : {
        image: "/pet/clawd-waving.svg",
        alt: "学习伙伴正在挥手欢迎",
        label: "初次见面",
        title: "嗨，我是你的学习伙伴",
        message: "登录后，我会陪你完成每日词汇和语法计划。"
      };
  const isOpen = isHovered || isPinned;
  const isRightAligned = !position || position.x > window.innerWidth - 360;

  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === "Escape") {
        closePanel();
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("pointermove", handlePointerMove);
    window.addEventListener("pointerup", handlePointerUp);
    window.addEventListener("pointercancel", handlePointerUp);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerup", handlePointerUp);
      window.removeEventListener("pointercancel", handlePointerUp);
    };
  }, []);

  useEffect(() => {
    function refreshPlan() {
      setAdvice(null);
      setPlanVersion((version) => version + 1);
    }

    window.addEventListener("learning-plan-updated", refreshPlan);
    return () => window.removeEventListener("learning-plan-updated", refreshPlan);
  }, []);

  function handlePointerDown(event) {
    if (event.button !== 0) {
      return;
    }

    const bounds = event.currentTarget.getBoundingClientRect();
    dragRef.current = {
      pointerId: event.pointerId,
      offsetX: event.clientX - bounds.left,
      offsetY: event.clientY - bounds.top,
      startX: event.clientX,
      startY: event.clientY,
      moved: false
    };

    if (event.currentTarget.setPointerCapture) {
      event.currentTarget.setPointerCapture(event.pointerId);
    }
  }

  function handlePointerMove(event) {
    const drag = dragRef.current;

    if (!drag || drag.pointerId !== event.pointerId) {
      return;
    }

    const distance = Math.hypot(event.clientX - drag.startX, event.clientY - drag.startY);
    if (distance > 4) {
      drag.moved = true;
    }

    setPosition({
      x: clamp(event.clientX - drag.offsetX, EDGE_GAP, window.innerWidth - PET_SIZE - EDGE_GAP),
      y: clamp(event.clientY - drag.offsetY, EDGE_GAP, window.innerHeight - PET_SIZE - EDGE_GAP)
    });
  }

  function handlePointerUp(event) {
    const drag = dragRef.current;

    if (!drag || drag.pointerId !== event.pointerId) {
      return;
    }

    if (drag.moved) {
      suppressClickRef.current = true;
      window.setTimeout(() => {
        suppressClickRef.current = false;
      }, 0);
    }

    dragRef.current = null;
  }

  function handleTriggerClick() {
    if (suppressClickRef.current) {
      return;
    }

    setIsPinned((current) => !current);
  }

  function closePanel() {
    setIsPinned(false);
    setIsHovered(false);
  }

  function requestAdvice() {
    setAdvice(getPetAdvice(data?.plan, data?.recommendedTask));
    setIsPinned(true);
  }

  function openRoute(route) {
    closePanel();
    navigate(route);
  }

  function openLogin() {
    openRoute("/login");
  }

  function openPlan() {
    openRoute("/profile");
  }

  const wrapperStyle = position ? { left: position.x, top: position.y } : undefined;
  const wrapperClassName = [
    "floating-pet",
    position ? "floating-pet--positioned" : "",
    position && position.y < 240 ? "floating-pet--top" : "",
    isRightAligned ? "floating-pet--right" : "",
    isOpen ? "floating-pet--open" : ""
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div
      className={wrapperClassName}
      style={wrapperStyle}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <button
        type="button"
        className="floating-pet__trigger"
        aria-label="打开学习伙伴"
        aria-expanded={isOpen}
        onPointerDown={handlePointerDown}
        onDragStart={(event) => event.preventDefault()}
        onClick={handleTriggerClick}
      >
        <img src={pet.image} alt="" aria-hidden="true" />
      </button>

      <section className="floating-pet__panel" aria-label="学习伙伴面板">
        {auth.isAuthenticated ? (
          <>
            <div className="floating-pet__panel-header">
              <div>
                <Text className="eyebrow">Learning Companion</Text>
                <Title level={5}>学习伙伴</Title>
              </div>
              <Button
                type="text"
                shape="circle"
                icon={<CloseOutlined />}
                aria-label="关闭学习伙伴面板"
                onClick={closePanel}
              />
            </div>

            <div className="floating-pet__message">
              <span className="floating-pet__status">{advice ? "学习建议" : pet.label}</span>
              <Text strong>{advice ? "我为你选好了" : pet.title}</Text>
              <Text type="secondary">{advice?.message ?? pet.message}</Text>
            </div>

            <div className="floating-pet__progress">
              <div>
                <Text type="secondary">今日计划进度</Text>
                <Text strong>
                  {completed}/{total}
                </Text>
              </div>
              <Progress
                percent={percentage}
                showInfo={false}
                strokeColor="#6d8fed"
                trailColor="rgba(109, 143, 237, 0.14)"
              />
            </div>

            {advice ? (
              <Button
                type="primary"
                block
                icon={<CalendarOutlined />}
                onClick={() => openRoute(advice.route)}
              >
                {advice.actionLabel}
              </Button>
            ) : (
              <Button block icon={<BulbOutlined />} onClick={requestAdvice}>
                今天学什么？
              </Button>
            )}

            <Button type="text" block className="floating-pet__plan-link" onClick={openPlan}>
              查看今日计划
            </Button>
          </>
        ) : (
          <div className="floating-pet__welcome">
            <Button
              type="text"
              shape="circle"
              className="floating-pet__welcome-close"
              icon={<CloseOutlined />}
              aria-label="关闭学习伙伴面板"
              onClick={closePanel}
            />
            <img src="/pet/clawd-waving.svg" alt="学习伙伴正在挥手欢迎" />
            <Text className="eyebrow">Hi, I am Lumi</Text>
            <Title level={5}>嗨，我是你的学习伙伴</Title>
            <Text type="secondary">
              我会陪你拆解每日任务、记录进步，也会在完成时和你一起庆祝。
            </Text>
            <Button type="primary" block icon={<LoginOutlined />} onClick={openLogin}>
              登录后一起学习
            </Button>
          </div>
        )}
      </section>
    </div>
  );
}
