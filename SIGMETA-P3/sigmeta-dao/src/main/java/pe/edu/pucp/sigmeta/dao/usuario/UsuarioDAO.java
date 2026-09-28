package pe.edu.pucp.sigmeta.dao.usuario;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import java.sql.SQLException;
import java.util.List;
public interface UsuarioDAO {
        List<Usuario> getAll()throws SQLException;
        void create(Usuario user)throws SQLException;
        void update(Usuario user)throws SQLException;
}
