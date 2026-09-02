package pe.edu.pucp.model.comun;

import java.time.LocalDateTime;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * Contrato de las entidades que conservan usuario y fecha de registro (RNF002).
 */
public interface Auditable {

    LocalDateTime getFechaRegistro();
    void setFechaRegistro(LocalDateTime fechaRegistro);
    Usuario getUsuarioRegistro();
    void setUsuarioRegistro(Usuario usuarioRegistro);
}
