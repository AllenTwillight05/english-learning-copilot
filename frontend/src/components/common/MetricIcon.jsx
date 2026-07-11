import {
  AudioOutlined,
  BookOutlined,
  ClockCircleOutlined,
  FireOutlined,
  ReadOutlined,
  RiseOutlined,
  SoundOutlined
} from "@ant-design/icons";

const iconMap = {
  microphone: <AudioOutlined />,
  waveform: <SoundOutlined />,
  streak: <FireOutlined />,
  trend: <RiseOutlined />,
  clock: <ClockCircleOutlined />,
  vocabulary: <BookOutlined />,
  grammar: <ReadOutlined />
};

export function MetricIcon({ icon }) {
  return <>{iconMap[icon]}</>;
}
