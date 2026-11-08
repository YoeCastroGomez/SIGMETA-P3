package pe.edu.pucp.sigmeta.ws;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

// Esta clase activa el escaneo de servicios REST (igual que en el material del curso).
// Todos los servicios quedan bajo /api, por ejemplo:
// http://localhost:8080/sigmeta-ws/api/clientes
@ApplicationPath("api")
public class RestApplication extends Application {
}
