package com.mos.qrcode.service;

import com.mos.qrcode.entity.QrCode;
import com.mos.qrcode.enums.QrScanPolicy;
import com.mos.qrcode.repository.QrCodeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowHeightRule;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrPrintDocxService {

    private static final int COLUMNS = 2;
    private static final int ROWS_PER_PAGE = 5;
    private static final int QR_SIZE_EMU = Units.toEMU(180);
    private static final int PAGE_WIDTH_TWIPS = 11906;
    private static final int MARGIN_TWIPS = 720;

    private final QrCodeRepository qrCodeRepository;
    private final QrImageService qrImageService;

    @Transactional(readOnly = true)
    public byte[] buildPrintDocument(UUID gameSessionId) {
        List<QrCode> codes = qrCodeRepository.findByGameSessionIdOrderByCreatedAtAsc(gameSessionId);
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (codes.isEmpty()) {
                XWPFParagraph empty = document.createParagraph();
                empty.createRun().setText("В сессии пока нет QR-кодов.");
            } else {
                int usableWidth = PAGE_WIDTH_TWIPS - (MARGIN_TWIPS * 2);
                int cellWidth = usableWidth / COLUMNS;
                int pageCapacity = COLUMNS * ROWS_PER_PAGE;

                for (int offset = 0; offset < codes.size(); offset += pageCapacity) {
                    if (offset > 0) {
                        document.createParagraph().setPageBreak(true);
                    }
                    int pageCount = Math.min(pageCapacity, codes.size() - offset);
                    int rowCount = (pageCount + COLUMNS - 1) / COLUMNS;
                    XWPFTable table = document.createTable(rowCount, COLUMNS);
                    setTableWidth(table, usableWidth);

                    for (int row = 0; row < rowCount; row++) {
                        XWPFTableRow tableRow = table.getRow(row);
                        tableRow.setHeight(4200);
                        tableRow.setHeightRule(TableRowHeightRule.AT_LEAST);
                        for (int col = 0; col < COLUMNS; col++) {
                            int index = offset + row * COLUMNS + col;
                            XWPFTableCell cell = tableRow.getCell(col);
                            setCellWidth(cell, cellWidth);
                            if (index >= codes.size()) {
                                clearCell(cell);
                                continue;
                            }
                            fillCell(cell, codes.get(index));
                        }
                    }
                }
            }
            document.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build QR print document", ex);
        }
    }

    private void fillCell(XWPFTableCell cell, QrCode qrCode) throws Exception {
        clearCell(cell);
        byte[] png = qrImageService.generateQrImage(qrCode);

        XWPFParagraph imageParagraph = cell.addParagraph();
        imageParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun imageRun = imageParagraph.createRun();
        imageRun.addPicture(
                new ByteArrayInputStream(png),
                XWPFDocument.PICTURE_TYPE_PNG,
                qrCode.getCode() + ".png",
                QR_SIZE_EMU,
                QR_SIZE_EMU
        );

        XWPFParagraph titleParagraph = cell.addParagraph();
        titleParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(10);
        titleRun.setText(displayTitle(qrCode));

        XWPFParagraph metaParagraph = cell.addParagraph();
        metaParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun metaRun = metaParagraph.createRun();
        metaRun.setFontSize(8);
        metaRun.setText(policyLabel(qrCode.getScanPolicy()) + " · " + qrCode.getCode());
    }

    private static String displayTitle(QrCode qrCode) {
        if (qrCode.getTitle() != null && !qrCode.getTitle().isBlank()) {
            return qrCode.getTitle();
        }
        return qrCode.getCode();
    }

    private static String policyLabel(QrScanPolicy policy) {
        return switch (policy) {
            case FIRST_PLAYER -> "Первый забирает";
            case EVERY_PLAYER -> "Каждый раз";
            case LIMITED -> "Лимит";
        };
    }

    private static void clearCell(XWPFTableCell cell) {
        while (cell.getParagraphs().size() > 1) {
            cell.removeParagraph(cell.getParagraphs().size() - 1);
        }
        if (!cell.getParagraphs().isEmpty()) {
            XWPFParagraph first = cell.getParagraphs().getFirst();
            for (int i = first.getRuns().size() - 1; i >= 0; i--) {
                first.removeRun(i);
            }
        }
    }

    private static void setTableWidth(XWPFTable table, int widthTwips) {
        CTTblWidth width = table.getCTTbl().addNewTblPr().addNewTblW();
        width.setType(STTblWidth.DXA);
        width.setW(BigInteger.valueOf(widthTwips));
    }

    private static void setCellWidth(XWPFTableCell cell, int widthTwips) {
        CTTblWidth width = cell.getCTTc().addNewTcPr().addNewTcW();
        width.setType(STTblWidth.DXA);
        width.setW(BigInteger.valueOf(widthTwips));
    }
}
