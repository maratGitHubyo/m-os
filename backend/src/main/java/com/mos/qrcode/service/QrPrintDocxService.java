package com.mos.qrcode.service;

import com.mos.item.entity.ItemTemplate;
import com.mos.item.enums.ItemRarity;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.qrcode.entity.QrCode;
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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrPrintDocxService {

    private static final int COLUMNS = 2;
    private static final int ROWS_PER_PAGE = 5;
    /** Compact enough that 5 rows always fit on A4 even with a legendary caption. */
    private static final int QR_SIZE_EMU = Units.toEMU(95);
    private static final int PAGE_WIDTH_TWIPS = 11906;
    private static final int PAGE_HEIGHT_TWIPS = 16838;
    private static final int MARGIN_TWIPS = 567; // 1 cm
    /**
     * Fixed row height with headroom: 5 × 2700 = 13500 twips,
     * A4 usable height with 1 cm margins ≈ 15704 twips.
     */
    private static final int ROW_HEIGHT_TWIPS = 2700;

    private final QrCodeRepository qrCodeRepository;
    private final QrImageService qrImageService;
    private final ItemTemplateRepository itemTemplateRepository;

    @Transactional(readOnly = true)
    public byte[] buildPrintDocument(UUID gameSessionId) {
        List<QrCode> codes = qrCodeRepository.findByGameSessionIdOrderByCreatedAtAsc(gameSessionId);
        Map<UUID, ItemRarity> rarityByTemplateId = loadRarities(codes);

        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            clearDefaultBody(document);
            configurePage(document);

            if (codes.isEmpty()) {
                XWPFParagraph empty = document.createParagraph();
                empty.createRun().setText("В сессии пока нет QR-кодов.");
            } else {
                int usableWidth = PAGE_WIDTH_TWIPS - (MARGIN_TWIPS * 2);
                int cellWidth = usableWidth / COLUMNS;
                int pageCapacity = COLUMNS * ROWS_PER_PAGE;

                for (int offset = 0; offset < codes.size(); offset += pageCapacity) {
                    int pageCount = Math.min(pageCapacity, codes.size() - offset);
                    int rowCount = (pageCount + COLUMNS - 1) / COLUMNS;
                    XWPFTable table = document.createTable(rowCount, COLUMNS);
                    setTableWidth(table, usableWidth);

                    for (int row = 0; row < rowCount; row++) {
                        XWPFTableRow tableRow = table.getRow(row);
                        tableRow.setHeight(ROW_HEIGHT_TWIPS);
                        tableRow.setHeightRule(TableRowHeightRule.EXACT);
                        for (int col = 0; col < COLUMNS; col++) {
                            int index = offset + row * COLUMNS + col;
                            XWPFTableCell cell = tableRow.getCell(col);
                            setCellWidth(cell, cellWidth);
                            zeroCellMargins(cell);
                            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                            if (index >= codes.size()) {
                                clearCell(cell);
                                continue;
                            }
                            fillCell(cell, codes.get(index), rarityByTemplateId);
                        }
                    }

                    if (offset > 0) {
                        // Page break before this table without an extra body paragraph.
                        XWPFParagraph first = table.getRow(0).getCell(0).getParagraphs().getFirst();
                        first.setPageBreak(true);
                    }
                }
            }
            document.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build QR print document", ex);
        }
    }

    private Map<UUID, ItemRarity> loadRarities(List<QrCode> codes) {
        Set<UUID> templateIds = new HashSet<>();
        for (QrCode code : codes) {
            UUID templateId = extractItemTemplateId(code);
            if (templateId != null) {
                templateIds.add(templateId);
            }
        }
        if (templateIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, ItemRarity> rarities = new HashMap<>();
        for (ItemTemplate template : itemTemplateRepository.findAllById(templateIds)) {
            rarities.put(template.getId(), template.getRarity());
        }
        return rarities;
    }

    private void fillCell(
            XWPFTableCell cell,
            QrCode qrCode,
            Map<UUID, ItemRarity> rarityByTemplateId
    ) throws Exception {
        clearCell(cell);
        byte[] png = qrImageService.generateQrImage(qrCode);

        XWPFParagraph imageParagraph = cell.getParagraphs().isEmpty()
                ? cell.addParagraph()
                : cell.getParagraphs().getFirst();
        imageParagraph.setAlignment(ParagraphAlignment.CENTER);
        imageParagraph.setSpacingBefore(0);
        imageParagraph.setSpacingAfter(0);
        imageParagraph.setSpacingBetween(1.0);
        XWPFRun imageRun = imageParagraph.createRun();
        imageRun.addPicture(
                new ByteArrayInputStream(png),
                XWPFDocument.PICTURE_TYPE_PNG,
                qrCode.getCode() + ".png",
                QR_SIZE_EMU,
                QR_SIZE_EMU
        );

        if (isLegendary(qrCode, rarityByTemplateId)) {
            imageRun.addBreak();
            XWPFRun titleRun = imageParagraph.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(8);
            titleRun.setText(displayTitle(qrCode));
        }
    }

    private static boolean isLegendary(QrCode qrCode, Map<UUID, ItemRarity> rarityByTemplateId) {
        UUID templateId = extractItemTemplateId(qrCode);
        if (templateId == null) {
            return false;
        }
        return rarityByTemplateId.get(templateId) == ItemRarity.LEGENDARY;
    }

    private static UUID extractItemTemplateId(QrCode qrCode) {
        Map<String, Object> payload = qrCode.getRewardPayload();
        if (payload == null) {
            return null;
        }
        Object value = payload.get("itemTemplateId");
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String displayTitle(QrCode qrCode) {
        if (qrCode.getTitle() != null && !qrCode.getTitle().isBlank()) {
            return qrCode.getTitle();
        }
        return qrCode.getCode();
    }

    private static void clearDefaultBody(XWPFDocument document) {
        for (int i = document.getBodyElements().size() - 1; i >= 0; i--) {
            document.removeBodyElement(i);
        }
    }

    private static void configurePage(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();

        CTPageSz pageSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pageSz.setW(BigInteger.valueOf(PAGE_WIDTH_TWIPS));
        pageSz.setH(BigInteger.valueOf(PAGE_HEIGHT_TWIPS));
        pageSz.setOrient(STPageOrientation.PORTRAIT);

        CTPageMar pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        BigInteger margin = BigInteger.valueOf(MARGIN_TWIPS);
        pageMar.setTop(margin);
        pageMar.setBottom(margin);
        pageMar.setLeft(margin);
        pageMar.setRight(margin);
        pageMar.setHeader(BigInteger.ZERO);
        pageMar.setFooter(BigInteger.ZERO);
        pageMar.setGutter(BigInteger.ZERO);
    }

    private static void clearCell(XWPFTableCell cell) {
        while (cell.getParagraphs().size() > 1) {
            cell.removeParagraph(cell.getParagraphs().size() - 1);
        }
        if (!cell.getParagraphs().isEmpty()) {
            XWPFParagraph first = cell.getParagraphs().getFirst();
            first.setSpacingBefore(0);
            first.setSpacingAfter(0);
            for (int i = first.getRuns().size() - 1; i >= 0; i--) {
                first.removeRun(i);
            }
        }
    }

    private static void setTableWidth(XWPFTable table, int widthTwips) {
        CTTblWidth width = table.getCTTbl().getTblPr() == null
                ? table.getCTTbl().addNewTblPr().addNewTblW()
                : (table.getCTTbl().getTblPr().isSetTblW()
                ? table.getCTTbl().getTblPr().getTblW()
                : table.getCTTbl().getTblPr().addNewTblW());
        width.setType(STTblWidth.DXA);
        width.setW(BigInteger.valueOf(widthTwips));
    }

    private static void setCellWidth(XWPFTableCell cell, int widthTwips) {
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTTblWidth width = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        width.setType(STTblWidth.DXA);
        width.setW(BigInteger.valueOf(widthTwips));
    }

    private static void zeroCellMargins(XWPFTableCell cell) {
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTTcMar margins = tcPr.isSetTcMar() ? tcPr.getTcMar() : tcPr.addNewTcMar();
        BigInteger zero = BigInteger.ZERO;
        (margins.isSetTop() ? margins.getTop() : margins.addNewTop()).setW(zero);
        (margins.isSetBottom() ? margins.getBottom() : margins.addNewBottom()).setW(zero);
        (margins.isSetLeft() ? margins.getLeft() : margins.addNewLeft()).setW(zero);
        (margins.isSetRight() ? margins.getRight() : margins.addNewRight()).setW(zero);
        margins.getTop().setType(STTblWidth.DXA);
        margins.getBottom().setType(STTblWidth.DXA);
        margins.getLeft().setType(STTblWidth.DXA);
        margins.getRight().setType(STTblWidth.DXA);
    }
}
