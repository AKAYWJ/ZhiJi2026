import requests

BASE = "http://localhost:8085"

MOCK_NOTE = """传输层协议主要有 TCP 和 UDP 两种。
TCP 是面向连接的协议，通过三次握手建立连接：客户端发送 SYN，服务端回复 SYN+ACK，客户端再回复 ACK。
TCP 提供可靠传输，使用序号和确认机制保证数据不丢失、不重复、按序到达。
TCP 还有流量控制和拥塞控制，滑动窗口实现流量控制，慢开始、拥塞避免、快重传、快恢复实现拥塞控制。
连接释放需要四次挥手，主动关闭方进入 TIME_WAIT 状态等待 2MSL。
UDP 是无连接的，首部仅 8 字节，适合实时应用如 DNS、视频直播、VoIP。"""

MOCK_NOTES = [
    "传输层核心概念：TCP 面向连接、可靠传输、流量控制、拥塞控制。UDP 无连接、开销小、实时性强。",
    "TCP 三次握手：SYN → SYN+ACK → ACK。四次挥手：FIN → ACK → FIN → ACK，TIME_WAIT 2MSL。",
    "拥塞控制四种算法：慢开始指数增长、拥塞避免线性增长、快重传收到3个重复ACK立即重传、快恢复调整阈值后进入拥塞避免。",
    "UDP 应用场景：DNS 域名解析使用 UDP 端口53、视频直播要求低延迟不重传、VoIP 语音通话容忍少量丢包。"
]


def test_mindmap():
    print("=" * 50)
    print("测试 1: 思维导图生成")
    print("=" * 50)
    r = requests.post(f"{BASE}/mindmap",
                      json={"note_content": MOCK_NOTE},
                      timeout=180)
    print(f"状态码: {r.status_code}")
    if r.status_code != 200:
        print(f"错误: {r.text}")
        return
    mermaid = r.json()["mermaid"]
    print(mermaid[:500])
    if len(mermaid) > 500:
        print("...")


def test_graph():
    print("\n" + "=" * 50)
    print("测试 2: 知识图谱生成")
    print("=" * 50)
    r = requests.post(f"{BASE}/knowledge-graph",
                      json={"note_content": MOCK_NOTE},
                      timeout=180)
    print(f"状态码: {r.status_code}")
    if r.status_code != 200:
        print(f"错误: {r.text}")
        return
    data = r.json()
    print(f"节点数: {len(data.get('nodes', []))}")
    print(f"边数: {len(data.get('edges', []))}")
    print("前3个节点:", data.get("nodes", [])[:3])


def test_exam():
    print("\n" + "=" * 50)
    print("测试 3: 考前突击复习提纲")
    print("=" * 50)
    r = requests.post(f"{BASE}/exam-review",
                      json={
                          "course_name": "计算机网络",
                          "scope": "传输层（第3章）",
                          "focus": "TCP三次握手、四次挥手、拥塞控制",
                          "notes": MOCK_NOTES
                      },
                      timeout=180)
    print(f"状态码: {r.status_code}")
    if r.status_code != 200:
        print(f"错误: {r.text}")
        return
    html = r.json()["html"]
    print(html[:500])
    if len(html) > 500:
        print("...")


if __name__ == "__main__":
    test_mindmap()
    test_graph()
    test_exam()
    print("\n全部测试完成！")
