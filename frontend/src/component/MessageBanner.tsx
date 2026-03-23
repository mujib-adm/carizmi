import { Alert } from 'antd';
import { GlobalMsg, MessageType } from '../api/generated/types/index';

const typeMap: Record<
  MessageType,
  { antd: 'error' | 'warning' | 'success' | 'info'; icon: string }
> = {
  ERROR: { antd: 'error', icon: '❌' },
  WARNING: { antd: 'warning', icon: '⚠️' },
  SUCCESS: { antd: 'success', icon: '✅' },
  INFO: { antd: 'info', icon: 'ℹ️' },
  STATUS: { antd: 'info', icon: '🔁' },
  CONFIRMATION: { antd: 'success', icon: '✅' },
};

interface MessageBannerProps {
  messages: GlobalMsg[];
  closable?: boolean;
}

export function MessageBanner({ messages, closable = false }: MessageBannerProps) {
  if (!messages || messages.length === 0) return null;
  return (
    <div style={{ marginBottom: '16px' }}>
      {messages.map((msg, index) => {
        // Safely cast msg.type to MessageType and provide a fallback for the antd type
        const mappedType = typeMap[msg.type as MessageType]?.antd || 'info';
        return (
          <Alert
            key={index}
            type={mappedType}
            description={msg.message}
            showIcon
            closable={closable}
            style={{ marginBottom: '8px' }}
          />
        );
      })}
    </div>
  );
}
