package pe.edu.pucp.sigmeta.boimpl;

import pe.edu.pucp.sigmeta.model.enums.TipoDocumentoIdentidad;

// validaciones comunes de la capa de negocio. Los limites de longitud son los de las tablas.
public final class Validador {

    private static final String PATRON_CORREO = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final String PATRON_TELEFONO = "^[0-9+ -]+$";

    private Validador() {
    }

    public static void obligatorio(Object valor, String campo) {
        if(valor == null){
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
    }

    // devuelve el texto sin espacios al inicio y al final
    public static String textoObligatorio(String valor, String campo, int longitudMaxima) {
        if(valor == null || valor.isBlank()){
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
        return validarLongitud(valor.trim(), campo, longitudMaxima);
    }

    // devuelve null si el texto esta vacio
    public static String textoOpcional(String valor, String campo, int longitudMaxima) {
        if(valor == null || valor.isBlank()){
            return null;
        }
        return validarLongitud(valor.trim(), campo, longitudMaxima);
    }

    public static String correo(String valor) {
        String correo = textoOpcional(valor, "correo", 120);
        if(correo != null && !correo.matches(PATRON_CORREO)){
            throw new IllegalArgumentException("El correo no tiene un formato valido");
        }
        return correo;
    }

    public static String telefono(String valor) {
        String telefono = textoOpcional(valor, "telefono", 20);
        if(telefono != null && !telefono.matches(PATRON_TELEFONO)){
            throw new IllegalArgumentException("El telefono solo puede contener digitos, espacios, '+' y '-'");
        }
        return telefono;
    }

    public static String ruc(String valor) {
        String ruc = textoObligatorio(valor, "RUC", 11);
        // SUNAT: 11 digitos, empieza en 10 (persona natural) o 15, 16, 17, 20 (otros contribuyentes)
        if(!ruc.matches("^(10|15|16|17|20)\\d{9}$")){
            throw new IllegalArgumentException("El RUC debe tener 11 digitos y empezar en 10, 15, 16, 17 o 20");
        }
        return ruc;
    }

    public static String numeroDocumento(TipoDocumentoIdentidad tipo, String valor) {
        obligatorio(tipo, "tipo de documento");
        if(tipo == TipoDocumentoIdentidad.RUC){
            return ruc(valor);
        }
        String numero = textoObligatorio(valor, "numero de documento", 20);
        boolean valido = switch (tipo) {
            case DNI -> numero.matches("^\\d{8}$");
            case CARNET_EXTRANJERIA -> numero.matches("^[A-Za-z0-9]{9,12}$");
            case PASAPORTE -> numero.matches("^[A-Za-z0-9]{6,12}$");
            default -> true;
        };
        if(!valido){
            throw new IllegalArgumentException("El numero de documento no es valido para el tipo " + tipo);
        }
        return numero;
    }

    private static String validarLongitud(String valor, String campo, int longitudMaxima) {
        if(valor.length() > longitudMaxima){
            throw new IllegalArgumentException("El campo " + campo + " admite como maximo "
                    + longitudMaxima + " caracteres");
        }
        return valor;
    }
}
