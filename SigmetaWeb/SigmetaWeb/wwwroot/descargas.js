// Descarga en el navegador un archivo que generó el servidor (por ejemplo los reportes PDF de RF015).
// Blazor manda el contenido en base64; aquí se arma el archivo y se descarga con su nombre.
window.sigmetaDescargar = function (nombre, tipo, base64) {
    const binario = atob(base64);
    const bytes = new Uint8Array(binario.length);
    for (let i = 0; i < binario.length; i++) {
        bytes[i] = binario.charCodeAt(i);
    }
    const url = URL.createObjectURL(new Blob([bytes], { type: tipo }));
    const enlace = document.createElement("a");
    enlace.href = url;
    enlace.download = nombre;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
};
