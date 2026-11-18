"""SearchController: the HTTP endpoints of ai-search. It reads requests and calls the two services."""
from fastapi import UploadFile


class SearchController:
    def __init__(self, voiceSearchService, imageSearchService):
        self.voiceSearchService = voiceSearchService
        self.imageSearchService = imageSearchService

    def embedText(self, request: dict) -> dict:
        """POST /embed/text {"text": ...}: the vector of a typed query or of a product's text."""
        vector = self.voiceSearchService.embedText(str(request.get("text", "")))
        return {"vector": vector, "modelVersion": self.voiceSearchService.textEmbedding.modelVersion()}

    async def searchVoice(self, audio: UploadFile) -> dict:
        """POST /search/voice (multipart "audio"): transcript and vector of a spoken query."""
        return self.voiceSearchService.searchVoice(await audio.read(), audio.filename or "audio.webm")

    async def embedImage(self, image: UploadFile) -> dict:
        """POST /embed/image (multipart "image"): the vector of a query photo or of a product picture."""
        vector = self.imageSearchService.embedImage(await image.read())
        return {"vector": vector, "modelVersion": self.imageSearchService.imageEncoder.modelVersion()}

    def health(self) -> dict:
        return {"status": "ok",
                "textModel": self.voiceSearchService.textEmbedding.modelVersion(),
                "imageModel": self.imageSearchService.imageEncoder.modelVersion()}
