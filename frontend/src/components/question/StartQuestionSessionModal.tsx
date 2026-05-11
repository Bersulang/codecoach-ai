import { Form, Input, Modal, Radio, Typography, message } from "antd";
import { useEffect, useState } from "react";
import { createQuestionSession } from "../../api/question";
import type { InterviewDifficulty } from "../../types/interview";
import "./StartQuestionSessionModal.css";

const DEFAULT_TARGET_ROLE = "Java 后端实习";

const DIFFICULTY_OPTIONS: Array<{
  value: InterviewDifficulty;
  title: string;
  description: string;
}> = [
  {
    value: "EASY",
    title: "入门引导",
    description: "适合刚学完知识点，重点确认基础概念和表达。",
  },
  {
    value: "NORMAL",
    title: "常规面试",
    description: "模拟常规实习 / 校招技术面，关注原理、场景和常见追问。",
  },
  {
    value: "HARD",
    title: "深度拷打",
    description: "偏深度拷打，关注底层原理、边界条件、性能和工程权衡。",
  },
];

export interface StartQuestionSessionTopic {
  id: number;
  category: string;
  name: string;
  description?: string;
}

interface StartQuestionSessionModalProps {
  open: boolean;
  topic: StartQuestionSessionTopic | null;
  onCancel: () => void;
  onSuccess: (sessionId: number) => void;
}

interface TrainingSettingsForm {
  targetRole: string;
  difficulty: InterviewDifficulty;
}

function StartQuestionSessionModal({
  open,
  topic,
  onCancel,
  onSuccess,
}: StartQuestionSessionModalProps) {
  const [form] = Form.useForm<TrainingSettingsForm>();
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) {
      return;
    }
    form.setFieldsValue({
      targetRole: DEFAULT_TARGET_ROLE,
      difficulty: "NORMAL",
    });
  }, [form, open, topic?.id]);

  const handleCancel = () => {
    if (submitting) {
      return;
    }
    onCancel();
  };

  const handleStart = async () => {
    if (!topic) {
      return;
    }

    let values: TrainingSettingsForm;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }

    const targetRole = values.targetRole.trim();
    if (!targetRole) {
      form.setFields([{ name: "targetRole", errors: ["请填写目标岗位"] }]);
      return;
    }

    setSubmitting(true);
    try {
      const data = await createQuestionSession({
        topicId: topic.id,
        targetRole,
        difficulty: values.difficulty,
      });

      if (!data?.sessionId) {
        message.error("创建训练失败，请稍后重试");
        return;
      }

      onSuccess(data.sessionId);
    } catch {
      // The request interceptor shows the concrete business error.
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="训练设置"
      open={open}
      onCancel={handleCancel}
      onOk={handleStart}
      okText="开始训练"
      cancelText="取消"
      okButtonProps={{ loading: submitting }}
      cancelButtonProps={{ disabled: submitting }}
      maskClosable={!submitting}
      destroyOnClose
      className="question-session-modal"
    >
      <div className="question-session-modal__topic">
        <Typography.Text type="secondary">当前知识点</Typography.Text>
        <div className="question-session-modal__title">
          {topic?.name || "—"}
        </div>
        <div className="question-session-modal__meta">
          {topic?.category || "—"}
        </div>
      </div>
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          targetRole: DEFAULT_TARGET_ROLE,
          difficulty: "NORMAL",
        }}
      >
        <Form.Item
          label="目标岗位"
          name="targetRole"
          rules={[{ required: true, message: "请填写目标岗位" }]}
        >
          <Input placeholder="例如：Java 后端实习" />
        </Form.Item>
        <Form.Item
          label="训练难度"
          name="difficulty"
          rules={[{ required: true, message: "请选择训练难度" }]}
        >
          <Radio.Group className="question-session-modal__difficulty-group">
            {DIFFICULTY_OPTIONS.map((option) => (
              <Radio
                key={option.value}
                value={option.value}
                className="question-session-modal__difficulty-option"
              >
                <div className="question-session-modal__difficulty-content">
                  <div className="question-session-modal__difficulty-title">
                    {option.title}
                  </div>
                  <div className="question-session-modal__difficulty-desc">
                    {option.description}
                  </div>
                </div>
              </Radio>
            ))}
          </Radio.Group>
        </Form.Item>
      </Form>
    </Modal>
  );
}

export default StartQuestionSessionModal;
