package pe.edu.pucp.sigmeta.boimpl.reporte;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * RF008: representacion impresa del comprobante electronico (FACTURA o BOLETA) en PDF, con JasperReports.
 * Usa la plantilla precompilada /reports/comprobante.jasper (ver CompilarPlantillas) y el mismo exportador
 * que los reportes de RF015. Recibe los datos ya armados por ComprobanteBOImpl; aqui no se calcula nada.
 */
public final class ComprobantePdf {

    private static final Locale PERU = Locale.forLanguageTag("es-PE");

    private ComprobantePdf() {
    }

    /**
     * @param cabecera TITULO, NUMERO, CLIENTE, TIPO_DOCUMENTO, DOCUMENTO, DIRECCION, FECHA_EMISION,
     *                 CONDICION_PAGO, VENTA (textos) y SUB_TOTAL, IGV, TOTAL (montos)
     * @param lineas   numeroLinea, codigo, descripcion, cantidad, precioUnitario, descuento, importe
     * @param autor    quien descarga el comprobante (metadato del PDF)
     */
    public static byte[] generar(Map<String, Object> cabecera, List<Map<String, ?>> lineas, String autor) {
        Map<String, Object> parametros = new HashMap<>(cabecera);
        parametros.put(JRParameter.REPORT_LOCALE, PERU);
        String titulo = String.valueOf(cabecera.get("TITULO"));
        String numero = String.valueOf(cabecera.get("NUMERO"));
        try {
            JasperPrint impreso = JasperFillManager.fillReport(ReportePdf.plantilla("comprobante"), parametros,
                    new JRMapCollectionDataSource(lineas));
            return ReportePdf.exportar(impreso, titulo + " " + numero,
                    "Comprobante electrónico " + numero + " (RF008)", autor);
        } catch (JRException e) {
            throw new RuntimeException("No se pudo generar el PDF del comprobante " + numero + ": " + e.getMessage(), e);
        }
    }
}
