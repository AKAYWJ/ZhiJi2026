import json
import re
import os
import tempfile
import httpx
from fastapi import FastAPI, HTTPException, File, UploadFile
from pydantic import BaseModel
from config import SILICONFLOW_API_KEY, SILICONFLOW_BASE_URL, MODEL_NAME

app = FastAPI(title="ZhiJI2026 AI Backend")

# ======================== 请求模型 ========================
class MindMapRequest(BaseModel):
    note_content: str

class KnowledgeGraphRequest(BaseModel):
    note_content: str

class ExamReviewRequest(BaseModel):
    course_name: str
    scope: str
    focus: str
    notes: list[str]

class NoteTextRequest(BaseModel):
    content: str
    lecture_title: str = ""


# ======================== 硅基流动 通用调用 ========================
async def call_siliconflow(prompt: str, system_prompt: str = "你是严谨的知识整理专家。",
                           max_tokens: int = 4096) -> str:
    async with httpx.AsyncClient(timeout=600) as client:
        try:
            resp = await client.post(
                f"{SILICONFLOW_BASE_URL}/chat/completions",
                headers={
                    "Authorization": f"Bearer {SILICONFLOW_API_KEY}",
                    "Content-Type": "application/json"
                },
                json={
                    "model": MODEL_NAME,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": prompt}
                    ],
                    "temperature": 0.3,
                    "max_tokens": max_tokens,
                    # "enable_thinking": False
                    "thinking_budget":2048
                }
            )
            if resp.status_code != 200:
                raise HTTPException(status_code=502, detail=f"AI 调用失败: {resp.text}")
            data = resp.json()
            return data["choices"][0]["message"]["content"]
        except httpx.TimeoutException:
            raise HTTPException(status_code=504, detail="AI 响应超时，请重试或缩短笔记内容")

# ======================== 1. 思维导图 ========================
MINDMAP_PROMPT = """请根据以下笔记内容生成思维导图，要求：

1. 输出纯 Mermaid mindmap 语法（不要 markdown 代码块包裹，从 mindmap 关键字开始）
2. 层级 3~5 层，核心概念为根节点
3. 子节点用简练关键词，不用长句
4. 格式示例：

mindmap
  root((根主题))
    分支1
      子点A
      子点B
    分支2
      子点C

笔记内容：
{content}"""

@app.post("/mindmap")
async def generate_mindmap(req: MindMapRequest):
    prompt = MINDMAP_PROMPT.format(content=req.note_content)
    result = await call_siliconflow(prompt)
    # 清洗：去掉可能的 markdown 代码块包裹
    result = result.strip()
    if result.startswith("```"):
        lines = result.split("\n")
        result = "\n".join(lines[1:-1]) if len(lines) > 2 else result
    return {"mermaid": result}

# ======================== 2. 知识图谱 ========================
GRAPH_PROMPT = """根据以下笔记生成知识图谱 JSON。严格输出一行紧凑 JSON，不要任何其他文字。

格式：{{"nodes":[{{"id":"0","label":"根","group":0}}],"edges":[{{"source":"0","target":"1","label":"关系"}}]}}

规则：8~12个节点，10~18条边。group: 0根/1核心/2子概念/3细节。label≤5字。

笔记：
{content}"""


@app.post("/knowledge-graph")
async def generate_graph(req: KnowledgeGraphRequest):
    prompt = GRAPH_PROMPT.format(content=req.note_content)
    result = await call_siliconflow(prompt, max_tokens=20480)
    raw = result.strip()
    print(f"[knowledge-graph] 原始返回（前300字）:\n{raw[:300]}")
    
    # 1. 去掉 markdown 代码块
    if "```" in raw:
        lines = raw.split("\n")
        cleaned = []
        for line in lines:
            if "```" in line:
                continue
            cleaned.append(line)
        raw = "\n".join(cleaned).strip()
    
    # 2. 提取 { 到 } 区间
    start = raw.find("{")
    end = raw.rfind("}")
    if start == -1 or end == -1 or end <= start:
        raise HTTPException(status_code=500, detail=f"AI 未返回有效 JSON。原始: {raw[:300]}")
    raw = raw[start:end+1]
    
    # 3. 尝试解析，失败则清洗常见问题后重试
    try:
        graph_data = json.loads(raw)
    except json.JSONDecodeError as e:
        # 去掉可能的尾逗号
        import re
        fixed = re.sub(r',(\s*[}\]])', r'\1', raw)
        try:
            graph_data = json.loads(fixed)
        except json.JSONDecodeError:
            raise HTTPException(status_code=500, detail=f"AI 返回格式异常: {e}\n原始: {raw[:300]}")
    return graph_data

# ======================== 3. 考前突击 ========================
EXAM_PROMPT = """请根据以下课程笔记生成考前突击复习提纲，要求：

1. 输出 HTML 片段（<div> 包裹，不要 <html><body> 标签）
2. 结构：
   - <h2> 课程名 - 复习提纲
   - 考试范围：xxx / 重点关注：xxx
   - <h3> 核心考点（3~5个，用 <ul> 列出）
   - <h3> 高频易错（3~5个，用 <ul> 列出）
   - <h3> 知识框架梳理（用表格或列表总结）
   - <h3> 考前建议（3 条简洁建议）
3. 风格：紧凑、适合手机阅读、用颜色区分重点（红色#D32F2F标注必考点）

课程：{course_name}
考试范围：{scope}
重点关注：{focus}

笔记内容：
{notes}"""

