import { useEffect, useState } from "react";
import ProductHeader from "../../components/ProductHeader";
import type { CurrentUser } from "../../types/auth";
import "./index.css";

function readStoredUser(): CurrentUser | null {
  const storedUser = localStorage.getItem("user");
  if (!storedUser) {
    return null;
  }
  try {
    return JSON.parse(storedUser) as CurrentUser;
  } catch {
    localStorage.removeItem("user");
    return null;
  }
}

function LandingPage() {
  const [user, setUser] = useState<CurrentUser | null>(() => readStoredUser());

  useEffect(() => {
    const handleUserUpdated = () => {
      setUser(readStoredUser());
    };
    handleUserUpdated();
    window.addEventListener("codecoach:user-updated", handleUserUpdated);
    return () => {
      window.removeEventListener("codecoach:user-updated", handleUserUpdated);
    };
  }, []);

  return (
    <div className="landing-page">
      <ProductHeader user={user} onLogout={() => setUser(null)} />

      <main>
        <section className="landing-hero">
          <div className="landing-hero__eyebrow">为 Java 后端候选人设计</div>
          <h1 className="landing-hero__title">
            把你的技术实力
            <br />
            讲得<em>精准而可信。</em>
          </h1>
          <p className="landing-hero__subtitle">
            一个面向后端面试的 AI 训练空间，围绕项目架构、JVM、并发、
            数据库、缓存和系统设计进行连续追问，让表达和技术深度一起变扎实。
          </p>

          <div className="landing-preview-card">
            <div className="landing-preview-card__chrome">
              <span />
              <span />
              <span />
              <strong>训练会话 092 // 项目拷打</strong>
            </div>
            <div className="landing-preview-card__body">
              <div className="preview-row">
                <div className="preview-icon">□</div>
                <div>
                  <div className="preview-label">AI 面试官</div>
                  <p className="preview-question">
                    “你提到用 Redis 做分布式锁。请你解释一个失败场景：
                    如果加锁成功后服务发生长时间停顿，锁过期又被其他请求获取，
                    你如何避免并发写入导致的数据不一致？”
                  </p>
                </div>
              </div>
              <div className="preview-row preview-row--candidate">
                <div className="preview-avatar" />
                <div>
                  <div className="preview-label">候选人回答</div>
                  <div className="preview-answer">
                    “我会设置锁过期时间，并结合唯一标识释放锁；如果业务执行时间不可控，
                    还需要考虑续期机制和幂等校验。”
                  </div>
                </div>
              </div>
              <div className="preview-row">
                <div className="preview-icon">□</div>
                <div>
                  <div className="preview-label">AI 反馈</div>
                  <p className="preview-feedback">
                    方向正确，但还需要补充“为什么”：说明 Lua 原子释放、
                    续期失败的边界条件，以及什么时候应该换成更强一致的协调方案。
                  </p>
                  <div className="preview-structure">
                    <span>参考表达结构</span>
                    <ul>
                      <li>业务背景：高并发下的互斥写入。</li>
                      <li>实现机制：唯一标识、过期时间、原子释放。</li>
                      <li>异常场景：长时间停顿、续期失败和重复请求。</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="landing-split">
          <div>
            <h2>很多技术能力，输在讲不清楚。</h2>
            <p>
              做出高并发系统已经很难，在面试压力下把它讲清楚更难。
              CodeCoach AI 提供一个可重复训练的环境，让你提前练习真实面试里的追问、
              反问和技术取舍表达。
            </p>
          </div>
          <div className="landing-pain-cards">
            <article>
              <span>◇</span>
              <h3>架构表达断层</h3>
              <p>
                项目做过，但解释不清为什么这样拆模块、为什么选 MySQL、
                Redis 或消息队列，回答容易停留在“我用了什么”。
              </p>
            </article>
            <article>
              <span>⌂</span>
              <h3>八股追问失速</h3>
              <p>
                平时知道概念，但一到线程池参数、JVM 内存、事务隔离级别等连续追问，
                表达就开始碎片化。
              </p>
            </article>
          </div>
        </section>

        <section className="landing-capabilities">
          <div className="landing-section-heading">
            <span>训练能力</span>
            <h2>用更安静、更系统的方式准备硬核面试。</h2>
          </div>
          <div className="capability-grid">
            <article>
              <p>01</p>
              <h3>项目拷打训练</h3>
              <span>
                基于项目档案持续追问架构设计、异常场景、技术取舍和实现细节。
              </span>
            </article>
            <article>
              <p>02</p>
              <h3>八股问答训练</h3>
              <span>
                围绕 Java 基础、JVM、并发、MySQL、Redis、Spring、消息队列和分布式主题训练表达。
              </span>
            </article>
            <article>
              <p>03</p>
              <h3>结构化复盘报告</h3>
              <span>
                将粗糙回答转成可执行的改进地图：分数、薄弱点、优势和参考表达结构。
              </span>
            </article>
          </div>
        </section>
      </main>

      <footer className="landing-footer">
        <div className="landing-footer__brand">
          <span className="landing-footer__mark">◇</span>
          <strong>CodeCoach AI</strong>
        </div>
        <nav>
          <a href="/">Product</a>
          <a href="/">Privacy</a>
          <a href="/">Contact</a>
        </nav>
        <p>Built for Java backend candidates.</p>
      </footer>
    </div>
  );
}

export default LandingPage;
