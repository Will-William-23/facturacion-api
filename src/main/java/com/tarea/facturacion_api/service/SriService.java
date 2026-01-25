package com.tarea.facturacion_api.service;

import com.tarea.facturacion_api.model.DetalleFactura;
import com.tarea.facturacion_api.model.Factura;
import com.tarea.facturacion_api.repository.FacturaRepository;
import com.tarea.facturacion_api.sri.model.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SriService {

    @Value("${sri.emisor.ruc}")
    private String emisorRuc;

    @Value("${sri.emisor.razonSocial}")
    private String emisorRazonSocial;

    @Value("${sri.emisor.nombreComercial}")
    private String emisorNombreComercial;

    @Value("${sri.emisor.direccion}")
    private String emisorDireccion;

    @Autowired
    private FacturaRepository facturaRepository;
    @Autowired
    private WhatsappService whatsappService;

    @Autowired
    private XadesService xadesService; // <--- NUEVO SERVICIO HARDCORE
    @Autowired
    private SriSoapClient sriSoapClient;

    public Factura procesarFacturaElectronica(Long idFactura) {
        System.out.println(">>> [SRI] Iniciando proceso REAL para factura ID: " + idFactura);

        Factura factura = facturaRepository.findById(idFactura)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

        try {
            // 1. Datos y Clave
            String ruc = (this.emisorRuc != null) ? this.emisorRuc : "1799999999001"; // Uso propiedad o fallback
            // Asegurarse de que el RUC sea valido (13 digitos)
            if (ruc.length() != 13)
                throw new RuntimeException("RUC configurado invalido: " + ruc);

            String ambiente = "1"; // Pruebas
            String serie = "001001"; // Estab + PtoEmi
            String secuencial = String.format("%09d", factura.getId());
            // Generar código numérico aleatorio para evitar colisión de claves (8 dígitos)
            String codigoNumerico = String.format("%08d", new Random().nextInt(99999999));
            String tipoEmision = "1"; // Normal

            String claveAcceso = generarClaveAcceso(new Date(), "01", ruc, ambiente, serie, secuencial, codigoNumerico,
                    tipoEmision);
            factura.setClaveAcceso(claveAcceso);

            // 2. XML Base (Sin Firmar)
            // Pasamos todos los datos para asegurar consistencia
            String xmlBase = generarXmlJaxb(factura, claveAcceso);

            // 3. Firma XAdES-BES (REAL)
            // Ya no usamos la simulación. Usamos la librería xades4j.
            String xmlFirmado = xadesService.firmarXml(xmlBase);

            // Guardamos el XML final
            factura.setXmlContenido(xmlFirmado);

            // 4. ENVÍO REAL AL WEB SERVICE
            // Enviamos el XML firmado tal cual nos lo devolvió XadesService
            String respuestaSri = sriSoapClient.enviarComprobante(xmlFirmado);

            // 5. Analizar Respuesta (Parsing básico)
            if (respuestaSri.contains("RECIBIDA") || respuestaSri.contains("AUTORIZADO")) {
                factura.setEstadoSri("ENVIADO_SRI"); // Cambiamos estado
                factura.setFechaAutorizacion(LocalDateTime.now());
                System.out.println(">>> [SRI] ¡Documento recibido por el SRI!");
                whatsappService.enviarNotificacion(factura);
            } else if (respuestaSri.contains("DEVUELTA")) {
                factura.setEstadoSri("DEVUELTA");
                System.err.println(">>> [SRI] Documento devuelto por errores.");

                // LOGGING RESPONSE
                try (java.io.FileWriter fw = new java.io.FileWriter("sri_debug.log", true);
                        java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
                    pw.println("--- SRI RESPONSE (DEVUELTA) " + java.time.LocalDateTime.now() + " ---");
                    pw.println(respuestaSri);
                    pw.println("--------------------------------------------------");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            } else {
                // Si la respuesta es extraña o error de conexión
                factura.setEstadoSri("ERROR_ENVIO");
                System.err.println(">>> [SRI] Respuesta desconocida: " + respuestaSri);

                // LOGGING RESPONSE (UNKNOWN)
                try (java.io.FileWriter fw = new java.io.FileWriter("sri_debug.log", true);
                        java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
                    pw.println("--- SRI RESPONSE (UNKNOWN) " + java.time.LocalDateTime.now() + " ---");
                    pw.println(respuestaSri);
                    pw.println("--------------------------------------------------");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        } catch (

        Exception e) {
            System.err.println(">>> [SRI] Error crítico: " + e.getMessage());
            e.printStackTrace();

            // LOGGING DEBUG
            try (java.io.FileWriter fw = new java.io.FileWriter("sri_debug.log", true);
                    java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
                pw.println("--- ERROR " + java.time.LocalDateTime.now() + " ---");
                e.printStackTrace(pw);
                pw.println("--------------------------------------------------");
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            factura.setEstadoSri("ERROR_INTERNO");
        }

        return facturaRepository.save(factura);
    }

    // --- MÉTODOS PRIVADOS (Se mantienen igual que antes) ---
    // (Copia aquí los métodos generarXmlJaxb, format y generarClaveAccesoSimulada
    // del archivo anterior)
    // OJO: Asegúrate de NO borrarlos. Si copias y pegas todo, asegúrate de incluir
    // estos métodos auxiliares abajo.

    private String generarXmlJaxb(Factura f, String claveAcceso) throws Exception {
        FacturaXML xml = new FacturaXML();

        InfoTributariaXML infoT = new InfoTributariaXML();
        infoT.setAmbiente("1");
        infoT.setTipoEmision("1");
        infoT.setRazonSocial((this.emisorRazonSocial != null) ? this.emisorRazonSocial : "EMPRESA PRUEBAS");
        infoT.setNombreComercial(
                (this.emisorNombreComercial != null) ? this.emisorNombreComercial : "COMERCIAL PRUEBAS");
        infoT.setRuc((this.emisorRuc != null) ? this.emisorRuc : "1799999999001");
        infoT.setClaveAcceso(claveAcceso);
        infoT.setCodDoc("01");
        infoT.setEstab("001");
        infoT.setPtoEmi("001");
        infoT.setSecuencial(String.format("%09d", f.getId()));
        infoT.setDirMatriz((this.emisorDireccion != null) ? this.emisorDireccion : "Direccion Pruebas");
        xml.setInfoTributaria(infoT);

        InfoFacturaXML infoF = new InfoFacturaXML();
        infoF.setFechaEmision(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        infoF.setDirEstablecimiento("Av. Sucursal");
        infoF.setObligadoContabilidad("NO");
        infoF.setTipoIdentificacionComprador("05");
        infoF.setRazonSocialComprador(f.getCliente().getNombre() + " " + f.getCliente().getApellido());
        String ident = f.getCliente().getCedula() != null ? f.getCliente().getCedula() : "9999999999";
        infoF.setIdentificacionComprador(ident);
        infoF.setTotalSinImpuestos(format(f.getTotal()));
        infoF.setTotalDescuento("0.00");
        infoF.setTotalConImpuestos(Arrays.asList(new TotalImpuestoXML("2", "4", format(f.getTotal()), "0.00")));
        infoF.setPropina("0.00");
        infoF.setImporteTotal(format(f.getTotal()));
        infoF.setMoneda("DOLAR");
        infoF.setPagos(Arrays.asList(new PagoXML("01", format(f.getTotal()), "0", "DIAS")));
        xml.setInfoFactura(infoF);

        DetallesXML detallesContainer = new DetallesXML();
        List<DetalleXML> listaDetalles = new ArrayList<>();
        for (DetalleFactura det : f.getDetalles()) {
            DetalleXML d = new DetalleXML();
            d.setCodigoPrincipal(det.getProducto().getId().toString());
            d.setDescripcion(det.getProducto().getNombre());
            d.setCantidad(format(det.getCantidad()));
            d.setPrecioUnitario(format(det.getPrecioUnitario()));
            d.setDescuento("0.00");
            d.setPrecioTotalSinImpuesto(format(det.getCantidad() * det.getPrecioUnitario()));
            d.setImpuestos(Arrays.asList(
                    new ImpuestoXML("2", "4", "15", format(det.getCantidad() * det.getPrecioUnitario()), "0.00")));
            listaDetalles.add(d);
        }
        detallesContainer.setDetalle(listaDetalles);
        xml.setDetalles(detallesContainer);

        JAXBContext context = JAXBContext.newInstance(FacturaXML.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(xml, writer);
        return writer.toString();
    }

    private String format(Number n) {
        return new BigDecimal(n.toString()).setScale(2, RoundingMode.HALF_UP).toString();
    }

    private String generarClaveAcceso(Date fecha, String tipo, String ruc, String ambiente, String serie,
            String secuencial, String codigoNumerico, String tipoEmision) {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        String fechaStr = sdf.format(fecha);

        StringBuilder clave = new StringBuilder();
        clave.append(fechaStr);
        clave.append(tipo);
        clave.append(ruc);
        clave.append(ambiente);
        clave.append(serie);
        clave.append(secuencial);
        clave.append(codigoNumerico);
        clave.append(tipoEmision);

        // Digito Verificador (Modulo 11)
        String verificador = calcularDigitoVerificador(clave.toString());
        clave.append(verificador);

        return clave.toString();
    }

    private String calcularDigitoVerificador(String clave) {
        int factor = 2;
        int suma = 0;

        for (int i = clave.length() - 1; i >= 0; i--) {
            int digito = Integer.parseInt(String.valueOf(clave.charAt(i)));
            suma += digito * factor;
            factor++;
            if (factor > 7)
                factor = 2;
        }

        int residuo = suma % 11;
        int verificador = 11 - residuo;

        if (verificador == 11)
            return "0";
        if (verificador == 10)
            return "1";
        return String.valueOf(verificador);
    }
}