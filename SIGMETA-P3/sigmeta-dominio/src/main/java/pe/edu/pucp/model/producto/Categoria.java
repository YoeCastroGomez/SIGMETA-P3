package pe.edu.pucp.model.producto;

import java.util.List;
import java.util.ArrayList;

/**
 * Categoria con la que se clasifican los productos (RF005).
 */
public class Categoria {

    private int idCategoria;
    private String nombre;
    private String descripcion;
    private boolean estado;
    private List<Producto> productos;

    public Categoria() {
        this.productos = new ArrayList<>();
    }

    public Categoria(int idCategoria, String nombre, String descripcion, boolean estado) {
        this.idCategoria = idCategoria;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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


    public List<Producto> getProductos() {
        return productos;
    }

    public void setProductos(List<Producto> productos) {
        this.productos = productos;
    }
}
