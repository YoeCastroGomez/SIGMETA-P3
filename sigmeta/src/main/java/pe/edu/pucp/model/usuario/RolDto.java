package pe.edu.pucp.model.usuario;

import pe.edu.pucp.model.enums.TipoRol;

/**
 * Rol asignable a un usuario del sistema (RF002).
 */
public class RolDto {

    private int idRol;
    private TipoRol tipo;
    private String descripcion;
    private boolean estado;

    public RolDto() {
    }

    public RolDto(int idRol, TipoRol tipo, String descripcion, boolean estado) {
        this.idRol = idRol;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
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

}
