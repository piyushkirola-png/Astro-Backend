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

  private static final Color NAVY = new Color(23, 18, 12);
  private static final Color PRIMARY = new Color(184, 134, 42);
  private static final Color INK_900 = new Color(23, 18, 12);
  private static final Color INK_500 = new Color(122, 114, 96);
  private static final Color INK_400 = new Color(162, 154, 134);
  private static final Color INK_100 = new Color(244, 241, 234);
  private static final Color BORDER = new Color(231, 227, 216);

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
  private static final DateTimeFormatter DATETIME_FMT =
      DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

  private static final String COMPANY_NAME = "JYOTISH AI";
  private static final String COMPANY_TAGLINE = "Vedic Astrology & AI Guidance";
  private static final String COMPANY_ADDRESS = "123, Spiritual Tower, Mumbai - 400001";
  private static final String COMPANY_PHONE = "+91 98765 43210";
  private static final String COMPANY_EMAIL = "support@jyotishai.com";
  private static final String COMPANY_WEBSITE = "www.jyotishai.com";
  private static final String COMPANY_GSTIN = "27AABCZ1234D1ZP";

  public byte[] generate(Payment payment) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Document doc = new Document(PageSize.A4, 36, 36, 32, 32);
      PdfWriter.getInstance(doc, out);
      doc.open();

      addTopHeader(doc);
      addCompanyInfo(doc);
      addOrderMeta(doc, payment);
      addBilledTo(doc, payment);
      addLineItems(doc, payment);
      addSummary(doc, payment);
      addPaymentDetails(doc, payment);
      addFooter(doc);

      doc.close();
      return out.toByteArray();

    } catch (Exception e) {
      log.error("[InvoicePDF] Failed for {}: {}", payment.getGatewayOrderId(), e.getMessage(), e);
      throw new RuntimeException("Invoice generation failed", e);
    }
  }

  private void addTopHeader(Document doc) throws DocumentException {
    PdfPTable header = new PdfPTable(2);
    header.setWidthPercentage(100);
    header.setWidths(new float[] {2.2f, 1});

    PdfPCell left = new PdfPCell();
    left.setBorder(Rectangle.NO_BORDER);
    left.setPadding(0);
    left.setPaddingTop(6);
    left.setPaddingBottom(6);

    try {
      ClassPathResource logoRes = new ClassPathResource("assets/logo.png");
      if (logoRes.exists()) {
        try (InputStream is = logoRes.getInputStream()) {
          byte[] logoBytes = is.readAllBytes();
          Image logo = Image.getInstance(logoBytes);
          logo.scaleToFit(52, 52);
          logo.setAlignment(Image.ALIGN_LEFT);
          logo.setSpacingAfter(6);
          left.addElement(logo);
        }
      }
    } catch (Exception e) {
      log.warn("[InvoicePDF] Logo load failed: {}", e.getMessage());
    }

    Font nameFont = new Font(Font.HELVETICA, 20, Font.BOLD, PRIMARY);
    Paragraph name = new Paragraph(COMPANY_NAME, nameFont);
    name.setSpacingAfter(2);
    left.addElement(name);

    Font taglineFont = new Font(Font.HELVETICA, 9, Font.ITALIC, INK_500);
    left.addElement(new Paragraph(COMPANY_TAGLINE, taglineFont));

    header.addCell(left);

    PdfPCell right = new PdfPCell();
    right.setBorder(Rectangle.NO_BORDER);
    right.setPadding(0);
    right.setPaddingTop(6);
    right.setHorizontalAlignment(Element.ALIGN_RIGHT);

    Font invTitleFont = new Font(Font.HELVETICA, 20, Font.BOLD, NAVY);
    Paragraph invTitle = new Paragraph("TAX INVOICE", invTitleFont);
    invTitle.setAlignment(Element.ALIGN_RIGHT);
    invTitle.setSpacingAfter(2);
    right.addElement(invTitle);

    Font invSubFont = new Font(Font.HELVETICA, 8, Font.NORMAL, INK_500);
    Paragraph invSub = new Paragraph("Original for Recipient", invSubFont);
    invSub.setAlignment(Element.ALIGN_RIGHT);
    right.addElement(invSub);

    header.addCell(right);

    doc.add(header);

    PdfPTable topLine = new PdfPTable(1);
    topLine.setWidthPercentage(100);
    PdfPCell line = new PdfPCell();
    line.setBorder(Rectangle.NO_BORDER);
    line.setBorderWidthTop(2.5f);
    line.setBorderColorTop(NAVY);
    line.setMinimumHeight(4);
    topLine.addCell(line);
    doc.add(topLine);

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(4);
    doc.add(spacer);
  }

  private void addCompanyInfo(Document doc) throws DocumentException {
    PdfPTable bar = new PdfPTable(2);
    bar.setWidthPercentage(100);
    bar.setWidths(new float[] {1, 1});

    Font labelFont = new Font(Font.HELVETICA, 8, Font.BOLD, INK_400);
    Font valueFont = new Font(Font.HELVETICA, 8, Font.NORMAL, INK_900);

    addCompanyRow(bar, "Address", COMPANY_ADDRESS, "Email", COMPANY_EMAIL, labelFont, valueFont);
    addCompanyRow(bar, "Phone", COMPANY_PHONE, "GSTIN", COMPANY_GSTIN, labelFont, valueFont);
    addCompanyRow(bar, "Website", COMPANY_WEBSITE, "", "", labelFont, valueFont);

    doc.add(bar);

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(6);
    doc.add(spacer);

    addDivider(doc);
  }

  private void addCompanyRow(
      PdfPTable table, String l1, String v1, String l2, String v2, Font labelFont, Font valueFont) {
    PdfPCell c1 = new PdfPCell();
    c1.setBorder(Rectangle.NO_BORDER);
    c1.setPadding(3);

    Paragraph p1 = new Paragraph();
    p1.add(new Chunk(l1 + ": ", labelFont));
    p1.add(new Chunk(v1, valueFont));
    c1.addElement(p1);
    table.addCell(c1);

    PdfPCell c2 = new PdfPCell();
    c2.setBorder(Rectangle.NO_BORDER);
    c2.setPadding(3);

    Paragraph p2 = new Paragraph();
    if (l2 != null && !l2.isBlank()) {
      p2.add(new Chunk(l2 + ": ", labelFont));
      p2.add(new Chunk(v2 != null ? v2 : "", valueFont));
    }
    c2.addElement(p2);
    table.addCell(c2);
  }

  private void addOrderMeta(Document doc, Payment payment) throws DocumentException {
    PdfPTable row = new PdfPTable(2);
    row.setWidthPercentage(100);
    row.setWidths(new float[] {1, 1});

    PdfPCell left = new PdfPCell();
    left.setBorder(Rectangle.NO_BORDER);
    left.setPadding(4);

    PdfPCell right = new PdfPCell();
    right.setBorder(Rectangle.NO_BORDER);
    right.setPadding(4);

    Font labelFont = new Font(Font.HELVETICA, 8, Font.BOLD, INK_400);
    Font valueFont = new Font(Font.HELVETICA, 9, Font.BOLD, INK_900);

    addMetaLine(
        left,
        "Invoice #",
        payment.getInvoiceNumber() != null ? payment.getInvoiceNumber() : "—",
        labelFont,
        valueFont);
    addMetaLine(left, "Order #", nvl(payment.getGatewayOrderId()), labelFont, valueFont);
    addMetaLine(
        left,
        "Invoice Date",
        payment.getCreatedAt() != null ? payment.getCreatedAt().format(DATE_FMT) : "—",
        labelFont,
        valueFont);

    addMetaLine(right, "Payment Mode", nvl(payment.getGateway()), labelFont, valueFont);
    addMetaLine(right, "Status", nvl(payment.getStatus()), labelFont, valueFont);
    addMetaLine(
        right,
        "Gateway Txn",
        nvl(payment.getGatewayPaymentId()),
        labelFont,
        valueFont);

    row.addCell(left);
    row.addCell(right);

    doc.add(row);

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(6);
    doc.add(spacer);

    addDivider(doc);
  }

  private void addMetaLine(
      PdfPCell parent, String label, String value, Font labelFont, Font valueFont) {
    Paragraph p = new Paragraph();
    p.setSpacingAfter(3);
    p.add(new Chunk(label + ":  ", labelFont));
    p.add(new Chunk(value, valueFont));
    parent.addElement(p);
  }

  private void addBilledTo(Document doc, Payment payment) throws DocumentException {
    Font headingFont = new Font(Font.HELVETICA, 9, Font.BOLD, PRIMARY);
    Paragraph heading = new Paragraph("BILLED TO", headingFont);
    heading.setSpacingAfter(6);
    doc.add(heading);

    Font nameFont = new Font(Font.HELVETICA, 11, Font.BOLD, INK_900);
    Font detailFont = new Font(Font.HELVETICA, 9, Font.NORMAL, INK_500);

    doc.add(new Paragraph(nvl(payment.getCustomerName()), nameFont));

    if (payment.getCustomerEmail() != null && !payment.getCustomerEmail().isBlank()) {
      Paragraph email = new Paragraph(payment.getCustomerEmail(), detailFont);
      email.setSpacingBefore(2);
      doc.add(email);
    }
    if (payment.getCustomerPhone() != null && !payment.getCustomerPhone().isBlank()) {
      Paragraph phone = new Paragraph(payment.getCustomerPhone(), detailFont);
      phone.setSpacingBefore(1);
      doc.add(phone);
    }

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(6);
    doc.add(spacer);

    addDivider(doc);
  }

  private void addLineItems(Document doc, Payment payment) throws DocumentException {
    Font headingFont = new Font(Font.HELVETICA, 9, Font.BOLD, PRIMARY);
    Paragraph heading = new Paragraph("ORDER ITEMS", headingFont);
    heading.setAlignment(Element.ALIGN_CENTER);
    heading.setSpacingAfter(6);
    doc.add(heading);

    PdfPTable table = new PdfPTable(3);
    table.setWidthPercentage(100);
    table.setWidths(new float[] {4, 1, 1.5f});

    Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, INK_900);
    Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, INK_500);

    PdfPCell descHeader = new PdfPCell(new Phrase("DESCRIPTION", headerFont));
    descHeader.setBackgroundColor(NAVY);
    descHeader.setPadding(9);
    descHeader.setBorder(Rectangle.NO_BORDER);
    table.addCell(descHeader);

    PdfPCell taxHeader = new PdfPCell(new Phrase("TAX", headerFont));
    taxHeader.setBackgroundColor(NAVY);
    taxHeader.setPadding(9);
    taxHeader.setBorder(Rectangle.NO_BORDER);
    taxHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
    table.addCell(taxHeader);

    PdfPCell amtHeader = new PdfPCell(new Phrase("AMOUNT", headerFont));
    amtHeader.setBackgroundColor(NAVY);
    amtHeader.setPadding(9);
    amtHeader.setBorder(Rectangle.NO_BORDER);
    amtHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
    table.addCell(amtHeader);

    PdfPCell descCell = new PdfPCell();
    descCell.setPadding(10);
    descCell.setBorderColor(BORDER);
    descCell.addElement(new Paragraph(buildDescription(payment), bodyFont));

    String subline = buildSubline(payment);
    if (subline != null) {
      Paragraph sub = new Paragraph(subline, smallFont);
      sub.setSpacingBefore(3);
      descCell.addElement(sub);
    }
    table.addCell(descCell);

    String taxText =
        payment.getGstRate() != null
            ? payment.getGstRate().stripTrailingZeros().toPlainString() + "%"
            : "18%";
    PdfPCell taxCell = new PdfPCell(new Phrase(taxText, bodyFont));
    taxCell.setPadding(10);
    taxCell.setBorderColor(BORDER);
    taxCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    table.addCell(taxCell);

    PdfPCell amtCell =
        new PdfPCell(
            new Phrase(
                "Rs. "
                    + formatAmount(
                        payment.getBaseAmount() != null
                            ? payment.getBaseAmount()
                            : payment.getAmount()),
                bodyFont));
    amtCell.setPadding(10);
    amtCell.setBorderColor(BORDER);
    amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    table.addCell(amtCell);

    doc.add(table);

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(10);
    doc.add(spacer);
  }

  private void addSummary(Document doc, Payment payment) throws DocumentException {
    PdfPTable totals = new PdfPTable(2);
    totals.setWidthPercentage(50);
    totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
    totals.setWidths(new float[] {1.5f, 1});

    Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL, INK_500);
    Font valueFont = new Font(Font.HELVETICA, 10, Font.BOLD, INK_900);
    Font totalLabelFont = new Font(Font.HELVETICA, 11, Font.BOLD, NAVY);
    Font totalValueFont = new Font(Font.HELVETICA, 12, Font.BOLD, NAVY);

    BigDecimal base =
        payment.getBaseAmount() != null ? payment.getBaseAmount() : payment.getAmount();
    BigDecimal gst = payment.getGstAmount() != null ? payment.getGstAmount() : BigDecimal.ZERO;
    BigDecimal total = payment.getAmount();
    BigDecimal rate = payment.getGstRate() != null ? payment.getGstRate() : new BigDecimal("18");

    addTotalRow(totals, "Subtotal", "Rs. " + formatAmount(base), labelFont, valueFont);
    addTotalRow(
        totals,
        "GST (" + rate.stripTrailingZeros().toPlainString() + "%)",
        "Rs. " + formatAmount(gst),
        labelFont,
        valueFont);

    PdfPCell emptyL = new PdfPCell(new Phrase("", labelFont));
    emptyL.setBorder(Rectangle.NO_BORDER);
    emptyL.setBorderWidthTop(1.5f);
    emptyL.setBorderColorTop(NAVY);
    emptyL.setPadding(6);
    totals.addCell(emptyL);

    PdfPCell emptyR = new PdfPCell(new Phrase("", valueFont));
    emptyR.setBorder(Rectangle.NO_BORDER);
    emptyR.setBorderWidthTop(1.5f);
    emptyR.setBorderColorTop(NAVY);
    emptyR.setPadding(6);
    totals.addCell(emptyR);

    addTotalRow(
        totals, "GRAND TOTAL", "Rs. " + formatAmount(total), totalLabelFont, totalValueFont);

    doc.add(totals);

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(8);
    doc.add(spacer);

    String words = convertToWords(total);
    Font wordsLabelFont = new Font(Font.HELVETICA, 8, Font.BOLD, INK_400);
    Font wordsValueFont = new Font(Font.HELVETICA, 9, Font.ITALIC, INK_900);

    Paragraph amountWords = new Paragraph();
    amountWords.setAlignment(Element.ALIGN_RIGHT);
    amountWords.add(new Chunk("Amount in Words: ", wordsLabelFont));
    amountWords.add(new Chunk(words, wordsValueFont));
    doc.add(amountWords);

    Paragraph spacer2 = new Paragraph(" ");
    spacer2.setSpacingAfter(10);
    doc.add(spacer2);

    addDivider(doc);
  }

  private void addPaymentDetails(Document doc, Payment payment) throws DocumentException {
    Font headingFont = new Font(Font.HELVETICA, 9, Font.BOLD, PRIMARY);
    Paragraph heading = new Paragraph("PAYMENT DETAILS", headingFont);
    heading.setSpacingAfter(6);
    doc.add(heading);

    PdfPTable details = new PdfPTable(2);
    details.setWidthPercentage(100);
    details.setWidths(new float[] {1, 3});

    addDetailRow(details, "Gateway", nvl(payment.getGateway()));
    addDetailRow(details, "Payment ID", nvl(payment.getGatewayPaymentId()));
    addDetailRow(details, "UTR", nvl(payment.getUtr()));
    addDetailRow(
        details,
        "Completed At",
        payment.getCompletedAt() != null ? payment.getCompletedAt().format(DATETIME_FMT) : "—");

    if ("FAILED".equals(payment.getStatus()) && payment.getFailureReason() != null) {
      String reason = payment.getFailureReason();
      if (reason.length() > 200) reason = reason.substring(0, 200) + "...";
      addDetailRow(details, "Failure Reason", reason);
    }

    doc.add(details);

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(12);
    doc.add(spacer);

    addDivider(doc);
  }

  private void addFooter(Document doc) throws DocumentException {
    PdfPTable footer = new PdfPTable(2);
    footer.setWidthPercentage(100);
    footer.setWidths(new float[] {2, 1});

    PdfPCell left = new PdfPCell();
    left.setBorder(Rectangle.NO_BORDER);
    left.setPadding(4);

    Font labelFont = new Font(Font.HELVETICA, 8, Font.BOLD, INK_400);
    Font noteFont = new Font(Font.HELVETICA, 8, Font.NORMAL, INK_500);

    Paragraph policyLabel = new Paragraph("Returns Policy", labelFont);
    policyLabel.setSpacingAfter(2);
    left.addElement(policyLabel);

    Paragraph policy =
        new Paragraph(
            "Digital products are non-refundable once delivered. "
                + "For support, contact "
                + COMPANY_EMAIL
                + ".",
            noteFont);
    left.addElement(policy);

    footer.addCell(left);

    PdfPCell right = new PdfPCell();
    right.setBorder(Rectangle.NO_BORDER);
    right.setPadding(4);
    right.setHorizontalAlignment(Element.ALIGN_RIGHT);

    Font sigNameFont = new Font(Font.HELVETICA, 9, Font.BOLD, NAVY);
    Paragraph sigName = new Paragraph("FOR " + COMPANY_NAME, sigNameFont);
    sigName.setAlignment(Element.ALIGN_RIGHT);
    right.addElement(sigName);

    Paragraph sigLine = new Paragraph(" ");
    sigLine.setSpacingBefore(28);
    right.addElement(sigLine);

    Font sigFont = new Font(Font.HELVETICA, 8, Font.NORMAL, INK_500);
    Paragraph sign = new Paragraph("Authorized Signatory", sigFont);
    sign.setAlignment(Element.ALIGN_RIGHT);
    right.addElement(sign);

    footer.addCell(right);

    doc.add(footer);

    Paragraph spacer = new Paragraph(" ");
    spacer.setSpacingAfter(10);
    doc.add(spacer);

    PdfPTable thanks = new PdfPTable(1);
    thanks.setWidthPercentage(100);
    PdfPCell thanksCell = new PdfPCell();
    thanksCell.setBorder(Rectangle.NO_BORDER);
    thanksCell.setBorderWidthTop(0.8f);
    thanksCell.setBorderColorTop(BORDER);
    thanksCell.setPadding(10);

    Font thanksFont = new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY);
    Paragraph thanksP = new Paragraph("Thank you for choosing " + COMPANY_NAME + "!", thanksFont);
    thanksP.setAlignment(Element.ALIGN_CENTER);
    thanksCell.addElement(thanksP);

    Font thanksNoteFont = new Font(Font.HELVETICA, 8, Font.NORMAL, INK_400);
    Paragraph note =
        new Paragraph(
            "This is a system-generated invoice and does not require a physical signature.",
            thanksNoteFont);
    note.setAlignment(Element.ALIGN_CENTER);
    note.setSpacingBefore(3);
    thanksCell.addElement(note);

    thanks.addCell(thanksCell);
    doc.add(thanks);
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

  private void addTotalRow(
      PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
    PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
    lc.setBorder(Rectangle.NO_BORDER);
    lc.setPadding(5);
    table.addCell(lc);

    PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
    vc.setBorder(Rectangle.NO_BORDER);
    vc.setPadding(5);
    vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
    table.addCell(vc);
  }

  private void addDetailRow(PdfPTable table, String label, String value) {
    Font labelFont = new Font(Font.HELVETICA, 9, Font.NORMAL, INK_500);
    Font valueFont = new Font(Font.HELVETICA, 9, Font.BOLD, INK_900);

    PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
    lc.setBorder(Rectangle.NO_BORDER);
    lc.setPadding(3);
    table.addCell(lc);

    PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
    vc.setBorder(Rectangle.NO_BORDER);
    vc.setPadding(3);
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
        return "AI Chat Wallet Recharge - " + mins + " min";
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
      return "Lifetime access - Instant delivery to your email";
    }
    return null;
  }

  private String formatAmount(BigDecimal amount) {
    if (amount == null) return "0.00";
    return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private String nvl(String s) {
    return s != null && !s.isBlank() ? s : "-";
  }

  private String convertToWords(BigDecimal amount) {
    if (amount == null) return "Zero Rupees Only";

    long rupees = amount.longValue();
    long paise =
        amount
            .subtract(BigDecimal.valueOf(rupees))
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValue();

    StringBuilder sb = new StringBuilder();
    if (rupees == 0) {
      sb.append("Zero");
    } else {
      sb.append(numToWords(rupees));
    }
    sb.append(" Rupees");

    if (paise > 0) {
      sb.append(" and ").append(numToWords(paise)).append(" Paise");
    }

    sb.append(" Only");
    return sb.toString();
  }

  private String numToWords(long n) {
    if (n == 0) return "";

    String[] ones = {
      "",
      "One",
      "Two",
      "Three",
      "Four",
      "Five",
      "Six",
      "Seven",
      "Eight",
      "Nine",
      "Ten",
      "Eleven",
      "Twelve",
      "Thirteen",
      "Fourteen",
      "Fifteen",
      "Sixteen",
      "Seventeen",
      "Eighteen",
      "Nineteen"
    };
    String[] tens = {
      "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    if (n < 20) return ones[(int) n];
    if (n < 100) return tens[(int) (n / 10)] + (n % 10 != 0 ? " " + ones[(int) (n % 10)] : "");

    if (n < 1000) {
      return ones[(int) (n / 100)] + " Hundred" + (n % 100 != 0 ? " " + numToWords(n % 100) : "");
    }
    if (n < 100_000) {
      return numToWords(n / 1000) + " Thousand" + (n % 1000 != 0 ? " " + numToWords(n % 1000) : "");
    }
    if (n < 10_000_000) {
      return numToWords(n / 100_000)
          + " Lakh"
          + (n % 100_000 != 0 ? " " + numToWords(n % 100_000) : "");
    }
    return numToWords(n / 10_000_000)
        + " Crore"
        + (n % 10_000_000 != 0 ? " " + numToWords(n % 10_000_000) : "");
  }
}