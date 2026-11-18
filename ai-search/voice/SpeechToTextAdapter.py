"""SpeechToTextAdapter: transcribes a short spoken query with Whisper, run locally through faster-whisper."""
import os
import tempfile
import threading


class SpeechToTextAdapter:
    """
    Adapter in front of the speech model, so another engine can replace Whisper without touching the services.
    The "base" model (about 140 MB) runs on the CPU with 8-bit weights; language None lets Whisper detect
    Vietnamese or English by itself.
    """

    def __init__(self, modelName="base", language=None, cacheDir=None):
        self.modelName = modelName
        self.language = language
        self._cacheDir = cacheDir or os.path.join(os.path.dirname(os.path.dirname(__file__)), "models")
        self._model = None
        self._lock = threading.Lock()

    def transcribe(self, audio: bytes, filename: str) -> str:
        """The recognised text of the recording; any format FFmpeg reads (webm, ogg, wav, mp3) is accepted."""
        suffix = os.path.splitext(filename or "")[1] or ".webm"
        fd, path = tempfile.mkstemp(suffix=suffix)
        try:
            with os.fdopen(fd, "wb") as f:
                f.write(audio)
            with self._lock:
                if self._model is None:
                    from faster_whisper import WhisperModel
                    self._model = WhisperModel(self.modelName, device="cpu", compute_type="int8",
                                               download_root=self._cacheDir)
                segments, _info = self._model.transcribe(path, language=self.language, beam_size=1,
                                                         vad_filter=True)
                text = " ".join(s.text.strip() for s in segments)
        finally:
            os.remove(path)
        return text.strip()
