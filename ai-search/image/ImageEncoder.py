"""ImageEncoder: turns a picture into a CLIP image vector, run locally."""
import io
import os
import threading

from PIL import Image


class ImageEncoder:
    """Wraps fastembed's ONNX build of the CLIP ViT-B/32 vision tower (512 dimensions)."""

    def __init__(self, modelName="Qdrant/clip-ViT-B-32-vision", cacheDir=None):
        self.modelName = modelName
        self._cacheDir = cacheDir or os.path.join(os.path.dirname(os.path.dirname(__file__)), "models")
        self._model = None
        self._lock = threading.Lock()

    def encode(self, image: bytes) -> list:
        picture = Image.open(io.BytesIO(image)).convert("RGB")
        with self._lock:
            if self._model is None:
                from fastembed import ImageEmbedding
                self._model = ImageEmbedding(self.modelName, cache_dir=self._cacheDir)
            vector = next(iter(self._model.embed([picture])))
        norm = float((vector ** 2).sum()) ** 0.5 or 1.0
        return [float(x) / norm for x in vector]

    def modelVersion(self) -> str:
        return self.modelName
