package com.ssafy.domain.meeting.service;

import com.ssafy.common.storage.LocalFileStorageService;
import com.ssafy.domain.meeting.dto.response.MeetingPhotoResponse;
import com.ssafy.domain.meeting.entity.MeetingPhoto;
import com.ssafy.domain.meeting.repository.MeetingPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MeetingPhotoService {

    private final MeetingPhotoRepository meetingPhotoRepository;
    private final LocalFileStorageService localFileStorageService;
    private final MeetingServiceHelper helper;

    @Transactional(readOnly = true)
    public List<MeetingPhotoResponse> getPhotos(Long studyId, Long meetingId, Long userId) {
        helper.getMeetingOrThrow(studyId, meetingId);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
        }
        return meetingPhotoRepository.findByMeetingIdAndUserIdOrderByCapturedAtDesc(meetingId, userId).stream()
                .map(photo -> new MeetingPhotoResponse(
                        photo.getId(),
                        photo.getImageUrl(),
                        photo.getCapturedAt(),
                        photo.getIsSelected()))
                .toList();
    }

    @Transactional
    public MeetingPhotoResponse addPhoto(Long studyId, Long meetingId, Long userId, MultipartFile image) {
        helper.getMeetingOrThrow(studyId, meetingId);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
        }
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMAGE_REQUIRED");
        }
        String imageUrl = localFileStorageService.saveMeetingPhoto(meetingId, image);
        MeetingPhoto saved = meetingPhotoRepository.save(
                MeetingPhoto.capture(meetingId, userId, imageUrl, LocalDateTime.now()));
        return new MeetingPhotoResponse(saved.getId(), saved.getImageUrl(), saved.getCapturedAt(), saved.getIsSelected());
    }

    @Transactional
    public MeetingPhotoResponse selectPhoto(Long studyId, Long meetingId, Long userId, Long photoId) {
        helper.getMeetingOrThrow(studyId, meetingId);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
        }
        MeetingPhoto target = meetingPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PHOTO_NOT_FOUND"));
        if (!target.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PHOTO_MEETING_MISMATCH");
        }
        if (target.getUserId() == null || !target.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PHOTO_FORBIDDEN");
        }
        List<MeetingPhoto> photos = meetingPhotoRepository.findByMeetingIdAndUserIdOrderByCapturedAtDesc(meetingId, userId);
        for (MeetingPhoto photo : photos) {
            photo.updateSelected(photo.getId().equals(photoId));
        }
        meetingPhotoRepository.saveAll(photos);
        return new MeetingPhotoResponse(target.getId(), target.getImageUrl(), target.getCapturedAt(), true);
    }

    @Transactional
    public List<MeetingPhotoResponse> selectPhotos(Long studyId, Long meetingId, Long userId, List<Long> photoIds) {
        helper.getMeetingOrThrow(studyId, meetingId);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
        }
        List<MeetingPhoto> photos = meetingPhotoRepository.findByMeetingIdAndUserIdOrderByCapturedAtDesc(meetingId, userId);
        Set<Long> selectedIds = photoIds == null ? Set.of() : new HashSet<>(photoIds);
        for (MeetingPhoto photo : photos) {
            photo.updateSelected(selectedIds.contains(photo.getId()));
        }
        meetingPhotoRepository.saveAll(photos);
        return photos.stream()
                .map(photo -> new MeetingPhotoResponse(
                        photo.getId(),
                        photo.getImageUrl(),
                        photo.getCapturedAt(),
                        photo.getIsSelected()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeetingPhoto> getSelectedPhotos(Long meetingId) {
        return meetingPhotoRepository.findByMeetingIdAndIsSelectedTrueOrderByCapturedAtDesc(meetingId);
    }
}
