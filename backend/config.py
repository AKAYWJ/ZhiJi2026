import os
from dotenv import load_dotenv

load_dotenv()

SILICONFLOW_API_KEY = os.getenv("SILICONFLOW_API_KEY", "")
SILICONFLOW_BASE_URL = "https://api.siliconflow.cn/v1"
MODEL_NAME = "deepseek-ai/DeepSeek-R1-0528-Qwen3-8B"

"""
Qwen/Qwen3-8B
Qwen/Qwen2.5-7B-Instruct
deepseek-ai/DeepSeek-R1-0528-Qwen3-8B
nex-agi/Nex-N2-Pro
"""

"""
ASR：
FunAudioLLM/SenseVoiceSmal
TeleAI/TeleSpeechASR

"""

if not SILICONFLOW_API_KEY:
    raise RuntimeError("请设置 SILICONFLOW_API_KEY 环境变量或在 .env 文件中配置")
