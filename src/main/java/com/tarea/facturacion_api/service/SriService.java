package com.tarea.facturacion_api.service;

import com.tarea.facturacion_api.model.DetalleFactura;
import com.tarea.facturacion_api.model.Factura;
import com.tarea.facturacion_api.model.Configuracion;
import com.tarea.facturacion_api.repository.ConfiguracionRepository;
import com.tarea.facturacion_api.repository.FacturaRepository;
import com.tarea.facturacion_api.sri.model.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SriService {

    @Autowired private FacturaRepository facturaRepository;
    @Autowired private WhatsappService whatsappService;
    @Autowired private FirmaService firmaService;
    @Autowired private SriSoapClient sriSoapClient;
    @Autowired private ConfiguracionRepository configRepo;

    public Factura procesarFacturaElectronica(Long idFactura) {
        System.out.println(">>> [SRI] Procesando Factura ID: " + idFactura);
        
        Factura factura = facturaRepository.findById(idFactura)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

        Configuracion conf = configRepo.findById(1L).orElseThrow(() -> new RuntimeException("Falta Configuración"));

        try {
            String claveAcceso = generarClaveAccesoReal(factura.getId(), conf.getRuc());
            factura.setClaveAcceso(claveAcceso);

            String xml = generarXmlJaxb(factura, claveAcceso, conf);
            
            String firmaDigital = firmaService.firmarDatos(xml);
            String xmlParaEnviar = xml + "\n<!-- FIRMA DIGITAL: " + firmaDigital + " -->";
            
            factura.setXmlContenido(xmlParaEnviar);
            guardarXmlEnArchivo(xmlParaEnviar, claveAcceso);

            String respuestaSri = sriSoapClient.enviarComprobante(xmlParaEnviar);
            
            if (respuestaSri.contains("RECIBIDA") || respuestaSri.contains("AUTORIZADO")) {
                factura.setEstadoSri("AUTORIZADO"); 
                factura.setFechaAutorizacion(LocalDateTime.now());
                whatsappService.enviarNotificacion(factura);
            } else if (respuestaSri.contains("DEVUELTA")) {
                factura.setEstadoSri("DEVUELTA");
                factura.setFechaAutorizacion(LocalDateTime.now()); // Guardamos fecha de respuesta
            } else {
                factura.setEstadoSri("ERROR_ENVIO");
            }

        } catch (Exception e) {
            e.printStackTrace();
            factura.setEstadoSri("ERROR_INTERNO");
        }

        return facturaRepository.save(factura);
    }

    private String generarXmlJaxb(Factura f, String claveAcceso, Configuracion conf) throws Exception {
        FacturaXML xml = new FacturaXML();
        
        // Info Tributaria
        InfoTributariaXML infoT = new InfoTributariaXML();
        infoT.setAmbiente("1");
        infoT.setTipoEmision("1");
        infoT.setRazonSocial(conf.getNombreEmpresa());
        infoT.setNombreComercial(conf.getNombreEmpresa());
        infoT.setRuc(conf.getRuc());
        infoT.setClaveAcceso(claveAcceso);
        infoT.setCodDoc("01"); 
        infoT.setEstab("001");
        infoT.setPtoEmi("001");
        infoT.setSecuencial(String.format("%09d", f.getId()));
        infoT.setDirMatriz(conf.getDireccion());
        xml.setInfoTributaria(infoT);

        // DETALLES Y CÁLCULO DE IMPUESTOS
        double subtotalIva = 0.0;
        double subtotalCero = 0.0;
        double totalIva = 0.0;

        DetallesXML detallesContainer = new DetallesXML();
        List<DetalleXML> listaDetalles = new ArrayList<>();
        
        for (DetalleFactura det : f.getDetalles()) {
            DetalleXML d = new DetalleXML();
            d.setCodigoPrincipal(det.getProducto().getCodigoPrincipal()); // SKU Real
            d.setDescripcion(det.getProducto().getNombre());
            d.setCantidad(format(det.getCantidad()));
            d.setPrecioUnitario(format(det.getPrecioUnitario()));
            d.setDescuento("0.00");
            
            double totalLinea = det.getCantidad() * det.getPrecioUnitario();
            d.setPrecioTotalSinImpuesto(format(totalLinea));
            
            // Lógica de Impuestos
            String codigoPorcentaje = "0";
            String tarifa = "0.00";
            double valorIva = 0.0;

            if (det.getProducto().getGrabaIva()) {
                codigoPorcentaje = "4"; // 15%
                tarifa = "15.00";
                valorIva = totalLinea * 0.15;
                subtotalIva += totalLinea;
                totalIva += valorIva;
            } else {
                subtotalCero += totalLinea;
            }

            ImpuestoXML imp = new ImpuestoXML("2", codigoPorcentaje, tarifa, format(totalLinea), format(valorIva));
            d.setImpuestos(Arrays.asList(imp));
            listaDetalles.add(d);
        }
        detallesContainer.setDetalle(listaDetalles);
        xml.setDetalles(detallesContainer);

        // Info Factura (Totales)
        InfoFacturaXML infoF = new InfoFacturaXML();
        infoF.setFechaEmision(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        infoF.setDirEstablecimiento("Sucursal Centro");
        infoF.setObligadoContabilidad("NO");
        infoF.setTipoIdentificacionComprador("05");
        infoF.setRazonSocialComprador(f.getCliente().getNombre() + " " + f.getCliente().getApellido());
        String ident = f.getCliente().getCedula() != null ? f.getCliente().getCedula() : "9999999999";
        infoF.setIdentificacionComprador(ident);
        infoF.setTotalSinImpuestos(format(f.getTotal())); // Simplificado (debería ser subtotal real)
        infoF.setTotalDescuento("0.00");
        
        // Lista de Impuestos
        List<TotalImpuestoXML> totalImpuestos = new ArrayList<>();
        if (subtotalIva > 0) totalImpuestos.add(new TotalImpuestoXML("2", "4", format(subtotalIva), format(totalIva)));
        if (subtotalCero > 0) totalImpuestos.add(new TotalImpuestoXML("2", "0", format(subtotalCero), "0.00"));
        
        infoF.setTotalConImpuestos(totalImpuestos);
        
        infoF.setPropina("0.00");
        infoF.setImporteTotal(format(f.getTotal()));
        infoF.setMoneda("DOLAR");
        // CAMBIO AQUÍ: Usamos f.getFormaPago()
        // Si viene nulo por alguna razón, usamos "01" (Efectivo) por defecto
        //String codigoPago = f.getFormaPago() != null ? f.getFormaPago() : "01";
        infoF.setPagos(Arrays.asList(new PagoXML("01", format(f.getTotal()), "0", "DIAS")));
        
        xml.setInfoFactura(infoF);

        JAXBContext context = JAXBContext.newInstance(FacturaXML.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(xml, writer);
        return writer.toString();
    }

    private void guardarXmlEnArchivo(String xmlContent, String nombreArchivo) {
        try {
            Path path = Paths.get("xmls_generados");
            if (!Files.exists(path)) Files.createDirectory(path);
            File archivo = new File(path.toString(), nombreArchivo + ".xml");
            try (FileWriter writer = new FileWriter(archivo)) { writer.write(xmlContent); }
        } catch (Exception e) {}
    }

    private String generarClaveAccesoReal(Long idFactura, String rucEmisor) {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        String fecha = sdf.format(new Date());
        String clave48 = fecha + "01" + rucEmisor + "1" + "001001" + String.format("%09d", idFactura) + "12345678" + "1";
        return clave48 + calcularModulo11(clave48);
    }

    private String calcularModulo11(String clave) {
        int[] factores = {7, 6, 5, 4, 3, 2};
        int suma = 0;
        int indiceFactor = 0;
        for (int i = clave.length() - 1; i >= 0; i--) {
            int digito = Integer.parseInt(String.valueOf(clave.charAt(i)));
            suma += digito * factores[indiceFactor];
            indiceFactor++;
            if (indiceFactor == 6) indiceFactor = 0; 
        }
        int residuo = suma % 11;
        int resultado = 11 - residuo;
        if (resultado == 11) return "0";
        if (resultado == 10) return "1";
        return String.valueOf(resultado);
    }

    private String format(Number n) {
        return new BigDecimal(n.toString()).setScale(2, RoundingMode.HALF_UP).toString();
    }
}