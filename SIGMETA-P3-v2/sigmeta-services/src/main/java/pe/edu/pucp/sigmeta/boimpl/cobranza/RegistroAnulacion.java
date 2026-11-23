package pe.edu.pucp.sigmeta.boimpl.cobranza;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import pe.edu.pucp.sigmeta.dao.ConsultaDAO;
import pe.edu.pucp.sigmeta.daoimpl.ConsultaDAOImpl;

/**
 * RNF001, RF009, RF013: deja constancia de quien, cuando y por que se anulo un cobro, una nota de credito o una
 * cuenta por cobrar (tabla registro_anulacion, procedimiento sp_anulacion_registrar). Se llama DENTRO de la
 * transaccion de la anulacion: si la anulacion falla, tampoco queda el registro (RNF002).
 * La fecha la pone la base (NOW()), igual que cobro.fecha_anulacion.
 */
public final class RegistroAnulacion {

    public static final String COBRO = "COBRO";
    public static final String NOTA_CREDITO = "NOTA_CREDITO";
    public static final String CUENTA_POR_COBRAR = "CUENTA_POR_COBRAR";

    private static final ConsultaDAO CONSULTA = new ConsultaDAOImpl();

    private RegistroAnulacion() {
    }

    public static void registrar(String tipo, int idDocumento, String documento, Integer idCuentaPorCobrar,
                                 double monto, String motivo, int idUsuario) throws SQLException {
        String texto = documento == null || documento.isBlank() ? tipo + " " + idDocumento : documento.trim();
        CONSULTA.ejecutar("sp_anulacion_registrar", tipo, idDocumento,
                texto.length() > 40 ? texto.substring(0, 40) : texto, idCuentaPorCobrar,
                BigDecimal.valueOf(monto).setScale(2, RoundingMode.HALF_UP), motivo.trim(), idUsuario);
    }
}
