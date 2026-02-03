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

    // Helper para obtener la configuración de la empresa o valores por defecto
    private Configuracion getConfig() {
        return configRepo.findById(1L).orElseGet(() -> {
            Configuracion c = new Configuracion();
            c.setNombreEmpresa("EMPRESA DEMO");
            c.setRuc("9999999999001");
            c.setDireccion("Sin Dirección");
            c.setTelefono("0000000000");
            return c;
        });
    }

    // --- REPORTE 1: FACTURA INDIVIDUAL ---
    public ByteArrayInputStream generarFacturaPdf(Factura factura) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Configuracion conf = getConfig();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. CABECERA EMPRESA (Dinámica desde Base de Datos)
            Font fontEmpresa = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Paragraph empresa = new Paragraph(conf.getNombreEmpresa(), fontEmpresa);
            empresa.setAlignment(Element.ALIGN_CENTER);
            document.add(empresa);
            
            Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            StringBuilder info = new StringBuilder();
            info.append("RUC: ").append(conf.getRuc()).append("\n");
            info.append(conf.getDireccion()).append("\n");
            info.append("Telf: ").append(conf.getTelefono());
            
            if(conf.getEmail() != null && !conf.getEmail().isEmpty()) {
                info.append("\nEmail: ").append(conf.getEmail());
            }
            if(conf.getSitioWeb() != null && !conf.getSitioWeb().isEmpty()) {
                info.append("\nWeb: ").append(conf.getSitioWeb());
            }

            Paragraph datosEmpresa = new Paragraph(info.toString(), fontSub);
            datosEmpresa.setAlignment(Element.ALIGN_CENTER);
            document.add(datosEmpresa);
            document.add(Chunk.NEWLINE);

            // 2. DATOS DEL CLIENTE Y FACTURA
            Font fontDatos = FontFactory.getFont(FontFactory.HELVETICA, 11);
            document.add(new Paragraph("Factura No: " + String.format("%09d", factura.getId()), fontDatos));
            document.add(new Paragraph("Fecha: " + factura.getFecha().toString(), fontDatos));
            document.add(new Paragraph("Cliente: " + factura.getCliente().getNombre() + " " + factura.getCliente().getApellido(), fontDatos));
            document.add(new Paragraph("CI/RUC: " + factura.getCliente().getCedula(), fontDatos));
            document.add(new Paragraph("Dirección: " + factura.getCliente().getDireccion(), fontDatos));
            
            if(factura.getCliente().getTelefono() != null) {
                document.add(new Paragraph("Teléfono: " + factura.getCliente().getTelefono(), fontDatos));
            }

            // Forma de Pago (Interpretación del código SRI)
            String pagoTexto = "Efectivo";
            if("19".equals(factura.getFormaPago())) pagoTexto = "Tarjeta Crédito/Débito";
            if("20".equals(factura.getFormaPago())) pagoTexto = "Transferencia Bancaria";
            document.add(new Paragraph("Forma de Pago: " + pagoTexto, fontDatos));
            
            document.add(Chunk.NEWLINE);

            // 3. TABLA DE PRODUCTOS
            PdfPTable table = new PdfPTable(5); // 5 columnas para incluir indicador de IVA
            table.setWidthPercentage(100);
            table.setWidths(new int[]{1, 4, 2, 1, 2}); // Anchos relativos

            // Encabezados
            Stream.of("Cant", "Descripción", "P. Unit", "IVA", "Total")
                .forEach(headerTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(Color.LIGHT_GRAY);
                    header.setPhrase(new Phrase(headerTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                    table.addCell(header);
                });

            // Filas
            for (DetalleFactura det : factura.getDetalles()) {
                table.addCell(String.valueOf(det.getCantidad()));
                table.addCell(det.getProducto().getNombre());
                table.addCell(String.format("$%.2f", det.getPrecioUnitario()));
                
                // Mostrar si el producto grava IVA
                String ivaMarca = (det.getProducto().getGrabaIva() != null && det.getProducto().getGrabaIva()) ? "15%" : "0%";
                table.addCell(ivaMarca);
                
                table.addCell(String.format("$%.2f", det.getCantidad() * det.getPrecioUnitario()));
            }
            document.add(table);

            // 4. TOTALES (Con desglose)
            document.add(Chunk.NEWLINE);
            
            // Cálculos de respaldo por si es una factura antigua sin desglose guardado
            double subtotal = factura.getSubtotal() != null ? factura.getSubtotal() : (factura.getTotal() / 1.15);
            double iva = factura.getTotalIva() != null ? factura.getTotalIva() : (factura.getTotal() - subtotal);
            
            // Tabla pequeña alineada a la derecha para los totales
            PdfPTable totalesTable = new PdfPTable(2);
            totalesTable.setWidthPercentage(40);
            totalesTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            totalesTable.addCell(getCellSinBorde("Subtotal:"));
            totalesTable.addCell(getCellSinBorde("$" + String.format("%.2f", subtotal)));
            
            totalesTable.addCell(getCellSinBorde("IVA (15%):"));
            totalesTable.addCell(getCellSinBorde("$" + String.format("%.2f", iva)));
            
            PdfPCell celdaTotalLabel = getCellSinBorde("TOTAL:");
            celdaTotalLabel.getPhrase().setFont(FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            totalesTable.addCell(celdaTotalLabel);
            
            PdfPCell celdaTotalValue = getCellSinBorde("$" + String.format("%.2f", factura.getTotal()));
            celdaTotalValue.getPhrase().setFont(FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            totalesTable.addCell(celdaTotalValue);
            
            document.add(totalesTable);
            
            // 5. PIE DE PÁGINA (Información del SRI)
            if(factura.getClaveAcceso() != null) {
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("Información Tributaria:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
                document.add(new Paragraph("Clave de Acceso: " + factura.getClaveAcceso(), FontFactory.getFont(FontFactory.COURIER, 8)));
                
                if(factura.getFechaAutorizacion() != null) {
                    document.add(new Paragraph("Fecha Autorización: " + factura.getFechaAutorizacion().toString(), FontFactory.getFont(FontFactory.COURIER, 8)));
                }
                
                // Estado del SRI
                if("DEVUELTA".equals(factura.getEstadoSri())) {
                    Font fontError = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.RED);
                    document.add(new Paragraph("ESTADO SRI: RECHAZADA", fontError));
                } else if ("AUTORIZADO".equals(factura.getEstadoSri())) {
                    Font fontOk = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(0, 100, 0)); // Verde oscuro
                    document.add(new Paragraph("ESTADO SRI: AUTORIZADO", fontOk));
                }
            }

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(out.toByteArray());
    }
    
    // Método auxiliar para celdas sin bordes (Totales)
    private PdfPCell getCellSinBorde(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    // --- REPORTE 2: LISTADO DE CLIENTES ---
    public ByteArrayInputStream generarReporteClientes(List<Cliente> clientes) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Configuracion conf = getConfig();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph empresa = new Paragraph(conf.getNombreEmpresa(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY));
            empresa.setAlignment(Element.ALIGN_CENTER);
            document.add(empresa);

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLUE);
            Paragraph titulo = new Paragraph("Reporte de Clientes", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4); 
            table.setWidthPercentage(100);
            table.setWidths(new int[]{2, 3, 3, 2});

            Stream.of("Cédula/RUC", "Nombre Completo", "Email", "Teléfono")
                .forEach(headerTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(Color.ORANGE);
                    header.setPhrase(new Phrase(headerTitle));
                    table.addCell(header);
                });

            for (Cliente cliente : clientes) {
                table.addCell(cliente.getCedula());
                table.addCell(cliente.getNombre() + " " + cliente.getApellido());
                table.addCell(cliente.getEmail());
                table.addCell(cliente.getTelefono());
            }
            document.add(table);
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    // --- REPORTE 3: LISTADO DE PRODUCTOS ---
    public ByteArrayInputStream generarReporteProductos(List<Producto> productos) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Configuracion conf = getConfig();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph empresa = new Paragraph(conf.getNombreEmpresa(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY));
            empresa.setAlignment(Element.ALIGN_CENTER);
            document.add(empresa);

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.GREEN);
            Paragraph titulo = new Paragraph("Inventario de Productos", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{2, 4, 2, 2});

            Stream.of("Código", "Producto", "Precio", "Stock")
                .forEach(headerTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(Color.CYAN);
                    header.setPhrase(new Phrase(headerTitle));
                    table.addCell(header);
                });

            for (Producto prod : productos) {
                table.addCell(prod.getCodigoPrincipal());
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