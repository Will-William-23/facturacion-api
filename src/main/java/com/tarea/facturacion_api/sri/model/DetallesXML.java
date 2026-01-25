package com.tarea.facturacion_api.sri.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import lombok.Data;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class DetallesXML {
    @XmlElement(name = "detalle")
    private List<DetalleXML> detalle;
}
