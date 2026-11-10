package pe.edu.pucp.sigmeta.model.cobranza;

/**
 * Totales de las cuentas por cobrar que ve el Administrador (RF009):
 * saldo pendiente de todas las cuentas y saldo de las cuentas vencidas.
 */
public class ResumenCuentasPorCobrar {

    private double totalSaldoPendiente;
    private double totalSaldoVencido;
    private int cuentasConSaldo;
    private int cuentasVencidas;

    public ResumenCuentasPorCobrar() {
    }

    public double getTotalSaldoPendiente() {
        return totalSaldoPendiente;
    }

    public void setTotalSaldoPendiente(double totalSaldoPendiente) {
        this.totalSaldoPendiente = totalSaldoPendiente;
    }

    public double getTotalSaldoVencido() {
        return totalSaldoVencido;
    }

    public void setTotalSaldoVencido(double totalSaldoVencido) {
        this.totalSaldoVencido = totalSaldoVencido;
    }

    public int getCuentasConSaldo() {
        return cuentasConSaldo;
    }

    public void setCuentasConSaldo(int cuentasConSaldo) {
        this.cuentasConSaldo = cuentasConSaldo;
    }

    public int getCuentasVencidas() {
        return cuentasVencidas;
    }

    public void setCuentasVencidas(int cuentasVencidas) {
        this.cuentasVencidas = cuentasVencidas;
    }
}
