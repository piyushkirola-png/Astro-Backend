package com.astrologytalk.service;

import com.astrologytalk.dto.response.DashaPeriodResponse;
import com.astrologytalk.dto.response.KundaliBasicResponse;
import com.astrologytalk.dto.response.KundaliChartResponse;
import com.astrologytalk.dto.response.PlanetaryPositionResponse;
import com.astrologytalk.entity.User;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KundaliPdfService {

    private final KundaliService kundaliService;

    private static final Color PRIMARY = new Color(184, 134, 42);
    private static final Color INK_900 = new Color(23, 18, 12);
    private static final Color INK_500 = new Color(122, 114, 96);
    private static final Color INK_100 = new Color(244, 241, 234);
    private static final Color BORDER = new Color(231, 227, 216);

    private static final String COMPANY_NAME = "Jyotish AI";
    private static final String COMPANY_EMAIL = "support@jyotishai.com";
    private static final String COMPANY_WEBSITE = "www.jyotishai.com";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public byte[] generate(User user) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addHeader(doc, user);
            addDivider(doc);

            KundaliBasicResponse basic = null;
            try {
                basic = kundaliService.getBasic(user.getId());
            } catch (Exception e) {
                log.warn("[Kundali PDF] Basic fetch failed: {}", e.getMessage());
            }
            if (basic != null) {
                addBirthDetails(doc, basic);
                addPanchang(doc, basic);
                addAvakhada(doc, basic);
            }

            try {
                List<KundaliChartResponse> charts = kundaliService.getCharts(user.getId());
                if (charts != null && !charts.isEmpty()) {
                    addChartsSection(doc, charts);
                }
            } catch (Exception e) {
                log.warn("[Kundali PDF] Charts fetch failed: {}", e.getMessage());
            }

            try {
                List<PlanetaryPositionResponse> planets = kundaliService.getPlanetaryPositions(user.getId());
                if (planets != null && !planets.isEmpty()) {
                    addPlanetaryTable(doc, planets);
                }
            } catch (Exception e) {
                log.warn("[Kundali PDF] Planets fetch failed: {}", e.getMessage());
            }

            try {
                List<DashaPeriodResponse> dashas = kundaliService.getDashaPeriods(user.getId());
                if (dashas != null && !dashas.isEmpty()) {
                    addDashaSection(doc, dashas);
                }
            } catch (Exception e) {
                log.warn("[Kundali PDF] Dasha fetch failed: {}", e.getMessage());
            }

            addFooter(doc);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("[Kundali PDF] Generation failed for user {}: {}",
                    user.getId(), e.getMessage(), e);
            throw new RuntimeException("Kundali PDF generation failed", e);
        }
    }

    private void addHeader(Document doc, User user) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.3f, 1});

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(0);

        try {
            ClassPathResource logoRes = new ClassPathResource("assets/logo.png");
            if (logoRes.exists()) {
                try (InputStream is = logoRes.getInputStream()) {
                    byte[] logoBytes = is.readAllBytes();
                    Image logo = Image.getInstance(logoBytes);
                    logo.scaleToFit(60, 60);
                    logo.setAlignment(Image.ALIGN_LEFT);
                    left.addElement(logo);
                }
            }
        } catch (Exception e) {
            log.warn("[Kundali PDF] Logo load failed: {}", e.getMessage());
        }

        Font brandFont = new Font(Font.HELVETICA, 16, Font.BOLD, PRIMARY);
        Paragraph brand = new Paragraph(COMPANY_NAME.toUpperCase(), brandFont);
        brand.setSpacingBefore(6);
        left.addElement(brand);

        Font infoFont = new Font(Font.HELVETICA, 8, Font.NORMAL, INK_500);
        left.addElement(new Paragraph(COMPANY_EMAIL, infoFont));
        left.addElement(new Paragraph(COMPANY_WEBSITE, infoFont));

        header.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setPadding(0);

        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, INK_900);
        Paragraph title = new Paragraph("KUNDALI REPORT", titleFont);
        title.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(title);

        Font subFont = new Font(Font.HELVETICA, 10, Font.NORMAL, INK_500);
        Paragraph sub = new Paragraph(
                user.getName() != null ? user.getName() : "User", subFont);
        sub.setAlignment(Element.ALIGN_RIGHT);
        sub.setSpacingBefore(4);
        right.addElement(sub);

        header.addCell(right);

        doc.add(header);
        doc.add(spacer(14));
    }

    private void addBirthDetails(Document doc, KundaliBasicResponse basic) throws DocumentException {
        sectionTitle(doc, "Birth Details");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1});

        addInfoRow(table, "Name", nullSafe(basic.getName()));
        addInfoRow(table, "Gender", nullSafe(basic.getGender()));
        addInfoRow(table, "Date of Birth", formatDate(basic.getDateOfBirth()));
        addInfoRow(table, "Time of Birth", nullSafe(basic.getTimeOfBirth()));
        addInfoRow(table, "Place of Birth", nullSafe(basic.getPlaceOfBirth()));
        addInfoRow(table, "Timezone", nullSafe(basic.getBirthTimezone()));

        doc.add(table);
        doc.add(spacer(12));
    }

    private void addPanchang(Document doc, KundaliBasicResponse basic) throws DocumentException {
        sectionTitle(doc, "Panchang");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1});

        addInfoRow(table, "Tithi", nullSafe(basic.getPanchangTithi()));
        addInfoRow(table, "Karana", nullSafe(basic.getKarana()));
        addInfoRow(table, "Yoga", nullSafe(basic.getYoga()));
        addInfoRow(table, "Nakshatra", nullSafe(basic.getNakshatra()));
        addInfoRow(table, "Nakshatra Lord", nullSafe(basic.getNakshatraLord()));
        addInfoRow(table, "Ascendant", nullSafe(basic.getAscendant()));
        addInfoRow(table, "Ascendant Lord", nullSafe(basic.getAscendantLord()));
        addInfoRow(table, "Sunrise", nullSafe(basic.getSunrise()));
        addInfoRow(table, "Sunset", nullSafe(basic.getSunset()));

        doc.add(table);
        doc.add(spacer(12));
    }

    private void addAvakhada(Document doc, KundaliBasicResponse basic) throws DocumentException {
        sectionTitle(doc, "Avakhada Details");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1});

        addInfoRow(table, "Varna", nullSafe(basic.getVarna()));
        addInfoRow(table, "Vashya", nullSafe(basic.getVashya()));
        addInfoRow(table, "Yoni", nullSafe(basic.getYoni()));
        addInfoRow(table, "Gan", nullSafe(basic.getGan()));
        addInfoRow(table, "Nadi", nullSafe(basic.getNadi()));
        addInfoRow(table, "Sign", nullSafe(basic.getSign()));
        addInfoRow(table, "Sign Lord", nullSafe(basic.getSignLord()));
        addInfoRow(table, "Charan", nullSafe(basic.getCharan()));
        addInfoRow(table, "Tatva", nullSafe(basic.getTatva()));
        addInfoRow(table, "Name Alphabet", nullSafe(basic.getNameAlphabet()));
        addInfoRow(table, "Paya", nullSafe(basic.getPaya()));
        addInfoRow(table, "Yunja", nullSafe(basic.getYunja()));

        doc.add(table);
        doc.add(spacer(12));
    }

    private void addChartsSection(Document doc, List<KundaliChartResponse> charts) throws DocumentException {
        sectionTitle(doc, "Kundali Charts");

        PdfPTable container = new PdfPTable(2);
        container.setWidthPercentage(100);
        container.setWidths(new float[]{1, 1});

        for (KundaliChartResponse chart : charts) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.BOX);
            cell.setBorderColor(BORDER);
            cell.setPadding(10);

            Font chartTitleFont = new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY);
            Paragraph chartTitle = new Paragraph(
                    chart.getChartLabel() != null ? chart.getChartLabel() : chart.getChartType(),
                    chartTitleFont);
            chartTitle.setAlignment(Element.ALIGN_CENTER);
            chartTitle.setSpacingAfter(6);
            cell.addElement(chartTitle);

            cell.addElement(buildChartGrid(chart));
            container.addCell(cell);
        }

        if (charts.size() % 2 != 0) {
            PdfPCell empty = new PdfPCell();
            empty.setBorder(Rectangle.NO_BORDER);
            container.addCell(empty);
        }

        doc.add(container);
        doc.add(spacer(12));
    }

    private PdfPTable buildChartGrid(KundaliChartResponse chart) throws DocumentException {
        PdfPTable grid = new PdfPTable(3);
        grid.setWidthPercentage(100);
        grid.setWidths(new float[]{1, 1, 1});

        Font cellFont = new Font(Font.HELVETICA, 8, Font.NORMAL, INK_900);
        List<String> houses = chart.getHouses();

        for (int i = 0; i < 12; i++) {
            PdfPCell cell = new PdfPCell();
            cell.setPadding(6);
            cell.setBorderColor(BORDER);
            cell.setMinimumHeight(40);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            String value = (houses != null && i < houses.size()) ? houses.get(i) : "—";
            if (value == null || value.isBlank()) value = "—";

            Paragraph p = new Paragraph();
            Font numFont = new Font(Font.HELVETICA, 7, Font.BOLD, PRIMARY);
            p.add(new Chunk((i + 1) + "\n", numFont));
            p.add(new Chunk(value, cellFont));
            p.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(p);

            grid.addCell(cell);
        }

        return grid;
    }

    private void addPlanetaryTable(Document doc, List<PlanetaryPositionResponse> planets) throws DocumentException {
        sectionTitle(doc, "Planetary Positions");

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1.2f, 1, 1.2f, 1, 0.8f});

        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        Font cellFont = new Font(Font.HELVETICA, 8, Font.NORMAL, INK_900);

        String[] headers = {"Planet", "Sign", "Degree", "Nakshatra", "House", "Retro"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(PRIMARY);
            cell.setPadding(6);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        for (PlanetaryPositionResponse p : planets) {
            addDataCell(table, nullSafe(p.getPlanet()), cellFont, Element.ALIGN_LEFT);
            addDataCell(table, nullSafe(p.getSign()), cellFont, Element.ALIGN_LEFT);
            addDataCell(table, nullSafe(p.getDegree()), cellFont, Element.ALIGN_CENTER);
            addDataCell(table, nullSafe(p.getNakshatra()), cellFont, Element.ALIGN_LEFT);
            addDataCell(table, p.getHouse() != null ? String.valueOf(p.getHouse()) : "—",
                    cellFont, Element.ALIGN_CENTER);
            addDataCell(table, nullSafe(p.getRetro()), cellFont, Element.ALIGN_CENTER);
        }

        doc.add(table);
        doc.add(spacer(12));
    }

    private void addDashaSection(Document doc, List<DashaPeriodResponse> dashas) throws DocumentException {
        sectionTitle(doc, "Vimshottari Mahadasha");

        Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD, PRIMARY);
        Font bodyFont = new Font(Font.HELVETICA, 8, Font.NORMAL, INK_900);

        for (DashaPeriodResponse d : dashas) {
            PdfPTable card = new PdfPTable(1);
            card.setWidthPercentage(100);

            PdfPCell cell = new PdfPCell();
            cell.setPadding(8);
            cell.setBorderColor(BORDER);

            String dates = (d.getStartDateFormatted() != null ? d.getStartDateFormatted() : "—")
                    + " to "
                    + (d.getEndDateFormatted() != null ? d.getEndDateFormatted() : "—");

            Paragraph heading = new Paragraph(
                    d.getPlanet() + " Mahadasha (" + dates + ")",
                    labelFont);
            heading.setSpacingAfter(4);
            cell.addElement(heading);

            if (d.getParagraph1() != null) {
                cell.addElement(new Paragraph(d.getParagraph1(), bodyFont));
            }
            if (d.getParagraph2() != null) {
                Paragraph p2 = new Paragraph(d.getParagraph2(), bodyFont);
                p2.setSpacingBefore(3);
                cell.addElement(p2);
            }

            card.addCell(cell);
            doc.add(card);
            doc.add(spacer(4));
        }
    }

    private void addFooter(Document doc) throws DocumentException {
        doc.add(spacer(20));

        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(INK_100);
        cell.setPadding(12);
        cell.setBorder(Rectangle.NO_BORDER);

        Font thanksFont = new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY);
        Paragraph thanks = new Paragraph("Jyotish AI — Vedic guidance for modern life", thanksFont);
        thanks.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(thanks);

        Font noteFont = new Font(Font.HELVETICA, 9, Font.NORMAL, INK_500);
        Paragraph note = new Paragraph(
                "Questions? Contact " + COMPANY_EMAIL, noteFont);
        note.setAlignment(Element.ALIGN_CENTER);
        note.setSpacingBefore(4);
        cell.addElement(note);

        footer.addCell(cell);
        doc.add(footer);
    }

    private void sectionTitle(Document doc, String title) throws DocumentException {
        Font font = new Font(Font.HELVETICA, 12, Font.BOLD, PRIMARY);
        Paragraph p = new Paragraph(title, font);
        p.setSpacingBefore(4);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private void addDivider(Document doc) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderWidthBottom(0.8f);
        cell.setBorderColorBottom(BORDER);
        cell.setMinimumHeight(4);
        line.addCell(cell);
        doc.add(line);
        doc.add(spacer(10));
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        Font labelFont = new Font(Font.HELVETICA, 9, Font.NORMAL, INK_500);
        Font valueFont = new Font(Font.HELVETICA, 9, Font.BOLD, INK_900);

        PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
        lc.setBorderColor(BORDER);
        lc.setPadding(5);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setBorderColor(BORDER);
        vc.setPadding(5);
        table.addCell(vc);
    }

    private void addDataCell(PdfPTable table, String value, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBorderColor(BORDER);
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private Paragraph spacer(int height) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(height);
        return p;
    }

    private String nullSafe(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "—";
        return date.format(DATE_FMT);
    }
}