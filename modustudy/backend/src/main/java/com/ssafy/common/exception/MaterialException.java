package com.ssafy.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 자료실 관련 예외 클래스
 */
public class MaterialException {

    /**
     * 자료를 찾을 수 없음 (404)
     */
    public static class MaterialNotFoundException extends BusinessException {
        public MaterialNotFoundException(Long materialId) {
            super(HttpStatus.NOT_FOUND, "MATERIAL_NOT_FOUND",
                    "존재하지 않는 자료입니다: " + materialId);
        }

        public MaterialNotFoundException() {
            super(HttpStatus.NOT_FOUND, "MATERIAL_NOT_FOUND",
                    "자료를 찾을 수 없습니다");
        }
    }

    /**
     * 자료 댓글을 찾을 수 없음 (404)
     */
    public static class MaterialCommentNotFoundException extends BusinessException {
        public MaterialCommentNotFoundException(Long commentId) {
            super(HttpStatus.NOT_FOUND, "MATERIAL_COMMENT_NOT_FOUND",
                    "존재하지 않는 댓글입니다: " + commentId);
        }

        public MaterialCommentNotFoundException() {
            super(HttpStatus.NOT_FOUND, "MATERIAL_COMMENT_NOT_FOUND",
                    "댓글을 찾을 수 없습니다");
        }
    }

    /**
     * 자료 소유자가 아님 (403)
     */
    public static class NotMaterialOwnerException extends BusinessException {
        public NotMaterialOwnerException() {
            super(HttpStatus.FORBIDDEN, "NOT_MATERIAL_OWNER",
                    "본인이 업로드한 자료만 수정/삭제할 수 있습니다");
        }

        public NotMaterialOwnerException(String message) {
            super(HttpStatus.FORBIDDEN, "NOT_MATERIAL_OWNER", message);
        }
    }

    /**
     * 댓글 작성자가 아님 (403)
     */
    public static class NotCommentAuthorException extends BusinessException {
        public NotCommentAuthorException() {
            super(HttpStatus.FORBIDDEN, "NOT_COMMENT_AUTHOR",
                    "본인이 작성한 댓글만 삭제할 수 있습니다");
        }
    }

    /**
     * 파일 크기 초과 (400)
     */
    public static class FileSizeExceededException extends BusinessException {
        public FileSizeExceededException(long maxSize) {
            super(HttpStatus.BAD_REQUEST, "FILE_SIZE_EXCEEDED",
                    "파일 크기가 최대 허용 크기를 초과했습니다: " + (maxSize / 1024 / 1024) + "MB");
        }

        public FileSizeExceededException() {
            super(HttpStatus.BAD_REQUEST, "FILE_SIZE_EXCEEDED",
                    "파일 크기가 최대 허용 크기를 초과했습니다");
        }
    }

    /**
     * 지원하지 않는 파일 형식 (400)
     */
    public static class InvalidFileTypeException extends BusinessException {
        public InvalidFileTypeException(String extension) {
            super(HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE",
                    "지원하지 않는 파일 형식입니다: " + extension);
        }

        public InvalidFileTypeException() {
            super(HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE",
                    "지원하지 않는 파일 형식입니다");
        }
    }

    /**
     * 파일 저장 실패 (500)
     */
    public static class FileStorageException extends BusinessException {
        public FileStorageException(String message) {
            super(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_STORAGE_ERROR", message);
        }

        public FileStorageException() {
            super(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_STORAGE_ERROR",
                    "파일 저장 중 오류가 발생했습니다");
        }
    }

    /**
     * 파일 삭제 실패 (500)
     */
    public static class FileDeletionException extends BusinessException {
        public FileDeletionException(String message) {
            super(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_DELETION_ERROR", message);
        }

        public FileDeletionException() {
            super(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_DELETION_ERROR",
                    "파일 삭제 중 오류가 발생했습니다");
        }
    }
}