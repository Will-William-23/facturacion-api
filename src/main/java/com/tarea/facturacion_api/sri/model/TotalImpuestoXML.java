package com.tarea.facturacion_api.sri.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"codigo", "codigoPorcentaje", "baseImponible", "valor"})
public class TotalImpuestoXML {
    private String codigo;
    private String codigoPorcentaje;
    private String baseImponible;
    private String valor;
}
