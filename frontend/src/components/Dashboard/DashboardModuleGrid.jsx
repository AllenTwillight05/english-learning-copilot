import {
  AudioOutlined,
  BookOutlined,
  ReadOutlined,
  UserOutlined
} from "@ant-design/icons";
import { Typography } from "antd";

const { Text } = Typography;

// 首页模块入口卡片，只负责导航展示，具体页面内容由各自模块维护。
const moduleCards = [
  {
    title: "口语",
    description: "情境对话、录音练习与口语反馈",
    path: "/speaking",
    icon: <AudioOutlined />
  },
  {
    title: "词汇",
    description: "单词本与词汇练习",
    path: "/vocabulary",
    icon: <BookOutlined />
  },
  {
    title: "语法",
    description: "练习本与语法练习",
    path: "/grammar",
    icon: <ReadOutlined />
  },
  {
    title: "个人",
    description: "学习计划、能力进度和最近反馈",
    path: "/profile",
    icon: <UserOutlined />
  }
];

export function DashboardModuleGrid({ onNavigate }) {
  return (
    <section className="home-panel-grid">
      {moduleCards.map((card) => (
        <button
          className="home-panel"
          key={card.path}
          type="button"
          onClick={() => onNavigate(card.path)}
        >
          <span className="home-panel__icon">{card.icon}</span>
          <span className="home-panel__copy">
            <Text strong className="home-panel__title">
              {card.title}
            </Text>
            <span className="helper-text">{card.description}</span>
          </span>
        </button>
      ))}
    </section>
  );
}
