package pe.edu.pucp.sigmeta.boimpl.seguridad;

import pe.edu.pucp.sigmeta.bo.seguridad.RolBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.seguridad.RolDAO;
import pe.edu.pucp.sigmeta.daoimpl.seguridad.RolDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Rol;

import java.sql.SQLException;
import java.util.List;

// los roles son fijos (uno por TipoRol), por eso solo se consultan
public class RolBOImpl implements RolBO {

    private final RolDAO rolDAO;

    public RolBOImpl() {
        this.rolDAO = new RolDAOImpl();
    }

    // RF002
    @Override
    public Rol obtener(int idRol) throws SQLException {
        return rolDAO.load(idRol);
    }

    // RF002
    @Override
    public List<Rol> listarTodos() throws SQLException {
        return rolDAO.listarTodos();
    }

    // RF002
    @Override
    public Rol buscarPorTipo(TipoRol tipo) throws SQLException {
        Validador.obligatorio(tipo, "tipo de rol");
        return rolDAO.buscarPorTipo(tipo);
    }
}
