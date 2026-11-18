"""End-to-end tests of the ai-search endpoints. Small fakes replace the models, so the tests run in seconds."""
import io
import os
import sys

import pytest
from fastapi.testclient import TestClient
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from main import create_app  # noqa: E402


class FakeSpeechToText:
    def transcribe(self, audio, filename):
        return "" if audio == b"silence" else "noise cancelling headphones"


class FakeTextEmbedding:
    def embed(self, text):
        return [float(len(text)), 1.0, 0.0]

    def modelVersion(self):
        return "fake-text"


class FakeImageEncoder:
    def encode(self, image):
        return [0.0, 1.0]

    def modelVersion(self):
        return "fake-image"


class BrokenTextEmbedding(FakeTextEmbedding):
    def embed(self, text):
        raise RuntimeError("model file missing")


@pytest.fixture
def client():
    return TestClient(create_app(FakeSpeechToText(), FakeTextEmbedding(), FakeImageEncoder()))


def png_bytes():
    buf = io.BytesIO()
    Image.new("RGB", (8, 8), (200, 0, 0)).save(buf, "PNG")
    return buf.getvalue()


def test_embed_text_returns_vector_and_model(client):
    r = client.post("/embed/text", json={"text": "gaming laptop"})
    assert r.status_code == 200
    assert r.json() == {"vector": [13.0, 1.0, 0.0], "modelVersion": "fake-text"}


def test_embed_text_rejects_empty_text(client):
    r = client.post("/embed/text", json={"text": "   "})
    assert r.status_code == 400
    assert "empty" in r.json()["error"]


def test_embed_text_rejects_too_long_text(client):
    assert client.post("/embed/text", json={"text": "x" * 501}).status_code == 400


def test_search_voice_returns_transcript_and_vector(client):
    r = client.post("/search/voice", files={"audio": ("q.webm", b"some audio", "audio/webm")})
    assert r.status_code == 200
    body = r.json()
    assert body["transcript"] == "noise cancelling headphones"
    assert body["vector"] == [27.0, 1.0, 0.0]


def test_search_voice_without_speech_is_refused(client):
    r = client.post("/search/voice", files={"audio": ("q.webm", b"silence", "audio/webm")})
    assert r.status_code == 400
    assert "No speech" in r.json()["error"]


def test_embed_image_returns_vector(client):
    r = client.post("/embed/image", files={"image": ("q.png", png_bytes(), "image/png")})
    assert r.status_code == 200
    assert r.json()["vector"] == [0.0, 1.0]


def test_embed_image_rejects_a_file_that_is_not_a_picture(client):
    r = client.post("/embed/image", files={"image": ("q.png", b"not a picture", "image/png")})
    assert r.status_code == 400


def test_model_failure_answers_503_so_the_backend_falls_back():
    broken = TestClient(create_app(FakeSpeechToText(), BrokenTextEmbedding(), FakeImageEncoder()),
                        raise_server_exceptions=False)
    r = broken.post("/embed/text", json={"text": "phone"})
    assert r.status_code == 503


def test_health_names_the_models(client):
    assert client.get("/health").json() == {"status": "ok", "textModel": "fake-text", "imageModel": "fake-image"}
