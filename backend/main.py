import librosa
import numpy as np
import os

def analyze_audio_features(audio_file_path: str) -> dict:
    """
    Extracts acoustic & prosodic features from an audio file:
    1. Pitch Variability (Standard Deviation of F0)
    2. Speech vs. Pause Ratios
    """
    if not os.path.exists(audio_file_path):
        raise FileNotFoundError(f"Audio file not found at {audio_file_path}")

    # Load audio file (resampled automatically)
    y, sr = librosa.load(audio_file_path, sr=None)
    
    # 1. Pitch Extraction (Fundamental Frequency - F0)
    pitches, magnitudes = librosa.piptrack(y=y, sr=sr)
    f0 = pitches[pitches > 0]
    pitch_variability = float(np.std(f0)) if len(f0) > 0 else 0.0
    
    # 2. Pause and Speech Duration Detection
    # Splits audio where sound drops below 25 dB relative to max
    non_silent_intervals = librosa.effects.split(y, top_db=25)
    total_duration = len(y) / sr
    speech_duration = sum([(end - start) for start, end in non_silent_intervals]) / sr
    pause_duration = total_duration - speech_duration
    pause_ratio = pause_duration / total_duration if total_duration > 0 else 0.0

    return {
        "total_duration_sec": round(total_duration, 2),
        "speech_duration_sec": round(speech_duration, 2),
        "pause_duration_sec": round(pause_duration, 2),
        "pause_ratio": round(pause_ratio, 4),
        "pitch_std_dev": round(pitch_variability, 4)
    }

if __name__ == "__main__":
    print("Audio processor initialized successfully!")
