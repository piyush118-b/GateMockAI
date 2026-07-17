"""
MinIO Client — handles all object storage operations for the extraction service.

Object key convention:
  papers/{paper_id}/questions/{question_id}/{filename}
  papers/{paper_id}/questions/{question_id}/options/{label}/{filename}
  papers/{paper_id}/original_paper.pdf
  papers/{paper_id}/answer_key.pdf
"""

import os
import hashlib
import io
import logging
from minio import Minio
from minio.error import S3Error

logger = logging.getLogger(__name__)

MINIO_ENDPOINT   = os.getenv("MINIO_ENDPOINT", "http://minio:9000").replace("http://", "")
MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY", "minioadmin")
MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY", "minioadmin")
MINIO_BUCKET     = os.getenv("MINIO_BUCKET", "gate-assets")
MINIO_SECURE     = os.getenv("MINIO_ENDPOINT", "http://minio:9000").startswith("https://")


def get_client() -> Minio:
    """Returns a configured MinIO client."""
    return Minio(
        MINIO_ENDPOINT,
        access_key=MINIO_ACCESS_KEY,
        secret_key=MINIO_SECRET_KEY,
        secure=MINIO_SECURE,
    )


def ensure_bucket(client: Minio, bucket: str = MINIO_BUCKET) -> None:
    """Creates the bucket if it doesn't exist."""
    try:
        if not client.bucket_exists(bucket):
            client.make_bucket(bucket)
            logger.info(f"Created MinIO bucket: {bucket}")
    except S3Error as e:
        logger.error(f"Failed to ensure bucket {bucket}: {e}")
        raise


def upload_image(
    image_bytes: bytes,
    object_key: str,
    mime_type: str = "image/png",
    bucket: str = MINIO_BUCKET,
) -> dict:
    """
    Uploads image bytes to MinIO.

    Returns a dict with:
      - object_key
      - mime_type
      - checksum (SHA-256 hex)
      - size (bytes)
    """
    client = get_client()
    ensure_bucket(client, bucket)

    checksum = hashlib.sha256(image_bytes).hexdigest()
    data_stream = io.BytesIO(image_bytes)
    size = len(image_bytes)

    try:
        client.put_object(
            bucket_name=bucket,
            object_name=object_key,
            data=data_stream,
            length=size,
            content_type=mime_type,
        )
        logger.debug(f"Uploaded {size} bytes to minio://{bucket}/{object_key}")
        return {
            "object_key": object_key,
            "mime_type": mime_type,
            "checksum": checksum,
            "size": size,
        }
    except S3Error as e:
        logger.error(f"MinIO upload failed for {object_key}: {e}")
        raise


def upload_pdf(pdf_bytes: bytes, object_key: str, bucket: str = MINIO_BUCKET) -> str:
    """Uploads a raw PDF to MinIO and returns its object key."""
    result = upload_image(pdf_bytes, object_key, mime_type="application/pdf", bucket=bucket)
    return result["object_key"]


def question_image_key(paper_id: str, question_id: str, filename: str) -> str:
    return f"papers/{paper_id}/questions/{question_id}/{filename}"


def option_image_key(paper_id: str, question_id: str, option_label: str, filename: str) -> str:
    return f"papers/{paper_id}/questions/{question_id}/options/{option_label}/{filename}"


def paper_pdf_key(paper_id: str, filename: str) -> str:
    return f"papers/{paper_id}/{filename}"
