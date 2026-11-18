"""VoiceSearchService: a spoken or typed query becomes a text vector."""


class VoiceSearchService:
    MAX_TEXT = 500

    def __init__(self, speechToText, textEmbedding):
        self.speechToText = speechToText
        self.textEmbedding = textEmbedding

    def searchVoice(self, audio: bytes, filename: str) -> dict:
        """Transcribes the recording and encodes the transcript; the transcript is returned for the customer to check."""
        if not audio:
            raise ValueError("The recording is empty")
        transcript = self.speechToText.transcribe(audio, filename)
        if not transcript:
            raise ValueError("No speech was recognised; please try again or type the query")
        return {"transcript": transcript, "vector": self.textEmbedding.embed(transcript),
                "modelVersion": self.textEmbedding.modelVersion()}

    def embedText(self, text: str) -> list:
        text = (text or "").strip()
        if not text:
            raise ValueError("The text is empty")
        if len(text) > self.MAX_TEXT:
            raise ValueError(f"The text is limited to {self.MAX_TEXT} characters")
        return self.textEmbedding.embed(text)
