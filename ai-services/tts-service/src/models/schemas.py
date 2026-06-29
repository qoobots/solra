"""Pydantic data models for TTS Service."""

from typing import List, Optional
from enum import Enum
from pydantic import BaseModel, Field


class VoiceOption(str, Enum):
    """Available voice presets."""
    FEMALE_WARM = "female_warm"
    FEMALE_CLEAR = "female_clear"
    MALE_DEEP = "male_deep"
    MALE_BRIGHT = "male_bright"
    CHILD = "child"
    ELDER = "elder"


class AudioFormat(str, Enum):
    """Supported audio output formats."""
    WAV = "wav"
    MP3 = "mp3"
    PCM = "pcm"
    OGG = "ogg"


class TTSRequest(BaseModel):
    """Request for text-to-speech synthesis."""
    text: str = Field(..., min_length=1, max_length=5000, description="Text to synthesize")
    voice: VoiceOption = Field(VoiceOption.FEMALE_WARM, description="Voice preset")
    speed: float = Field(1.0, ge=0.5, le=2.0, description="Speech speed multiplier")
    pitch: float = Field(1.0, ge=0.5, le=2.0, description="Pitch adjustment multiplier")
    format: AudioFormat = Field(AudioFormat.WAV, description="Output audio format")
    sample_rate: int = Field(24000, ge=8000, le=48000, description="Sample rate in Hz")
    stream: bool = Field(False, description="Enable streaming response")


class TTSResponse(BaseModel):
    """Response for text-to-speech synthesis (non-streaming)."""
    audio_data: str = Field(..., description="Base64-encoded audio data")
    format: AudioFormat = Field(..., description="Audio format")
    sample_rate: int = Field(..., description="Sample rate in Hz")
    duration_sec: float = Field(..., description="Audio duration in seconds")
    text_length: int = Field(..., description="Input text length")
    processing_time_ms: float = Field(0.0, description="Processing time in milliseconds")
    model_name: str = Field("", description="TTS model used")


class VoiceInfo(BaseModel):
    """Information about a voice preset."""
    name: str
    voice_id: str
    gender: str
    description: str
    sample_text: str


class ModelStatus(BaseModel):
    """Status of the TTS model."""
    model_name: str
    is_loaded: bool
    device: str
    supported_formats: List[str]
    supported_sample_rates: List[int]
    available_voices: List[VoiceInfo]


class SSEResponse(BaseModel):
    """Server-Sent Event for streaming TTS."""
    chunk_index: int
    audio_chunk: str = Field(..., description="Base64-encoded audio chunk")
    is_final: bool = False