@app.post("/exam-review")
async def generate_exam_review(req: ExamReviewRequest):
    notes_text = "\n\n---\n\n".join(
        f"笔记{i+1}：{n}" for i, n in enumerate(req.notes)
    )
    prompt = EXAM_PROMPT.format(
        course_name=req.course_name,
        scope=req.scope,
        focus=req.focus,
        notes=notes_text
    )
    result = await call_siliconflow(prompt)
    # 清洗：提取 div 内容
    result = result.strip()
    if "```" in result:
        lines = result.split("\n")
        result = "\n".join(l for l in lines if not l.startswith("```"))
    # 如果 AI 返回了完整 HTML，只取 body 部分
    if "<body>" in result:
        start = result.find("<body>") + 6
        end = result.find("</body>")
        result = result[start:end] if end > 0 else result[start:]
    return {"html": result.strip()}


# ======================== 4. 笔记生成（纯文本） ========================
NOTE_PROMPT = """请将以下课程笔记原始内容整理为结构化的 Markdown 笔记，要求：

1. 用 # 标题、## 子标题、### 三级标题分层
2. 核心概念用 **加粗** 标注
3. 知识点用 - 无序列表
4. 关键流程/步骤用 1. 2. 3. 有序列表
5. 重要公式或代码用 ``` 包裹
6. 补充相关知识点用 > 引用块
7. 末尾加 ## 小结，用 3~5 句总结核心要点

课程：{title}

原始内容：
{content}"""

@app.post("/note")
async def generate_note(req: NoteTextRequest):
    prompt = NOTE_PROMPT.format(
        title=req.lecture_title or "课程笔记",
        content=req.content
    )
    result = await call_siliconflow(prompt)
    result = result.strip()
    if result.startswith("```") and result.endswith("```"):
        lines = result.split("\n")
        result = "\n".join(lines[1:-1])
    return {"markdown": result}


# ======================== 请求模型 ========================
class NoteGenerateRequest(BaseModel):
    content: str
    lecture_title: str = "课程笔记"

# ======================== 5. 录音 → 笔记（ASR + LLM） ========================
async def speech_to_text(file_path: str) -> str:
    import mimetypes
    # 根据扩展名自动推断 MIME
    mime_type, _ = mimetypes.guess_type(file_path)
    if mime_type is None:
        mime_type = "audio/mpeg"  # 兜底

    async with httpx.AsyncClient(timeout=600) as client:
        with open(file_path, "rb") as f:
            resp = await client.post(
                f"{SILICONFLOW_BASE_URL}/audio/transcriptions",
                headers={"Authorization": f"Bearer {SILICONFLOW_API_KEY}"},
                files={"file": (os.path.basename(file_path), f, mime_type)},
                data={"model": "TeleAI/TeleSpeechASR", "language": "zh"}
            )
        if resp.status_code != 200:
            raise HTTPException(status_code=502, detail=f"ASR 失败: {resp.text}")
        return resp.json().get("text", "")


@app.post("/asr")
async def asr_transcribe(audio_file: UploadFile = File(...)):
    suffix = os.path.splitext(audio_file.filename)[1] or ".m4a"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        content = await audio_file.read()
        tmp.write(content)
        tmp_path = tmp.name
    try:
        transcript = await speech_to_text(tmp_path)
        if not transcript.strip():
            raise HTTPException(status_code=400, detail="语音识别结果为空")
        return {"transcript": transcript}
    finally:
        os.unlink(tmp_path)


@app.post("/note/generate")
async def generate_note_from_transcript(req: NoteGenerateRequest):
    prompt = NOTE_PROMPT.format(
        title=req.lecture_title,
        content=req.content
    )
    result = await call_siliconflow(prompt)
    result = result.strip()
    if result.startswith("```") and result.endswith("```"):
        lines = result.split("\n")
        result = "\n".join(lines[1:-1])
    return {"markdown": result}


@app.post("/note/from-audio")
async def generate_note_from_audio(
    lecture_title: str = "课程笔记",
    audio_file: UploadFile = File(...)
):
    suffix = os.path.splitext(audio_file.filename)[1] or ".m4a"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        content = await audio_file.read()
        tmp.write(content)
        tmp_path = tmp.name

    file_size_mb = len(content) / (1024 * 1024)
    print(f"[audio] 收到文件: {audio_file.filename}, 大小: {file_size_mb:.1f}MB")

    try:
        print("[audio] 开始 ASR 语音识别...")
        transcript = await speech_to_text(tmp_path)
        print(f"[audio] ASR 完成, 文本长度: {len(transcript)}")

        if not transcript.strip():
            raise HTTPException(status_code=400, detail="语音识别结果为空，请确认录音内容")

        print("[audio] 开始 LLM 整理笔记...")
        prompt = NOTE_PROMPT.format(title=lecture_title, content=transcript)
        markdown = await call_siliconflow(prompt)
        markdown = markdown.strip()
        print(f"[audio] LLM 完成, markdown长度: {len(markdown)}")

        if markdown.startswith("```") and markdown.endswith("```"):
            lines = markdown.split("\n")
            markdown = "\n".join(lines[1:-1])

        return {"transcript": transcript, "markdown": markdown}
    except HTTPException:
        raise
    except Exception as e:
        print(f"[audio] 异常: {type(e).__name__}: {e}")
        raise HTTPException(status_code=504, detail=f"处理超时或失败: {type(e).__name__}")
    finally:
        os.unlink(tmp_path)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8085)
