package com.tarea.facturacion_api.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import xades4j.algorithms.EnvelopedSignatureTransform;
import xades4j.production.DataObjectReference;
import xades4j.production.SignedDataObjects;
import xades4j.production.XadesBesSigningProfile;
import xades4j.production.XadesSigner;
import xades4j.properties.ObjectIdentifier;
import xades4j.providers.KeyingDataProvider;
// import xades4j.providers.impl.FileSystemKeyStoreKeyingDataProvider; 
// import xades4j.providers.impl.KeyStoreKeyingDataProvider;
// import xades4j.providers.impl.PKCS11Provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class XadesService {

    @org.springframework.beans.factory.annotation.Value("${sri.firma.clave}")
    private String CLAVE_FIRMA;

    @org.springframework.beans.factory.annotation.Value("${sri.firma.ruta}")
    private String RUTA_FIRMA;

    /**
     * Firma un contenido XML usando XAdES-BES
     * 
     * @param xmlContent El XML en String crudo
     * @return El XML firmado con <ds:Signature> incrustado
     */
    public String firmarXml(String xmlContent) throws Exception {
        System.out.println(">>> [XADÉ] Iniciando proceso de firma XAdES-BES...");

        // 1. Cargar el XML en un Documento DOM
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // CRUCIAL para firmas XML
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));
        Element elemToSign = doc.getDocumentElement();

        // 2. Preparar el proveedor de laves (KeyingProvider) desde el .p12
        KeyStore ks = KeyStore.getInstance("PKCS12");
        // FIX: Usar la ruta inyectada, asegurando que cargue del classpath o file
        // system
        // Si esta en resources raiz, ClassPathResource deberia encontrarlo
        InputStream is = new ClassPathResource(RUTA_FIRMA).getInputStream();
        ks.load(is, CLAVE_FIRMA.toCharArray());

        // Buscar el alias automáticamente (la llave dentro del archivo)
        String alias = null;
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            alias = aliases.nextElement();
            if (ks.isKeyEntry(alias))
                break;
        }
        final String finalAlias = alias; // Necesario para usar dentro de la clase anónima

        // 3. Configurar el proveedor de claves
        // Usamos una implementación simplificada directa
        KeyingDataProvider kp = new KeyingDataProvider() {
            @Override
            public java.util.List<X509Certificate> getSigningCertificateChain() {
                try {
                    java.security.cert.Certificate cert = ks.getCertificate(finalAlias);
                    return java.util.Collections.singletonList((X509Certificate) cert);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public PrivateKey getSigningKey(X509Certificate certificate) {
                try {
                    return (PrivateKey) ks.getKey(finalAlias, CLAVE_FIRMA.toCharArray());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // 4. Configurar el Perfil XAdES-BES
        XadesBesSigningProfile p = new XadesBesSigningProfile(kp);

        // 5. Crear el Firmador
        XadesSigner signer = p.newSigner();

        // 6. ¡FIRMAR!
        // signer.sign(DataObjectDesc, Node)
        // Manualmente definimos la referencia para Enveloped Signature
        DataObjectReference obj = new DataObjectReference("");
        obj.withTransform(new EnvelopedSignatureTransform());
        signer.sign(new SignedDataObjects(obj), elemToSign);

        // 6. Convertir DOM modificado a String
        return domToString(doc);
    }

    private String domToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        // Opcional: Indentar para que se vea bonito (el SRI a veces se queja si hay
        // mucho espacio, mejor plano)
        // transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        java.io.StringWriter writer = new java.io.StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
