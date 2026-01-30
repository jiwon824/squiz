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
import com.ssafy.domain.meeting.dto.response.MeetingActionItemResponse;
import com.ssafy.domain.meeting.entity.Meeting;
import com.ssafy.domain.meeting.entity.MeetingPhoto;
import com.ssafy.domain.meeting.entity.MeetingSttFile;
import com.ssafy.domain.meeting.entity.MeetingSttSummary;
import com.ssafy.domain.meeting.entity.MeetingTextTrackType;
import com.ssafy.domain.meeting.repository.MeetingPhotoRepository;
import com.ssafy.domain.meeting.repository.MeetingSttFileRepository;
import com.ssafy.domain.meeting.repository.MeetingSttSummaryRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingExportService {

    private final MeetingPhotoRepository meetingPhotoRepository;
    private final MeetingSttFileRepository meetingSttFileRepository;
    private final MeetingSttSummaryRepository meetingSttSummaryRepository;
    private final LocalFileStorageService localFileStorageService;
    private final MeetingServiceHelper helper;

    @Value("${meeting.pdf.font-path:}")
    private String pdfFontPath;

    @Transactional(readOnly = true)
    public String exportMeetingMarkdown(Long studyId, Long meetingId) {
        Meeting meeting = helper.getMeetingOrThrow(studyId, meetingId);
        MeetingSttSummary summary = meetingSttSummaryRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElse(null);
        MeetingSttFile transcriptFile = meetingSttFileRepository
                .findByMeetingIdAndTrackTypeAndUserIdIsNull(meetingId, MeetingTextTrackType.MIXED)
                .orElse(null);
        List<MeetingPhoto> selectedPhotos = meetingPhotoRepository
                .findByMeetingIdAndIsSelectedTrueOrderByCapturedAtDesc(meetingId);

        StringBuilder builder = new StringBuilder();
        builder.append("# Meeting Summary").append("\n\n");
        builder.append("- Title: ").append(meeting.getTitle()).append("\n");
        builder.append("- Type: ").append(meeting.getMeetingType().name()).append("\n");
        builder.append("- Started At: ").append(meeting.getStartedAt()).append("\n");
        builder.append("- Ended At: ").append(meeting.getEndedAt()).append("\n\n");

        String summaryText = summary == null ? null : helper.readUploadedTextFile(summary.getFileUrl());
        if (summaryText != null && !summaryText.isBlank()) {
            builder.append("## Overall Summary").append("\n").append(summaryText).append("\n\n");
        }

        List<MeetingActionItemResponse> actionItems = helper.parseActionItems(
                summary == null ? null : summary.getActionItemsJson());
        if (!actionItems.isEmpty()) {
            builder.append("## Action Items").append("\n");
            for (MeetingActionItemResponse item : actionItems) {
                builder.append("- [").append(item.status().name()).append("] ")
                        .append(item.content());
                if (item.assigneeId() != null) {
                    builder.append(" (assignee: ").append(item.assigneeId()).append(")");
                }
                builder.append("\n");
            }
            builder.append("\n");
        }

        if (transcriptFile != null) {
            String transcriptText = helper.readUploadedTextFile(transcriptFile.getFileUrl());
            if (transcriptText != null && !transcriptText.isBlank()) {
                builder.append("## Transcripts").append("\n");
                builder.append(transcriptText).append("\n");
            }
        }

        if (!selectedPhotos.isEmpty()) {
            builder.append("\n## 회의 사진\n");
            for (MeetingPhoto photo : selectedPhotos) {
                builder.append("![").append("회의 사진").append("](").append(photo.getImageUrl()).append(")\n");
            }
        }

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
                if (line.startsWith("## 회의 사진") || line.startsWith("![")) {
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
            return BaseFont.createFont(pdfFontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
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
