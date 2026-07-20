import { CalendarOutlined, CloseOutlined } from "@ant-design/icons";
import { Button, Progress, Typography } from "antd";
import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAsyncData } from "../../hooks/useAsyncData";
import { useAppServices } from "../../services/ServiceContext";
import "./FloatingPetCompanion.css";

const { Text, Title } = Typography;
const PLAN_AREAS = ["speaking", "vocabulary", "grammar"];
const EDGE_GAP = 16;
const PET_SIZE = 112;

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

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

export function FloatingPetCompanion() {
  const navigate = useNavigate();
  const { dashboard } = useAppServices();
  const [position, setPosition] = useState(null);
  const [isHovered, setIsHovered] = useState(false);
  const [isPinned, setIsPinned] = useState(false);
  const dragRef = useRef(null);
  const suppressClickRef = useRef(false);

  const loader = useCallback(
    () =>
      Promise.all([dashboard.getStudyPlan(), dashboard.getRecommendedTask()]).then(
        ([plan, recommendedTask]) => ({ plan, recommendedTask })
      ),
    [dashboard]
  );
  const { data } = useAsyncData(loader, [loader]);
  const { completed, total } = getPlanProgress(data?.plan);
  const percentage = total > 0 ? Math.round((completed / total) * 100) : 0;
  const pet = getPetStatus({
    completed,
    total,
    recommendedTask: data?.recommendedTask?.topic
  });
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
    event.currentTarget.setPointerCapture(event.pointerId);
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

  function openPlan() {
    closePanel();
    navigate("/profile");
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
          <span className="floating-pet__status">{pet.label}</span>
          <Text strong>{pet.title}</Text>
          <Text type="secondary">{pet.message}</Text>
        </div>

        <div className="floating-pet__progress">
          <div>
            <Text type="secondary">今日计划进度</Text>
            <Text strong>
              {completed}/{total}
            </Text>
          </div>
          <Progress percent={percentage} showInfo={false} strokeColor="#6d8fed" trailColor="rgba(109, 143, 237, 0.14)" />
        </div>

        <Button type="primary" block icon={<CalendarOutlined />} onClick={openPlan}>
          查看今日计划
        </Button>
      </section>
    </div>
  );
}
