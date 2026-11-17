package pe.edu.pucp.sigmeta.boimpl.almacen;

/**
 * Reglas numericas compartidas entre recepciones y reversiones.
 * No accede a BD: permite probar los limites antes de modificar stock.
 */
public final class ReglasInventario {
    private static final double EPSILON = 0.000001;

    private ReglasInventario() {}

    public static void validarCantidadRecepcion(double cantidad, double yaRecibida, double comprada) {
        if (!Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad recibida debe ser positiva y finita");
        }
        if (!Double.isFinite(yaRecibida) || yaRecibida < 0
                || !Double.isFinite(comprada) || comprada <= 0) {
            throw new IllegalStateException("Las cantidades historicas de compra no son validas");
        }
        if (cantidad + yaRecibida > comprada + EPSILON) {
            throw new IllegalArgumentException("La recepcion excede la cantidad pendiente");
        }
    }

    public static double convertirAStock(double cantidad, double factor) {
        if (!Double.isFinite(cantidad) || cantidad <= 0
                || !Double.isFinite(factor) || factor <= 0) {
            throw new IllegalArgumentException("La cantidad y el factor de conversion deben ser positivos");
        }
        double stock = cantidad * factor;
        if (!Double.isFinite(stock) || stock <= 0) {
            throw new IllegalArgumentException("La cantidad convertida no es valida");
        }
        return stock;
    }

    public static double ingresar(double actual, double cantidad) {
        if (!Double.isFinite(actual) || actual < 0
                || !Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalStateException("Stock o cantidad de ingreso invalidos");
        }
        double resultado = actual + cantidad;
        if (!Double.isFinite(resultado)) {
            throw new IllegalStateException("El stock resultante no es valido");
        }
        return resultado;
    }

    public static double retirarPorReversion(double actual, double cantidad) {
        if (!Double.isFinite(actual) || actual < 0
                || !Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalStateException("Stock o cantidad de reversion invalidos");
        }
        double resultado = actual - cantidad;
        if (!Double.isFinite(resultado) || resultado < -EPSILON) {
            throw new IllegalStateException("Stock insuficiente para revertir la recepcion");
        }
        return Math.max(0, resultado);
    }
}
