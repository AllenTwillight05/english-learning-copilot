import { Typography } from "antd";

const { Paragraph } = Typography;

function normalizeDefinitionText(value) {
  return String(value ?? "")
    .replace(/\\r\\n|\\n|\\r/g, "\n")
    .replace(/\r\n?/g, "\n")
    .trim();
}

export function VocabularyDefinitionText({ className, value }) {
  const normalizedValue = normalizeDefinitionText(value);
  const lines = normalizedValue ? normalizedValue.split("\n") : [];

  if (!normalizedValue) {
    return <Paragraph className={className}>-</Paragraph>;
  }

  return (
    <Paragraph className={className}>
      {lines.map((line, index) => (
        <span className="definition-line" key={`${index}-${line}`}>
          {line}
          {index < lines.length - 1 ? <br /> : null}
        </span>
      ))}
    </Paragraph>
  );
}
