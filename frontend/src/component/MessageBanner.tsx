import { Alert } from "antd";
import { GlobalMsg, MessageType } from "../constants/types";

const typeMap: Record<MessageType, { antd: "error" | "warning" | "success" | "info"; icon: string }> = {
  ERROR: { antd: "error", icon: "❌" },
  WARNING: { antd: "warning", icon: "⚠️" },
  SUCCESS: { antd: "success", icon: "✅" },
  INFO: { antd: "info", icon: "ℹ️" },
  STATUS: { antd: "info", icon: "🔁" },
  CONFIRMATION: { antd: "success", icon: "✅" },
};

interface MessageBannerProps {
  messages: GlobalMsg[];
  closable?: boolean;
}

export function MessageBanner({ messages, closable = false }: MessageBannerProps) {
  if (!messages || messages.length === 0) return null;
  return (
    <div style={{ marginBottom: "16px" }}>
      {messages.map((msg, index) => {
        const mapped = typeMap[msg.type];
        return (
          <Alert
            key={index}
            type={mapped.antd}
            description={msg.message}
            showIcon
            closable={closable}
            style={{ marginBottom: "8px" }}
          />
        );
      })}
    </div>
  );
};