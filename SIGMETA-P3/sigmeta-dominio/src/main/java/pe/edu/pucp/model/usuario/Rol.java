package pe.edu.pucp.model.usuario;

import pe.edu.pucp.model.enums.TipoRol;
import java.util.List;
import java.util.ArrayList;

/**
 * Rol asignable a un usuario del sistema (RF002).
 */
public class Rol {

    private int id;
    private TipoRol tipo;
    private String descripcion;
    private boolean estado;
    private List<Usuario> usuarios;

    public Rol() {
        this.usuarios = new ArrayList<>();
    }

    public Rol(int id, TipoRol tipo, String descripcion, boolean estado) {
        this.id = id;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TipoRol getTipo() {
        return tipo;
    }

    public void setTipo(TipoRol tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }


    public List<Usuario> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = usuarios;
    }
}
