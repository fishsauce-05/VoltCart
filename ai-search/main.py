"""Starts ai-search. The concrete adapters are chosen here, and only here (Figure 4.6).

run:  uvicorn main:app --port 8000
"""
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from SearchController import SearchController
from image.ImageEncoder import ImageEncoder
from image.ImageSearchService import ImageSearchService
from voice.SpeechToTextAdapter import SpeechToTextAdapter
from voice.TextEmbedding import TextEmbedding
from voice.VoiceSearchService import VoiceSearchService


def create_app(speechToText=None, textEmbedding=None, imageEncoder=None) -> FastAPI:
    """Builds the application; tests pass small fakes instead of the real models."""
    textEmbedding = textEmbedding or TextEmbedding()
    controller = SearchController(
        VoiceSearchService(speechToText or SpeechToTextAdapter(), textEmbedding),
        ImageSearchService(imageEncoder or ImageEncoder()))
    app = FastAPI(title="VoltCart ai-search", version="1.0.0")
    app.post("/embed/text")(controller.embedText)
    app.post("/search/voice")(controller.searchVoice)
    app.post("/embed/image")(controller.embedImage)
    app.get("/health")(controller.health)

    @app.exception_handler(ValueError)
    async def bad_input(_request: Request, error: ValueError):
        return JSONResponse(status_code=400, content={"error": str(error)})

    @app.exception_handler(Exception)
    async def model_failure(_request: Request, error: Exception):
        return JSONResponse(status_code=503, content={"error": f"Model unavailable: {type(error).__name__}"})

    return app


app = create_app()
