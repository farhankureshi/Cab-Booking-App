package com.cabgo.service;

import com.cabgo.model.Ride;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Invoice PDF generation with OpenPDF (the maintained iText 4 fork). */
@Service
public class ReceiptService {
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());

    public byte[] pdf(Ride r) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph("CabGo - Ride Receipt", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22)));
            doc.add(new Paragraph(" "));
            Font f = FontFactory.getFont(FontFactory.HELVETICA, 11);
            PdfPTable t = new PdfPTable(2);
            t.setWidthPercentage(100);
            row(t, f, "Receipt no.", "CG-" + r.getId());
            row(t, f, "Date", FMT.format(r.getCreatedAt()));
            row(t, f, "Rider", r.getRider().getName());
            row(t, f, "Driver", r.getDriver() == null ? "-" : r.getDriver().getUser().getName());
            row(t, f, "Vehicle", r.getDriver() == null ? "-" : r.getDriver().getVehicleModel() + " " + r.getDriver().getVehiclePlate());
            row(t, f, "Pickup", r.getPickupAddress());
            row(t, f, "Drop", r.getDropAddress());
            row(t, f, "Distance", String.format("%.1f km", r.getDistanceKm()));
            if (r.getDiscount() > 0) row(t, f, "Discount (" + r.getPromoCode() + ")", String.format("INR %.2f", r.getDiscount()));
            row(t, f, "Total fare", String.format("INR %.2f", r.getFare()));
            row(t, f, "Payment", r.isPaid() ? "PAID" : "PENDING");
            doc.add(t);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Thank you for riding with CabGo.", f));
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not generate receipt", e);
        }
        return out.toByteArray();
    }

    private void row(PdfPTable t, Font f, String k, String v) {
        t.addCell(new Phrase(k, f));
        t.addCell(new Phrase(v == null ? "-" : v, f));
    }
}
