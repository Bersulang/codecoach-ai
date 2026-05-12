import { Button } from "antd";
import { SendOutlined } from "@ant-design/icons";
import { useEffect, useRef } from "react";
import "./ChatInputBox.css";

interface ChatInputBoxProps {
  value: string;
  onChange: (value: string) => void;
  onSend: () => void;
  disabled?: boolean;
  loading?: boolean;
  placeholder?: string;
  submitHint?: string;
}

function ChatInputBox({
  value,
  onChange,
  onSend,
  disabled = false,
  loading = false,
  placeholder = "输入你的回答...",
  submitHint = "Enter 发送，Shift + Enter 换行",
}: ChatInputBoxProps) {
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const canSend = value.trim().length > 0 && !disabled && !loading;

  useEffect(() => {
    const textarea = textareaRef.current;
    if (!textarea) {
      return;
    }
    textarea.style.height = "auto";
    textarea.style.height = `${Math.min(textarea.scrollHeight, 180)}px`;
  }, [value]);

  return (
    <div className="chat-input-box">
      <textarea
        ref={textareaRef}
        className="chat-input-box__textarea"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            if (canSend) {
              onSend();
            }
          }
        }}
        rows={1}
        placeholder={placeholder}
        disabled={disabled || loading}
      />
      <div className="chat-input-box__footer">
        <span>{submitHint}</span>
        <Button
          type="primary"
          shape="circle"
          icon={<SendOutlined />}
          loading={loading}
          disabled={!canSend}
          onClick={onSend}
        />
      </div>
    </div>
  );
}

export default ChatInputBox;
