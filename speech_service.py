"""TTS service helpers used by app_fall_detection.py"""

from __future__ import annotations

import asyncio
import base64
import os
import tempfile
import threading

import edge_tts

DEFAULT_TTS_VOICE = "zh-CN-XiaoxiaoNeural"

tts_lock = threading.Lock()


def _to_edge_rate(value: float) -> str:
    """Map UI slider 0.5~2.0 to edge-tts rate (-50% ~ +100%)."""
    value = max(0.5, min(2.0, float(value)))
    pct = int(round((value - 1.0) * 100))
    pct = max(-50, min(100, pct))
    return f"{pct:+d}%"


def synthesize_speech_base64(
    text: str,
    rate: float = 1.0,
    voice: str = DEFAULT_TTS_VOICE,
) -> str:
    text = (text or "").strip()
    if not text:
        raise ValueError("text is required")

    mp3_path = None
    with tts_lock:
        fd, mp3_path = tempfile.mkstemp(suffix=".mp3")
        os.close(fd)
        try:
            rate_str = _to_edge_rate(rate)

            async def _generate() -> None:
                communicate = edge_tts.Communicate(text, voice, rate=rate_str)
                await communicate.save(mp3_path)

            loop = asyncio.new_event_loop()
            try:
                loop.run_until_complete(_generate())
            finally:
                loop.close()

            with open(mp3_path, "rb") as f:
                audio_bytes = f.read()
            if not audio_bytes:
                raise RuntimeError("TTS 生成了空音频")

            encoded = base64.b64encode(audio_bytes).decode("ascii")
            return f"data:audio/mpeg;base64,{encoded}"
        finally:
            if mp3_path and os.path.exists(mp3_path):
                try:
                    os.remove(mp3_path)
                except OSError:
                    pass
