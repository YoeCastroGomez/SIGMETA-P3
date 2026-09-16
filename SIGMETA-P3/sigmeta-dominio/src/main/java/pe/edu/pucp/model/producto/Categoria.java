package pe.edu.pucp.model.producto;

import java.util.List;
import java.util.ArrayList;

/**
 * Categoria con la que se clasifican los productos (RF005).
 */
public class Categoria {

    private int id;
    private String nombre;
    private String descripcion;
    private boolean estado;
    private List<Producto> productos;

    public Categoria() {
        this.productos = new ArrayList<>();
    }

    public Categoria(int id, String nombre, String descripcion, boolean estado) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
