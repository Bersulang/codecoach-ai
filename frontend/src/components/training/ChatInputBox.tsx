import { Button, Tooltip, message } from "antd";
import {
  AudioOutlined,
  LoadingOutlined,
  SendOutlined,
  StopOutlined,
} from "@ant-design/icons";
import { useEffect, useMemo, useRef, useState } from "react";
import "./ChatInputBox.css";

interface ChatInputBoxProps {
  value: string;
  onChange: (value: string) => void;
  onSend: () => void;
  disabled?: boolean;
  loading?: boolean;
  placeholder?: string;
  submitHint?: string;
  enableVoice?: boolean;
}

type SpeechRecognitionErrorEvent = Event & { error?: string };
type SpeechRecognitionResultAlternative = { transcript: string };
type SpeechRecognitionResult = {
  isFinal: boolean;
  length: number;
  [index: number]: SpeechRecognitionResultAlternative;
};
type SpeechRecognitionEvent = Event & {
  resultIndex: number;
  results: {
    length: number;
    [index: number]: SpeechRecognitionResult;
  };
};
type SpeechRecognitionLike = {
  lang: string;
  interimResults: boolean;
  continuous: boolean;
  start: () => void;
  stop: () => void;
  abort: () => void;
  onstart: (() => void) | null;
  onresult: ((event: SpeechRecognitionEvent) => void) | null;
  onerror: ((event: SpeechRecognitionErrorEvent) => void) | null;
  onend: (() => void) | null;
};
type SpeechRecognitionConstructor = new () => SpeechRecognitionLike;

declare global {
  interface Window {
    SpeechRecognition?: SpeechRecognitionConstructor;
    webkitSpeechRecognition?: SpeechRecognitionConstructor;
  }
}

function ChatInputBox({
  value,
  onChange,
  onSend,
  disabled = false,
  loading = false,
  placeholder = "输入你的回答...",
  submitHint = "Enter 发送，Shift + Enter 换行",
  enableVoice = true,
}: ChatInputBoxProps) {
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const recognitionRef = useRef<SpeechRecognitionLike | null>(null);
  const baseTextRef = useRef("");
  const finalTextRef = useRef("");
  const [voiceState, setVoiceState] = useState<
    "idle" | "recording" | "recognizing" | "error" | "unsupported"
  >("idle");
  const canSend = value.trim().length > 0 && !disabled && !loading;
  const speechCtor = useMemo(() => {
    if (typeof window === "undefined") {
      return undefined;
    }
    return window.SpeechRecognition || window.webkitSpeechRecognition;
  }, []);
  const voiceSupported = Boolean(speechCtor);
  const voiceDisabled = disabled || loading || !enableVoice || !voiceSupported;

  useEffect(() => {
    const textarea = textareaRef.current;
    if (!textarea) {
      return;
    }
    textarea.style.height = "auto";
    textarea.style.height = `${Math.min(textarea.scrollHeight, 180)}px`;
  }, [value]);

  useEffect(
    () => () => {
      recognitionRef.current?.abort();
      recognitionRef.current = null;
    },
    [],
  );

  useEffect(() => {
    if (enableVoice && !voiceSupported) {
      setVoiceState("unsupported");
    }
  }, [enableVoice, voiceSupported]);

  const stopVoice = () => {
    if (!recognitionRef.current) {
      return;
    }
    setVoiceState("recognizing");
    recognitionRef.current.stop();
  };

  const startVoice = () => {
    if (!enableVoice) {
      return;
    }
    if (!speechCtor) {
      setVoiceState("unsupported");
      message.info("当前浏览器不支持语音输入，可继续手动输入。");
      return;
    }
    if (disabled || loading) {
      return;
    }

    recognitionRef.current?.abort();
    const recognition = new speechCtor();
    recognitionRef.current = recognition;
    baseTextRef.current = value;
    finalTextRef.current = "";
    recognition.lang = "zh-CN";
    recognition.interimResults = true;
    recognition.continuous = true;
    recognition.onstart = () => setVoiceState("recording");
    recognition.onresult = (event) => {
      let interim = "";
      for (let index = event.resultIndex; index < event.results.length; index += 1) {
        const result = event.results[index];
        const transcript = result[0]?.transcript || "";
        if (result.isFinal) {
          finalTextRef.current += transcript;
        } else {
          interim += transcript;
        }
      }
      const prefix = baseTextRef.current.trimEnd();
      const spacer = prefix ? "\n" : "";
      onChange(`${prefix}${spacer}${finalTextRef.current}${interim}`);
    };
    recognition.onerror = (event) => {
      const error = event.error || "";
      setVoiceState("error");
      if (error === "not-allowed" || error === "service-not-allowed") {
        message.warning("麦克风权限被拒绝，请在浏览器设置中开启权限后重试。");
      } else {
        message.warning("语音识别失败，可重试或继续手动输入。");
      }
    };
    recognition.onend = () => {
      recognitionRef.current = null;
      setVoiceState((prev) => (prev === "error" ? "error" : "idle"));
      window.setTimeout(() => textareaRef.current?.focus(), 0);
    };

    try {
      recognition.start();
    } catch {
      setVoiceState("error");
      message.warning("语音输入启动失败，可重试或继续手动输入。");
    }
  };

  const renderVoiceIcon = () => {
    if (voiceState === "recording") {
      return <StopOutlined />;
    }
    if (voiceState === "recognizing") {
      return <LoadingOutlined />;
    }
    return <AudioOutlined />;
  };

  const voiceTitle =
    voiceState === "recording"
      ? "停止语音输入"
      : voiceState === "unsupported"
        ? "当前浏览器不支持语音输入"
        : "语音输入";

  return (
    <div className="chat-input-box">
      {voiceState === "recording" || voiceState === "recognizing" ? (
        <div className="chat-input-box__voice-status">
          <span className="chat-input-box__pulse" />
          {voiceState === "recording" ? "正在听你说话，点击停止后可继续编辑" : "正在整理识别结果"}
        </div>
      ) : voiceState === "error" ? (
        <div className="chat-input-box__voice-status is-error">
          语音输入失败，可重试或继续手动输入
        </div>
      ) : null}
      <div className="chat-input-box__row">
        {enableVoice ? (
          <Tooltip title={voiceTitle}>
            <Button
              className={`chat-input-box__voice ${
                voiceState === "recording" ? "is-recording" : ""
              }`}
              shape="circle"
              icon={renderVoiceIcon()}
              disabled={voiceDisabled && voiceState !== "recording"}
              onClick={voiceState === "recording" ? stopVoice : startVoice}
              aria-label={voiceTitle}
            />
          </Tooltip>
        ) : null}
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
        <Button
          className="chat-input-box__send"
          type="primary"
          shape="circle"
          icon={<SendOutlined />}
          loading={loading}
          disabled={!canSend}
          onClick={onSend}
        />
      </div>
      <div className="chat-input-box__footer">
        <span>{submitHint}</span>
      </div>
    </div>
  );
}

export default ChatInputBox;
