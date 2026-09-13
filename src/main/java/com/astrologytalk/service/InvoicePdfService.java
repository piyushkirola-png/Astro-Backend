package com.astrologytalk.service;

import com.astrologytalk.entity.Payment;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InvoicePdfService {

  private static final Color PRIMARY = new Color(184, 134, 42);
  private static final Color ACCENT = new Color(245, 158, 11);
  private static final Color INK_900 = new Color(23, 18, 12);
  private static final Color INK_500 = new Color(122, 114, 96);
  private static final Color INK_400 = new Color(162, 154, 134);
  private static final Color INK_100 = new Color(244, 241, 234);
  private static final Color BORDER = new Color(231, 227, 216);

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
  private static final DateTimeFormatter DATETIME_FMT =
      DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

  // TODO: replace with real company info before production
  private static final String COMPANY_NAME = "Jyotish AI";
  private static final String COMPANY_ADDRESS = "123 Astrology Street";
  private static final String COMPANY_CITY = "Mumbai, Maharashtra 400001";
  private static final String COMPANY_PHONE = "+91 00000 00000";
  private static final String COMPANY_EMAIL = "support@jyotishai.com";
  private static final String COMPANY_WEBSITE = "www.jyotishai.com";

  public byte[] generate(Payment payment) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
      PdfWriter.getInstance(doc, out);
      doc.open();

      addHeader(doc, payment);
      addDivider(doc);
      addBilledTo(doc, payment);
      addDivider(doc);
      addLineItems(doc, payment);
      addTotals(doc, payment);
      addDivider(doc);
      addPaymentDetails(doc, payment);
      addFooter(doc);

      doc.close();
      return out.toByteArray();

    } catch (Exception e) {
      log.error("[InvoicePDF] Failed for {}: {}", payment.getGatewayOrderId(), e.getMessage(), e);
      throw new RuntimeException("Invoice generation failed", e);
    }
  }

  private void addHeader(Document doc, Payment payment) throws DocumentException {
    PdfPTable header = new PdfPTable(2);
    header.setWidthPercentage(100);
    header.setWidths(new float[] {1.3f, 1});

    // --- LEFT: Logo + company info ---
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
      log.warn("[InvoicePDF] Logo load failed: {}", e.getMessage());
    }

    Font brandFont = new Font(Font.HELVETICA, 16, Font.BOLD, PRIMARY);
    Paragraph brand = new Paragraph(COMPANY_NAME.toUpperCase(), brandFont);
    brand.setSpacingBefore(6);
    left.addElement(brand);

    Font infoFont = new Font(Font.HELVETICA, 8, Font.NORMAL, INK_500);
    left.addElement(new Paragraph(COMPANY_ADDRESS, infoFont));
    left.addElement(new Paragraph(COMPANY_CITY, infoFont));
    left.addElement(new Paragraph("Phone: " + COMPANY_PHONE, infoFont));
    left.addElement(new Paragraph("Email: " + COMPANY_EMAIL, infoFont));
    left.addElement(new Paragraph("Web: " + COMPANY_WEBSITE, infoFont));

    header.addCell(left);

    // --- RIGHT: INVOICE title + meta ---
    PdfPCell right = new PdfPCell();
    right.setBorder(Rectangle.NO_BORDER);
    right.setPadding(0);
    right.setHorizontalAlignment(Element.ALIGN_RIGHT);

    Font invoiceFont = new Font(Font.HELVETICA, 22, Font.BOLD, INK_900);
    Paragraph invoiceTitle = new Paragraph("INVOICE", invoiceFont);
    invoiceTitle.setAlignment(Element.ALIGN_RIGHT);
    right.addElement(invoiceTitle);

    Font metaLabelFont = new Font(Font.HELVETICA, 8, Font.BOLD, INK_400);
    Font metaValueFont = new Font(Font.HELVETICA, 10, Font.BOLD, INK_900);

    Paragraph invoiceNo = new Paragraph();
    invoiceNo.setAlignment(Element.ALIGN_RIGHT);
    invoiceNo.setSpacingBefore(8);
    invoiceNo.add(new Chunk("INVOICE #\n", metaLabelFont));
    invoiceNo.add(
        new Chunk(
            payment.getInvoiceNumber() != null ? payment.getInvoiceNumber() : "—", metaValueFont));
    right.addElement(invoiceNo);

    Paragraph dateP = new Paragraph();
    dateP.setAlignment(Element.ALIGN_RIGHT);
    dateP.setSpacingBefore(6);
    dateP.add(new Chunk("DATE\n", metaLabelFont));
    dateP.add(
        new Chunk(
            payment.getCreatedAt() != null ? payment.getCreatedAt().format(DATE_FMT) : "—",
            metaValueFont));
    right.addElement(dateP);

    header.addCell(right);

    doc.add(header);

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(8);
    doc.add(spacer);
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

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(8);
    doc.add(spacer);
  }

  private void addBilledTo(Document doc, Payment payment) throws DocumentException {
    Font headingFont = new Font(Font.HELVETICA, 9, Font.BOLD, INK_400);
    Paragraph heading = new Paragraph("BILL TO", headingFont);
    heading.setSpacingAfter(6);
    doc.add(heading);

    Font nameFont = new Font(Font.HELVETICA, 12, Font.BOLD, INK_900);
    Font detailFont = new Font(Font.HELVETICA, 10, Font.NORMAL, INK_500);

    doc.add(new Paragraph(nvl(payment.getCustomerName()), nameFont));

    if (payment.getCustomerEmail() != null && !payment.getCustomerEmail().isBlank()) {
      doc.add(new Paragraph(payment.getCustomerEmail(), detailFont));
    }
    if (payment.getCustomerPhone() != null && !payment.getCustomerPhone().isBlank()) {
      doc.add(new Paragraph(payment.getCustomerPhone(), detailFont));
    }

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(12);
    doc.add(spacer);
  }

  private void addLineItems(Document doc, Payment payment) throws DocumentException {
    PdfPTable table = new PdfPTable(3);
    table.setWidthPercentage(100);
    table.setWidths(new float[] {4, 1, 1.5f});

    Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, INK_900);
    Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, INK_500);

    // --- Header row ---
    PdfPCell descHeader = new PdfPCell(new Phrase("DESCRIPTION", headerFont));
    descHeader.setBackgroundColor(PRIMARY);
    descHeader.setPadding(10);
    descHeader.setBorder(Rectangle.NO_BORDER);
    table.addCell(descHeader);

    PdfPCell taxHeader = new PdfPCell(new Phrase("TAX", headerFont));
    taxHeader.setBackgroundColor(PRIMARY);
    taxHeader.setPadding(10);
    taxHeader.setBorder(Rectangle.NO_BORDER);
    taxHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
    table.addCell(taxHeader);

    PdfPCell amtHeader = new PdfPCell(new Phrase("AMOUNT", headerFont));
    amtHeader.setBackgroundColor(PRIMARY);
    amtHeader.setPadding(10);
    amtHeader.setBorder(Rectangle.NO_BORDER);
    amtHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
    table.addCell(amtHeader);

    // --- Data row ---
    PdfPCell descCell = new PdfPCell();
    descCell.setPadding(12);
    descCell.setBorderColor(BORDER);
    descCell.addElement(new Paragraph(buildDescription(payment), bodyFont));

    String subline = buildSubline(payment);
    if (subline != null) {
      Paragraph sub = new Paragraph(subline, smallFont);
      sub.setSpacingBefore(4);
      descCell.addElement(sub);
    }
    table.addCell(descCell);

    String taxText =
        payment.getGstRate() != null
            ? payment.getGstRate().stripTrailingZeros().toPlainString() + "%"
            : "—";
    PdfPCell taxCell = new PdfPCell(new Phrase(taxText, bodyFont));
    taxCell.setPadding(12);
    taxCell.setBorderColor(BORDER);
    taxCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    table.addCell(taxCell);

    PdfPCell amtCell =
        new PdfPCell(
            new Phrase(
                "₹"
                    + formatAmount(
                        payment.getBaseAmount() != null
                            ? payment.getBaseAmount()
                            : payment.getAmount()),
                bodyFont));
    amtCell.setPadding(12);
    amtCell.setBorderColor(BORDER);
    amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    table.addCell(amtCell);

    doc.add(table);

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(12);
    doc.add(spacer);
  }

  private void addTotals(Document doc, Payment payment) throws DocumentException {
    PdfPTable totals = new PdfPTable(2);
    totals.setWidthPercentage(45);
    totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
    totals.setWidths(new float[] {1.5f, 1});

    Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL, INK_500);
    Font valueFont = new Font(Font.HELVETICA, 10, Font.BOLD, INK_900);
    Font totalLabelFont = new Font(Font.HELVETICA, 11, Font.BOLD, INK_900);
    Font totalValueFont = new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY);

    BigDecimal base =
        payment.getBaseAmount() != null ? payment.getBaseAmount() : payment.getAmount();
    BigDecimal gst = payment.getGstAmount() != null ? payment.getGstAmount() : BigDecimal.ZERO;
    BigDecimal total = payment.getAmount();
    BigDecimal rate = payment.getGstRate() != null ? payment.getGstRate() : BigDecimal.ZERO;

    addTotalRow(totals, "Subtotal", "₹" + formatAmount(base), labelFont, valueFont);
    addTotalRow(
        totals, "Tax Rate", rate.stripTrailingZeros().toPlainString() + "%", labelFont, valueFont);
    addTotalRow(totals, "GST", "₹" + formatAmount(gst), labelFont, valueFont);
    addTotalRow(totals, "Total", "₹" + formatAmount(total), totalLabelFont, totalValueFont);

    doc.add(totals);

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(16);
    doc.add(spacer);
  }

  private void addPaymentDetails(Document doc, Payment payment) throws DocumentException {
    Font headingFont = new Font(Font.HELVETICA, 9, Font.BOLD, INK_400);
    Paragraph heading = new Paragraph("PAYMENT DETAILS", headingFont);
    heading.setSpacingAfter(6);
    doc.add(heading);

    PdfPTable details = new PdfPTable(2);
    details.setWidthPercentage(100);
    details.setWidths(new float[] {1, 3});

    addDetailRow(details, "Gateway", nvl(payment.getGateway()));
    addDetailRow(details, "Status", nvl(payment.getStatus()));

    if (payment.getGatewayPaymentId() != null) {
      addDetailRow(details, "Payment ID", payment.getGatewayPaymentId());
    }
    if (payment.getUtr() != null && !payment.getUtr().isBlank()) {
      addDetailRow(details, "UTR", payment.getUtr());
    }
    if (payment.getCompletedAt() != null) {
      addDetailRow(details, "Completed", payment.getCompletedAt().format(DATETIME_FMT));
    }
    if ("FAILED".equals(payment.getStatus()) && payment.getFailureReason() != null) {
      addDetailRow(details, "Failure Reason", payment.getFailureReason());
    }

    doc.add(details);

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(16);
    doc.add(spacer);
  }

  private void addFooter(Document doc) throws DocumentException {
    PdfPTable footer = new PdfPTable(1);
    footer.setWidthPercentage(100);

    PdfPCell cell = new PdfPCell();
    cell.setBackgroundColor(INK_100);
    cell.setPadding(14);
    cell.setBorder(Rectangle.NO_BORDER);

    Font thanksFont = new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY);
    Paragraph thanks = new Paragraph("Thank you for choosing Jyotish AI 🙏", thanksFont);
    thanks.setAlignment(Element.ALIGN_CENTER);
    cell.addElement(thanks);

    Font noteFont = new Font(Font.HELVETICA, 9, Font.NORMAL, INK_500);

    Paragraph gen =
        new Paragraph("This is a computer-generated invoice. No signature required.", noteFont);
    gen.setAlignment(Element.ALIGN_CENTER);
    gen.setSpacingBefore(4);
    cell.addElement(gen);

    Paragraph support = new Paragraph("Questions? Contact " + COMPANY_EMAIL, noteFont);
    support.setAlignment(Element.ALIGN_CENTER);
    support.setSpacingBefore(2);
    cell.addElement(support);

    footer.addCell(cell);
    doc.add(footer);
  }

  private void addTotalRow(
      PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
    PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
    lc.setBorder(Rectangle.NO_BORDER);
    lc.setPadding(6);
    table.addCell(lc);

    PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
    vc.setBorder(Rectangle.NO_BORDER);
    vc.setPadding(6);
    vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
    table.addCell(vc);
  }

  private void addDetailRow(PdfPTable table, String label, String value) {
    Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL, INK_500);
    Font valueFont = new Font(Font.HELVETICA, 10, Font.BOLD, INK_900);

    PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
    lc.setBorder(Rectangle.NO_BORDER);
    lc.setPadding(4);
    table.addCell(lc);

    PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
    vc.setBorder(Rectangle.NO_BORDER);
    vc.setPadding(4);
    table.addCell(vc);
  }

  private String buildDescription(Payment payment) {
    if ("REPORT".equalsIgnoreCase(payment.getCategoryCode())) {
      return "Kundali Report (Full Vedic Analysis)";
    }
    if ("WALLET".equalsIgnoreCase(payment.getCategoryCode())) {
      Integer seconds = payment.getSecondsCredited();
      if (seconds != null && seconds > 0) {
        int mins = seconds / 60;
        return "AI Chat Wallet Recharge · " + mins + " min";
      }
      return "AI Chat Wallet Recharge";
    }
    return payment.getNotes() != null ? payment.getNotes() : "Payment";
  }

  private String buildSubline(Payment payment) {
    if ("WALLET".equalsIgnoreCase(payment.getCategoryCode())
        && payment.getSecondsCredited() != null
        && payment.getSecondsCredited() > 0) {
      return payment.getSecondsCredited() + " seconds added to your chat wallet";
    }
    if ("REPORT".equalsIgnoreCase(payment.getCategoryCode())) {
      return "Lifetime access · Instant delivery to your email";
    }
    return null;
  }

  private String formatAmount(BigDecimal amount) {
    if (amount == null) return "0.00";
    return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private String nvl(String s) {
    return s != null && !s.isBlank() ? s : "—";
  }
}
