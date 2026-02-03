package com.ssafy.domain.meeting.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
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
import java.awt.Color;

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
        ExportContext ctx = loadExportContext(studyId, meetingId);

        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(ctx.studyName()).append(" 회의록").append("\n\n");
        builder.append("## 회의 정보").append("\n");
        builder.append("- 회의 일자: ").append(ctx.meetingDate()).append("\n");
        builder.append("- 회의 시간: ").append(ctx.startTime()).append(" ~ ").append(ctx.endTime()).append("\n");
        builder.append("- 참석 인원: 총 ").append(ctx.participantCount()).append("명").append("\n");
        builder.append("- 회의 주제: ").append(ctx.meetingTitle()).append("\n\n");
        builder.append("---\n\n");
        builder.append("## 회의 목적 / 설명\n\n");
        if (ctx.meetingDescription() != null && !ctx.meetingDescription().isBlank()) {
            builder.append(ctx.meetingDescription().strip()).append("\n\n");
        } else {
            builder.append("이번 미팅의 목적 및 주요 논의 주제를 간략히 작성하세요.").append("\n\n");
        }
        builder.append("---\n\n");
        builder.append("## AI 요약\n\n");
        if (ctx.summaryText() != null && !ctx.summaryText().isBlank()) {
            builder.append("> ").append(ctx.summaryText().strip()).append("\n\n");
        } else {
            builder.append("> AI가 전체 내용을 분석하여 요약한 결과가 표시됩니다.").append("\n\n");
        }

        builder.append("\n---\n\n");
        builder.append("## 회의 이미지\n\n");
        builder.append("- 회의 스크린샷 또는 첨부 이미지\n\n");
        if (!ctx.selectedPhotos().isEmpty()) {
            for (MeetingPhoto photo : ctx.selectedPhotos()) {
                builder.append("![회의 이미지](").append(photo.getImageUrl()).append(")\n");
            }
        } else {
            builder.append("![회의 이미지](./meeting_image.png)\n");
        }

        builder.append("---\n\n");
        builder.append("## STT 기록 (전체 대화 내용)\n\n");

        if (ctx.transcriptText() != null && !ctx.transcriptText().isBlank()) {
            builder.append(ctx.transcriptText().strip()).append("\n");
        } else {
            builder.append("STT 기록이 없습니다.\n");
        }

        builder.append("\n---\n");

        return builder.toString();
    }

    @Transactional(readOnly = true)
    public byte[] exportMeetingPdf(Long studyId, Long meetingId) {
        ExportContext ctx = loadExportContext(studyId, meetingId);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();
            BaseFont baseFont = resolvePdfBaseFont();
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font sectionFont = new Font(baseFont, 13, Font.BOLD);
            Font bodyFont = new Font(baseFont, 11);
            Font metaLabelFont = new Font(baseFont, 10, Font.BOLD);
            Font metaValueFont = new Font(baseFont, 10);

            Paragraph title = new Paragraph(ctx.studyName() + " 회의록", titleFont);
            title.setSpacingAfter(8f);
            document.add(title);
            addLine(document);

            addSectionTitle(document, "회의 정보", sectionFont);
            document.add(buildMetaTable(ctx, metaLabelFont, metaValueFont));

            addSectionTitle(document, "회의 목적 / 설명", sectionFont);
            addBodyParagraph(document, ctx.meetingDescription(), bodyFont,
                    "이번 미팅의 목적 및 주요 논의 주제를 간략히 작성하세요.");

            addSectionTitle(document, "AI 요약", sectionFont);
            addBodyParagraph(document, ctx.summaryText(), bodyFont,
                    "AI가 전체 내용을 분석하여 요약한 결과가 표시됩니다.");

            appendSelectedPhotos(document, sectionFont, bodyFont, ctx.selectedPhotos());

            addSectionTitle(document, "STT 기록 (전체 대화 내용)", sectionFont);
            addBodyParagraph(document, ctx.transcriptText(), bodyFont, "STT 기록이 없습니다.");


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
            // .ttc (TrueType Collection) 파일은 폰트 인덱스가 필요 (예: path.ttc,0)
            String fontPath = pdfFontPath;
            if (pdfFontPath.toLowerCase().endsWith(".ttc") && !pdfFontPath.contains(",")) {
                fontPath = pdfFontPath + ",0";
            }
            return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        }
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    private ExportContext loadExportContext(Long studyId, Long meetingId) {
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
                : "스터디";
        String meetingTitle = meeting.getTitle() != null && !meeting.getTitle().isBlank()
                ? meeting.getTitle()
                : "미팅 제목";
        String meetingDescription = meeting.getSessionId() == null
                ? null
                : studySessionRepository.findById(meeting.getSessionId())
                .map(session -> session.getDescription())
                .orElse(null);

        String summaryText = summary == null ? null : helper.readUploadedTextFile(summary.getFileUrl());
        String transcriptText = transcriptFile == null ? null : helper.readUploadedTextFile(transcriptFile.getFileUrl());
        List<String> keywords = helper.parseKeywords(summary == null ? null : summary.getKeywordsJson());

        return new ExportContext(
                meetingDate,
                startTime,
                endTime,
                participantCount,
                studyName,
                meetingTitle,
                meetingDescription,
                summaryText,
                transcriptText,
                keywords,
                selectedPhotos
        );
    }

    private PdfPTable buildMetaTable(ExportContext ctx, Font labelFont, Font valueFont) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        try {
            table.setWidths(new float[]{1.2f, 3.8f});
        } catch (DocumentException e) {
            // fallback to default column widths
        }
        table.setSpacingBefore(6f);
        table.setSpacingAfter(6f);

        addMetaCell(table, "회의 일자", ctx.meetingDate(), labelFont, valueFont);
        addMetaCell(table, "회의 시간", ctx.startTime() + " ~ " + ctx.endTime(), labelFont, valueFont);
        addMetaCell(table, "참석 인원", "총 " + ctx.participantCount() + "명", labelFont, valueFont);
        addMetaCell(table, "회의 주제", ctx.meetingTitle(), labelFont, valueFont);
        return table;
    }

    private void addMetaCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPadding(6f);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPadding(6f);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addSectionTitle(Document document, String title, Font font) throws DocumentException {
        Paragraph paragraph = new Paragraph(title, font);
        paragraph.setSpacingBefore(12f);
        paragraph.setSpacingAfter(6f);
        document.add(paragraph);
        addLine(document);
    }

    private void addBodyParagraph(Document document, String text, Font font, String fallback)
            throws DocumentException {
        String content = (text == null || text.isBlank()) ? fallback : text.strip();
        String[] lines = content.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].isBlank() ? " " : lines[i];
            Paragraph paragraph = new Paragraph(line, font);
            paragraph.setLeading(0f, 1.4f);
            paragraph.setSpacingAfter(i == lines.length - 1 ? 4f : 2f);
            document.add(paragraph);
        }
    }

    private void addLine(Document document) throws DocumentException {
        LineSeparator line = new LineSeparator();
        line.setLineColor(new Color(220, 220, 220));
        line.setLineWidth(0.6f);
        document.add(new Chunk(line));
    }
    private void appendSelectedPhotos(Document document, Font sectionFont, Font bodyFont, List<MeetingPhoto> photos)
            throws DocumentException {
        if (photos == null || photos.isEmpty()) {
            return;
        }
        Paragraph paragraph = new Paragraph("회의 이미지", sectionFont);
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
    private record ExportContext(
            String meetingDate,
            String startTime,
            String endTime,
            String participantCount,
            String studyName,
            String meetingTitle,
            String meetingDescription,
            String summaryText,
            String transcriptText,
            List<String> keywords,
            List<MeetingPhoto> selectedPhotos
    ) {}
}

