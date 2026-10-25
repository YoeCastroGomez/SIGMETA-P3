package pe.edu.pucp.sigmeta.model.comun;

import java.time.LocalDateTime;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

/**
 * Contrato de las entidades que conservan usuario y fecha de registro (RNF002).
 */
public interface Auditable {

    LocalDateTime getFechaRegistro();
    void setFechaRegistro(LocalDateTime fechaRegistro);
    Usuario getUsuarioRegistro();
    void setUsuarioRegistro(Usuario usuarioRegistro);
}
