package com.tarea.facturacion_api.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tarea.facturacion_api.model.Cliente;
import com.tarea.facturacion_api.model.DetalleFactura;
import com.tarea.facturacion_api.model.Factura;
import com.tarea.facturacion_api.model.Producto;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Stream;

@Service
public class PdfService {

    public ByteArrayInputStream generarFacturaPdf(Factura factura) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fuentes
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font fontDatos = FontFactory.getFont(FontFactory.HELVETICA, 12);

            // Encabezado
            Paragraph titulo = new Paragraph("MI EMPRESA S.A.", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Paragraph ruc = new Paragraph("RUC: 9999999999001",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY));
            ruc.setAlignment(Element.ALIGN_CENTER);
            document.add(ruc);

            Paragraph matriz = new Paragraph("Matriz Ecuador",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY));
            matriz.setAlignment(Element.ALIGN_CENTER);
            document.add(matriz);

            Paragraph telf = new Paragraph("Telf: 0999999999",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY));
            telf.setAlignment(Element.ALIGN_CENTER);
            document.add(telf);

            document.add(Chunk.NEWLINE);

            // Datos Factura
            document.add(new Paragraph("Factura No: " + String.format("%09d", factura.getId()), fontDatos));
            document.add(new Paragraph("Fecha: " + factura.getFecha().toString(), fontDatos));
            document.add(new Paragraph(
                    "Cliente: " + factura.getCliente().getNombre() + " " + factura.getCliente().getApellido(),
                    fontDatos));
            document.add(new Paragraph("CI/RUC: " + factura.getCliente().getCedula(), fontDatos));
            document.add(new Paragraph("Dirección: " + factura.getCliente().getDireccion(), fontDatos));
            document.add(Chunk.NEWLINE);

            // Tabla
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new int[] { 1, 4, 2, 2 });

            Stream.of("Cant", "Descripción", "P. Unit", "Total")
                    .forEach(headerTitle -> {
                        PdfPCell header = new PdfPCell();
                        header.setBackgroundColor(Color.LIGHT_GRAY);
                        header.setPhrase(new Phrase(headerTitle));
                        table.addCell(header);
                    });

            for (DetalleFactura det : factura.getDetalles()) {
                // DEBUG PDF
                System.out.println(">>> [DEBUG PDF] Detalle ID: " + det.getId());
                if (det.getProducto() != null) {
                    System.out.println(">>> [DEBUG PDF] Producto: " + det.getProducto().getNombre() + " (ID: "
                            + det.getProducto().getId() + ")");
                } else {
                    System.err.println(">>> [DEBUG PDF] Producto es NULL en Detalle ID: " + det.getId());
                }

                table.addCell(String.valueOf(det.getCantidad()));
                table.addCell(det.getProducto() != null ? det.getProducto().getNombre() : "Producto Eliminado");
                table.addCell(String.format("$%.2f", det.getPrecioUnitario()));
                table.addCell(String.format("$%.2f", det.getCantidad() * det.getPrecioUnitario()));
            }
            document.add(table);

            // Totales
            document.add(Chunk.NEWLINE);
            Paragraph total = new Paragraph("TOTAL: $" + String.format("%.2f", factura.getTotal()),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            // Pie de página (Clave Acceso)
            if (factura.getClaveAcceso() != null) {
                document.add(Chunk.NEWLINE);
                Paragraph clave = new Paragraph("Autorización SRI (Clave de Acceso):",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8));
                document.add(clave);
                document.add(new Paragraph(factura.getClaveAcceso(), FontFactory.getFont(FontFactory.COURIER, 8)));
            }

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // --- REPORTE DE CLIENTES ---
    public ByteArrayInputStream generarReporteClientes(List<Cliente> clientes) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLUE);
            Paragraph titulo = new Paragraph("Reporte de Clientes", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            // Tabla
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new int[] { 3, 3, 2, 3 }); // Anchos relativos

            // Encabezados
            Stream.of("Nombre", "Apellido", "Cédula", "Email")
                    .forEach(headerTitle -> {
                        PdfPCell header = new PdfPCell();
                        header.setBackgroundColor(Color.LIGHT_GRAY);
                        header.setPhrase(new Phrase(headerTitle));
                        table.addCell(header);
                    });

            // Datos
            for (Cliente cliente : clientes) {
                table.addCell(cliente.getNombre());
                table.addCell(cliente.getApellido());
                table.addCell(cliente.getCedula());
                table.addCell(cliente.getEmail());
            }

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // --- REPORTE DE PRODUCTOS (INVENTARIO) ---
    public ByteArrayInputStream generarReporteProductos(List<Producto> productos) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
            Paragraph titulo = new Paragraph("Reporte de Inventario", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            // Tabla
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new int[] { 4, 2, 2, 2 }); // Anchos relativos

            // Encabezados
            Stream.of("Producto", "Precio", "Stock", "Tipo")
                    .forEach(headerTitle -> {
                        PdfPCell header = new PdfPCell();
                        header.setBackgroundColor(Color.ORANGE);
                        header.setPhrase(new Phrase(headerTitle));
                        table.addCell(header);
                    });

            // Datos
            for (Producto p : productos) {
                table.addCell(p.getNombre());
                table.addCell(String.format("$%.2f", p.getPrecio()));
                table.addCell(String.valueOf(p.getStock()));
                table.addCell(p.getTipo() != null ? p.getTipo() : "N/A");
            }

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}