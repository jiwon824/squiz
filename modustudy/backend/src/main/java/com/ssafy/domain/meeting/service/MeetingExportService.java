package com.ssafy.domain.meeting.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.ssafy.common.storage.LocalFileStorageService;
import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingPhoto;
import com.ssafy.domain.meeting.entity.MeetingSttFile;
import com.ssafy.domain.meeting.entity.MeetingSttSummary;
import com.ssafy.domain.meeting.entity.MeetingTextTrackType;
import com.ssafy.domain.meeting.repository.MeetingPhotoRepository;
import com.ssafy.domain.meeting.repository.MeetingSttFileRepository;
import com.ssafy.domain.meeting.repository.MeetingSttSummaryRepository;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingExportService {

    private final MeetingPhotoRepository meetingPhotoRepository;
    private final MeetingSttFileRepository meetingSttFileRepository;
    private final MeetingSttSummaryRepository meetingSttSummaryRepository;
    private final StudyRepository studyRepository;
    private final StudySessionRepository studySessionRepository;
    private final LocalFileStorageService localFileStorageService;
    private final MeetingServiceHelper helper;

    @Value("${meeting.pdf.font-path:}")
    private String pdfFontPath;

    @Transactional(readOnly = true)
    public String exportMeetingMarkdown(Long studyId, Long meetingId) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        Study study = studyRepository.findById(studyId).orElse(null);
        MeetingSttSummary summary = meetingSttSummaryRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElse(null);
        MeetingSttFile transcriptFile = meetingSttFileRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElse(null);
        List<MeetingPhoto> selectedPhotos = meetingPhotoRepository
                .findByMeetingIdAndIsSelectedTrueOrderByCapturedAtDesc(meetingId);

        LocalDateTime startedAt = meeting.getStartedAt();
        LocalDateTime endedAt = meeting.getEndedAt();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String meetingDate = startedAt != null ? startedAt.format(dateFormatter) : "YYYY.MM.DD";
        String startTime = startedAt != null ? startedAt.format(timeFormatter) : "HH:MM";
        String endTime = endedAt != null ? endedAt.format(timeFormatter) : "HH:MM";
        String participantCount = meeting.getParticipantCount() == null ? "N" : String.valueOf(meeting.getParticipantCount());
        String studyName = (study != null && study.getName() != null && !study.getName().isBlank())
                ? study.getName()
                : "스터디 명";
        String meetingTitle = meeting.getTitle() != null && !meeting.getTitle().isBlank()
                ? meeting.getTitle()
                : "미팅 제목";
        String meetingDescription = meeting.getSessionId() == null
                ? null
                : studySessionRepository.findById(meeting.getSessionId())
                .map(session -> session.getDescription())
                .orElse(null);

        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(studyName).append("\n\n");
        builder.append("---").append("\n\n");
        builder.append("회의 일자 : ").append(meetingDate).append("\n");
        builder.append("회의 시간 : ").append(startTime).append(" ~ ").append(endTime).append("\n");
        builder.append("참여 인원: 총 ").append(participantCount).append("명").append("\n\n");
        builder.append("---").append("\n\n");
        builder.append("## 주제 :").append(meetingTitle).append("\n\n");
        if (meetingDescription != null && !meetingDescription.isBlank()) {
            builder.append(meetingDescription.strip()).append("\n\n");
        } else {
            builder.append("이번 미팅의 목적 및 주요 논의 주제를 간략히 작성").append("\n\n");
        }
        builder.append("---").append("\n\n");
        builder.append("##  AI 요약").append("\n");
        String summaryText = summary == null ? null : helper.readUploadedTextFile(summary.getFileUrl());
        if (summaryText != null && !summaryText.isBlank()) {
            builder.append("- ").append(summaryText.strip()).append("\n\n");
        } else {
            builder.append("- AI가 전체 대화를 분석하여 핵심 내용만 요약한 결과").append("\n\n");
        }
        List<String> keywords = helper.parseKeywords(summary == null ? null : summary.getKeywordsJson());

        builder.append("\n\n").append("## 키워드 : ");
        if (!keywords.isEmpty()) {
            for (String keyword : keywords) {
                builder.append("`").append(keyword).append("` ");
            }
            builder.append("\n\n");
        }
        builder.append("---").append("\n\n");
        builder.append("##  STT 기록 (전체 대화 내역)").append("\n\n");

        if (transcriptFile != null) {
            String transcriptText = helper.readUploadedTextFile(transcriptFile.getFileUrl());
            if (transcriptText != null && !transcriptText.isBlank()) {
                builder.append(transcriptText).append("\n");
            }
        }

        builder.append("\n---").append("\n\n");
        builder.append("##  회의 이미지").append("\n\n");
        builder.append("- 회의 스크린샷 또는 대표 이미지 첨부").append("\n\n");
        if (!selectedPhotos.isEmpty()) {
            for (MeetingPhoto photo : selectedPhotos) {
                builder.append("![회의 이미지](").append(photo.getImageUrl()).append(")\n");
            }
        } else {
            builder.append("![회의 이미지](./meeting_image.png)\n");
        }
        builder.append("\n---").append("\n");

        return builder.toString();
    }

    @Transactional(readOnly = true)
    public byte[] exportMeetingPdf(Long studyId, Long meetingId) {
        String markdown = exportMeetingMarkdown(studyId, meetingId);
        List<MeetingPhoto> selectedPhotos = meetingPhotoRepository
                .findByMeetingIdAndIsSelectedTrueOrderByCapturedAtDesc(meetingId);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();
            BaseFont baseFont = resolvePdfBaseFont();
            Font titleFont = new Font(baseFont, 16, Font.BOLD);
            Font sectionFont = new Font(baseFont, 13, Font.BOLD);
            Font bodyFont = new Font(baseFont, 11);
            for (String line : markdown.split("\n")) {
                if (line.startsWith("##  회의 이미지") || line.startsWith("![")) {
                    continue;
                }
                if (line.trim().equals("---")) {
                    continue;
                }
                if (line.startsWith("# ")) {
                    document.add(new Paragraph(line.substring(2), titleFont));
                } else if (line.startsWith("## ")) {
                    Paragraph paragraph = new Paragraph(line.substring(3), sectionFont);
                    paragraph.setSpacingBefore(10f);
                    document.add(paragraph);
                } else {
                    Paragraph paragraph = new Paragraph(line, bodyFont);
                    paragraph.setLeading(0f, 1.4f);
                    paragraph.setAlignment(Element.ALIGN_LEFT);
                    document.add(paragraph);
                }
            }
            appendSelectedPhotos(document, sectionFont, bodyFont, selectedPhotos);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF_EXPORT_FAILED");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF_FONT_LOAD_FAILED");
        }
    }

    private BaseFont resolvePdfBaseFont() throws IOException, DocumentException {
        if (pdfFontPath != null && !pdfFontPath.isBlank()) {
            // .ttc (TrueType Collection) 파일은 폰트 인덱스 지정 필요 (예: path.ttc,0)
            String fontPath = pdfFontPath;
            if (pdfFontPath.toLowerCase().endsWith(".ttc") && !pdfFontPath.contains(",")) {
                fontPath = pdfFontPath + ",0";
            }
            return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        }
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    private void appendSelectedPhotos(Document document, Font sectionFont, Font bodyFont, List<MeetingPhoto> photos)
            throws DocumentException {
        if (photos == null || photos.isEmpty()) {
            return;
        }
        Paragraph paragraph = new Paragraph("회의 사진", sectionFont);
        paragraph.setSpacingBefore(10f);
        document.add(paragraph);
        for (MeetingPhoto photo : photos) {
            Path imagePath = localFileStorageService.resolveUploadedPath(photo.getImageUrl());
            if (imagePath == null || !Files.exists(imagePath)) {
                document.add(new Paragraph(photo.getImageUrl(), bodyFont));
                continue;
            }
            try {
                Image image = Image.getInstance(imagePath.toAbsolutePath().toString());
                float maxWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
                image.scaleToFit(maxWidth, 360f);
                image.setSpacingBefore(6f);
                document.add(image);
            } catch (Exception e) {
                document.add(new Paragraph(photo.getImageUrl(), bodyFont));
            }
        }
    }
}
