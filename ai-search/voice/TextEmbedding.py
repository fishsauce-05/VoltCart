"""TextEmbedding: turns a text into a sentence vector with a multilingual model run locally (no paid key)."""
import os
import threading


class TextEmbedding:
    """
    Wraps fastembed's ONNX build of paraphrase-multilingual-MiniLM-L12-v2 (384 dimensions), which understands
    Vietnamese as well as English. The model is downloaded once into models/ and then works offline.
    """

    def __init__(self, modelName="sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2", cacheDir=None):
        self.modelName = modelName
        self._cacheDir = cacheDir or os.path.join(os.path.dirname(os.path.dirname(__file__)), "models")
        self._model = None
        self._lock = threading.Lock()

    def embed(self, text: str) -> list:
        """The normalised vector of the text, as a list of floats."""
        with self._lock:
            if self._model is None:
                from fastembed import TextEmbedding as FastTextEmbedding
                self._model = FastTextEmbedding(self.modelName, cache_dir=self._cacheDir)
            vector = next(iter(self._model.embed([text])))
        norm = float((vector ** 2).sum()) ** 0.5 or 1.0
        return [float(x) / norm for x in vector]

    def modelVersion(self) -> str:
        return self.modelName
