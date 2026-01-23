package com.tarea.facturacion_api.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tarea.facturacion_api.model.*;
import com.tarea.facturacion_api.repository.ConfiguracionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Stream;

@Service
public class PdfService {

    @Autowired
    private ConfiguracionRepository configRepo;

    // Helper para obtener config o defaults
    private Configuracion getConfig() {
        return configRepo.findById(1L).orElseGet(() -> {
            Configuracion c = new Configuracion();
            c.setNombreEmpresa("Mi Empresa");
            c.setRuc("9999999999001");
            c.setDireccion("Matriz Cuenca-Ecuador");
            c.setTelefono("+593 978958721");
            return c;
        });
    }

    public ByteArrayInputStream generarFacturaPdf(Factura factura) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Configuracion conf = getConfig();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Cabecera Empresa
            Font fontEmpresa = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Paragraph empresa = new Paragraph(conf.getNombreEmpresa(), fontEmpresa);
            empresa.setAlignment(Element.ALIGN_CENTER);
            document.add(empresa);
            
            Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Paragraph datosEmpresa = new Paragraph("RUC: " + conf.getRuc() + "\n" + conf.getDireccion() + "\nTelf: " + conf.getTelefono(), fontSub);
            datosEmpresa.setAlignment(Element.ALIGN_CENTER);
            document.add(datosEmpresa);
            document.add(Chunk.NEWLINE);

            // Datos Factura
            Font fontDatos = FontFactory.getFont(FontFactory.HELVETICA, 11);
            document.add(new Paragraph("Factura No: " + String.format("%09d", factura.getId()), fontDatos));
            document.add(new Paragraph("Fecha: " + factura.getFecha().toString(), fontDatos));
            document.add(new Paragraph("Cliente: " + factura.getCliente().getNombre() + " " + factura.getCliente().getApellido(), fontDatos));
            document.add(new Paragraph("CI/RUC: " + factura.getCliente().getCedula(), fontDatos));
            document.add(new Paragraph("Dirección: " + factura.getCliente().getDireccion(), fontDatos));
            document.add(Chunk.NEWLINE);

            // Tabla
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{1, 4, 2, 2});

            Stream.of("Cant", "Descripción", "P. Unit", "Total")
                .forEach(headerTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(Color.LIGHT_GRAY);
                    header.setPhrase(new Phrase(headerTitle));
                    table.addCell(header);
                });

            for (DetalleFactura det : factura.getDetalles()) {
                table.addCell(String.valueOf(det.getCantidad()));
                table.addCell(det.getProducto().getNombre());
                table.addCell(String.format("$%.2f", det.getPrecioUnitario()));
                table.addCell(String.format("$%.2f", det.getCantidad() * det.getPrecioUnitario()));
            }
            document.add(table);

            // Totales
            document.add(Chunk.NEWLINE);
            Paragraph total = new Paragraph("TOTAL: $" + String.format("%.2f", factura.getTotal()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);
            
            // Método de Pago
            document.add(Chunk.NEWLINE);
            String metodoPagoText = obtenerTextoMetodoPago(factura.getMetodoPago(), factura.getDetallesPago());
            Paragraph metodoPago = new Paragraph(metodoPagoText, FontFactory.getFont(FontFactory.HELVETICA, 10));
            document.add(metodoPago);
            
            // Pie de página (Clave Acceso)
            if(factura.getClaveAcceso() != null) {
                document.add(Chunk.NEWLINE);
                Paragraph clave = new Paragraph("Autorización SRI (Clave de Acceso):", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8));
                document.add(clave);
                document.add(new Paragraph(factura.getClaveAcceso(), FontFactory.getFont(FontFactory.COURIER, 8)));
            }

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    // Helper para obtener el texto del método de pago
    private String obtenerTextoMetodoPago(String metodoPago, String detallesPago) {
        StringBuilder sb = new StringBuilder();
        sb.append("Método de Pago: ");
        
        if (metodoPago == null || metodoPago.isEmpty()) {
            return sb.append("No especificado").toString();
        }
        
        try {
            if (metodoPago.equalsIgnoreCase("efectivo")) {
                sb.append("EFECTIVO");
                // Podrías parsear detallesPago si tuviera info del cambio, pero es JSON
            } else if (metodoPago.equalsIgnoreCase("tarjeta")) {
                sb.append("TARJETA");
                if (detallesPago != null && detallesPago.contains("ultimosCuatroDigitos")) {
                    // Extracto simple del JSON (en un proyecto real, usar ObjectMapper)
                    int idx = detallesPago.indexOf("ultimosCuatroDigitos");
                    if (idx != -1) {
                        String digitos = detallesPago.substring(idx + 25, Math.min(idx + 30, detallesPago.length()));
                        sb.append(" (....").append(digitos).append(")");
                    }
                }
            } else if (metodoPago.equalsIgnoreCase("transferencia")) {
                sb.append("TRANSFERENCIA BANCARIA");
                if (detallesPago != null && detallesPago.contains("banco")) {
                    int idx = detallesPago.indexOf("banco");
                    if (idx != -1) {
                        int inicio = detallesPago.indexOf(":", idx) + 1;
                        int fin = detallesPago.indexOf(",", inicio);
                        if (fin == -1) fin = detallesPago.indexOf("}", inicio);
                        String banco = detallesPago.substring(inicio, fin).replaceAll("\"", "").trim();
                        sb.append(" - Banco: ").append(banco);
                    }
                }
            } else {
                sb.append(metodoPago);
            }
        } catch (Exception e) {
            sb.append(metodoPago);
        }
        
        return sb.toString();
    }

    // --- REPORTE 2: LISTADO DE CLIENTES (Nuevo) ---
    public ByteArrayInputStream generarReporteClientes(List<Cliente> clientes) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Paragraph titulo = new Paragraph("Reporte de Clientes", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4); // ID, Nombre, Email, Dirección
            table.setWidthPercentage(100);
            table.setWidths(new int[]{1, 3, 3, 3});

            Stream.of("ID", "Nombre Completo", "Email", "Dirección")
                .forEach(headerTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(Color.ORANGE);
                    header.setPhrase(new Phrase(headerTitle));
                    table.addCell(header);
                });

            for (Cliente cliente : clientes) {
                table.addCell(String.valueOf(cliente.getId()));
                table.addCell(cliente.getNombre() + " " + cliente.getApellido());
                table.addCell(cliente.getEmail());
                table.addCell(cliente.getDireccion());
            }
            document.add(table);
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    // --- REPORTE 3: LISTADO DE PRODUCTOS (Nuevo) ---
    public ByteArrayInputStream generarReporteProductos(List<Producto> productos) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Paragraph titulo = new Paragraph("Inventario de Productos", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4); // ID, Nombre, Precio, Stock
            table.setWidthPercentage(100);
            table.setWidths(new int[]{1, 4, 2, 2});

            Stream.of("ID", "Producto", "Precio", "Stock")
                .forEach(headerTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(Color.CYAN);
                    header.setPhrase(new Phrase(headerTitle));
                    table.addCell(header);
                });

            for (Producto prod : productos) {
                table.addCell(String.valueOf(prod.getId()));
                table.addCell(prod.getNombre());
                table.addCell("$" + prod.getPrecio());
                table.addCell(String.valueOf(prod.getStock()));
            }
            document.add(table);
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    
}