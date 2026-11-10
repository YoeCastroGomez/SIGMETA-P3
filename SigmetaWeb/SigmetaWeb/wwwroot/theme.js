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
