// Preferencia de apariencia compartida por todas las páginas Blazor.
(function () {
    const key = "sigmeta-theme";
    function preferred() {
        try {
            const saved = localStorage.getItem(key);
            if (saved === "light" || saved === "dark") return saved;
        } catch (_) { }
        return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches
            ? "dark" : "light";
    }
    function apply(theme, persist) {
        const value = theme === "dark" ? "dark" : "light";
        document.documentElement.setAttribute("data-bs-theme", value);
        document.documentElement.style.colorScheme = value;
        if (persist) {
            try { localStorage.setItem(key, value); } catch (_) { }
        }
        return value;
    }
    window.sigmetaTheme = {
        get: function () {
            return document.documentElement.getAttribute("data-bs-theme") || preferred();
        },
        set: function (theme) { return apply(theme, true); }
    };
    apply(preferred(), false);
})();

// UI/UX Pro Max (Forms): al aparecer un error, el foco va al primer campo marcado en rojo (o a la franja del error si
// no es de un campo) y cada campo con error queda enlazado a su mensaje para los lectores de pantalla.
window.sigmetaErrores = {
    enfocar: function (franja) {
        document.querySelectorAll('[aria-invalid="true"]:not(.is-invalid)').forEach(function (e) { e.removeAttribute("aria-invalid"); });
        var invalidos = document.querySelectorAll(".is-invalid");
        invalidos.forEach(function (campo) {
            campo.setAttribute("aria-invalid", "true");
            var mensaje = (campo.id && document.getElementById(campo.id + "-error"))
                || (campo.parentElement && campo.parentElement.querySelector(".error-campo"))
                || (campo.closest(".input-group") && campo.closest(".input-group").parentElement.querySelector(".error-campo"));
            if (mensaje) {
                if (!mensaje.id) mensaje.id = "error-" + Math.random().toString(36).slice(2, 9);
                campo.setAttribute("aria-describedby", mensaje.id);
            }
        });
        var destino = invalidos.length > 0 ? invalidos[0] : franja;
        if (destino && destino.focus) {
            destino.focus({ preventScroll: true });
            destino.scrollIntoView({ block: "center", behavior: "auto" });
        }
    }
};
