"""ImageSearchService: checks an uploaded picture and returns its image vector."""
import io

from PIL import Image, UnidentifiedImageError


class ImageSearchService:
    def __init__(self, imageEncoder, maxBytes=5 * 1024 * 1024):
        self.imageEncoder = imageEncoder
        self.maxBytes = maxBytes

    def embedImage(self, image: bytes) -> list:
        if not image:
            raise ValueError("The picture is empty")
        if len(image) > self.maxBytes:
            raise ValueError("The picture is larger than 5 MB")
        try:
            Image.open(io.BytesIO(image)).verify()
        except (UnidentifiedImageError, OSError):
            raise ValueError("The file is not a picture that can be read")
        return self.imageEncoder.encode(image)
