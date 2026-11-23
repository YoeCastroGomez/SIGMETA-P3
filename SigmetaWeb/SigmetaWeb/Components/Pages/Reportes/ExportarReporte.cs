using System.Globalization;
using System.IO.Compression;
using System.Security;
using System.Text;
using System.Xml.Linq;

namespace SigmetaWeb.Components.Pages.Reportes;

// RF015: descargas del reporte que se está viendo, además del PDF del Java:
//   - Excel (.xlsx): una hoja por tabla, con encabezado en negrita y los importes como números (se pueden sumar).
//   - XML: el mismo contenido en un XML simple (reporte > seccion > fila > dato), para importarlo en otro sistema.
// Las dos salen de las mismas secciones (las tablas que se ven en pantalla), así que siempre coinciden.
// El .xlsx se arma a mano (es un .zip con XML adentro, formato Office Open XML): no hace falta ninguna librería.
public record SeccionReporte(string Titulo, string[] Columnas, List<object?[]> Filas);

public static class ExportarReporte
{
    // ------------------------------------------------------------------ XML

    public static byte[] Xml(string reporte, DateTime desde, DateTime hasta, IEnumerable<SeccionReporte> secciones)
    {
        var raiz = new XElement("reporte",
            new XAttribute("sistema", "SIGMETA"),
            new XAttribute("tipo", reporte),
            new XAttribute("desde", desde.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture)),
            new XAttribute("hasta", hasta.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture)),
            new XAttribute("generado", DateTime.Now.ToString("yyyy-MM-ddTHH:mm:ss", CultureInfo.InvariantCulture)),
            new XAttribute("moneda", "PEN"));
        foreach (var s in secciones)
        {
            var seccion = new XElement("seccion", new XAttribute("nombre", s.Titulo), new XAttribute("filas", s.Filas.Count));
            foreach (var fila in s.Filas)
            {
                var f = new XElement("fila");
                for (int i = 0; i < s.Columnas.Length; i++)
                {
                    var v = i < fila.Length ? fila[i] : null;
                    f.Add(new XElement("dato", new XAttribute("columna", s.Columnas[i]), Texto(v, invariante: true)));
                }
                seccion.Add(f);
            }
            raiz.Add(seccion);
        }
        var doc = new XDocument(new XDeclaration("1.0", "utf-8", null), raiz);
        using var ms = new MemoryStream();
        using (var w = System.Xml.XmlWriter.Create(ms, new System.Xml.XmlWriterSettings { Indent = true, Encoding = new UTF8Encoding(false) }))
        {
            doc.Save(w);
        }
        return ms.ToArray();
    }

    // ------------------------------------------------------------------ Excel (.xlsx)

    public static byte[] Excel(string tituloReporte, DateTime desde, DateTime hasta, IEnumerable<SeccionReporte> secciones)
    {
        var hojas = secciones.ToList();
        using var ms = new MemoryStream();
        using (var zip = new ZipArchive(ms, ZipArchiveMode.Create, true))
        {
            Escribir(zip, "[Content_Types].xml", TiposContenido(hojas.Count));
            Escribir(zip, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                "</Relationships>");
            Escribir(zip, "xl/workbook.xml", Libro(hojas));
            Escribir(zip, "xl/_rels/workbook.xml.rels", RelacionesLibro(hojas.Count));
            Escribir(zip, "xl/styles.xml", Estilos());
            for (int i = 0; i < hojas.Count; i++)
            {
                Escribir(zip, $"xl/worksheets/sheet{i + 1}.xml", Hoja(tituloReporte, desde, hasta, hojas[i]));
            }
        }
        return ms.ToArray();
    }

    private static string Hoja(string tituloReporte, DateTime desde, DateTime hasta, SeccionReporte s)
    {
        var sb = new StringBuilder();
        sb.Append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.Append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        // ancho de cada columna según su texto más largo (entre 10 y 60 caracteres)
        sb.Append("<cols>");
        for (int c = 0; c < s.Columnas.Length; c++)
        {
            int largo = s.Columnas[c].Length;
            foreach (var f in s.Filas)
            {
                if (c < f.Length) largo = Math.Max(largo, Texto(f[c], invariante: false).Length);
            }
            sb.Append($"<col min=\"{c + 1}\" max=\"{c + 1}\" width=\"{Math.Clamp(largo + 2, 10, 60)}\" customWidth=\"1\"/>");
        }
        sb.Append("</cols><sheetData>");
        int r = 1;
        Fila(sb, r++, new object?[] { $"SIGMETA · {tituloReporte}" }, estilo: 1);
        Fila(sb, r++, new object?[] { $"{s.Titulo} · del {desde:dd/MM/yyyy} al {hasta:dd/MM/yyyy} · importes en soles (S/)" }, estilo: 0);
        r++;
        Fila(sb, r++, s.Columnas.Cast<object?>().ToArray(), estilo: 2);
        foreach (var f in s.Filas)
        {
            Fila(sb, r++, f, estilo: 0);
        }
        sb.Append("</sheetData></worksheet>");
        return sb.ToString();
    }

    // estilo: 0 normal (números con 2 decimales), 1 título, 2 encabezado
    private static void Fila(StringBuilder sb, int numero, object?[] celdas, int estilo)
    {
        sb.Append($"<row r=\"{numero}\">");
        for (int c = 0; c < celdas.Length; c++)
        {
            string referencia = Columna(c) + numero;
            switch (celdas[c])
            {
                case null:
                    break;
                case int or long when estilo == 0:
                    sb.Append($"<c r=\"{referencia}\" s=\"4\"><v>{Convert.ToInt64(celdas[c], CultureInfo.InvariantCulture)}</v></c>");
                    break;
                case double or decimal when estilo == 0:
                    var n = Convert.ToDouble(celdas[c], CultureInfo.InvariantCulture);
                    sb.Append($"<c r=\"{referencia}\" s=\"3\"><v>{n.ToString("0.############", CultureInfo.InvariantCulture)}</v></c>");
                    break;
                default:
                    sb.Append($"<c r=\"{referencia}\" t=\"inlineStr\" s=\"{estilo}\"><is><t xml:space=\"preserve\">{SecurityElement.Escape(Texto(celdas[c], invariante: false))}</t></is></c>");
                    break;
            }
        }
        sb.Append("</row>");
    }

    private static string Columna(int indice)
    {
        string nombre = "";
        for (int i = indice + 1; i > 0; i = (i - 1) / 26)
        {
            nombre = (char)('A' + (i - 1) % 26) + nombre;
        }
        return nombre;
    }

    private static string Libro(List<SeccionReporte> hojas)
    {
        var usados = new HashSet<string>();
        var sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (int i = 0; i < hojas.Count; i++)
        {
            sb.Append($"<sheet name=\"{SecurityElement.Escape(NombreHoja(hojas[i].Titulo, usados))}\" sheetId=\"{i + 1}\" r:id=\"rId{i + 1}\"/>");
        }
        sb.Append("</sheets></workbook>");
        return sb.ToString();
    }

    // Excel: nombre de hoja de 31 caracteres como máximo, sin : \ / ? * [ ] y sin repetirse
    private static string NombreHoja(string titulo, HashSet<string> usados)
    {
        var limpio = new string(titulo.Where(ch => !":\\/?*[]".Contains(ch)).ToArray()).Trim();
        if (limpio.Length > 31) limpio = limpio[..31].Trim();
        var nombre = limpio.Length == 0 ? "Hoja" : limpio;
        for (int n = 2; !usados.Add(nombre.ToUpperInvariant()); n++)
        {
            var sufijo = $" ({n})";
            nombre = (limpio.Length + sufijo.Length > 31 ? limpio[..(31 - sufijo.Length)] : limpio) + sufijo;
        }
        return nombre;
    }

    private static string RelacionesLibro(int hojas)
    {
        var sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 1; i <= hojas; i++)
        {
            sb.Append($"<Relationship Id=\"rId{i}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet{i}.xml\"/>");
        }
        sb.Append($"<Relationship Id=\"rId{hojas + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        sb.Append("</Relationships>");
        return sb.ToString();
    }

    private static string TiposContenido(int hojas)
    {
        var sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
            "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 1; i <= hojas; i++)
        {
            sb.Append($"<Override PartName=\"/xl/worksheets/sheet{i}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        sb.Append("</Types>");
        return sb.ToString();
    }

    // 0 normal · 1 título (negrita 13) · 2 encabezado (negrita, fondo gris, borde) · 3 importe #,##0.00 · 4 entero #,##0
    private static string Estilos() =>
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
        "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"#,##0.00\"/></numFmts>" +
        "<fonts count=\"3\"><font><sz val=\"10\"/><name val=\"Calibri\"/></font>" +
        "<font><b/><sz val=\"13\"/><name val=\"Calibri\"/></font>" +
        "<font><b/><sz val=\"10\"/><name val=\"Calibri\"/></font></fonts>" +
        "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>" +
        "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE6E6E6\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>" +
        "<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border>" +
        "<border><left/><right/><top/><bottom style=\"thin\"><color rgb=\"FF808080\"/></bottom><diagonal/></border></borders>" +
        "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
        "<cellXfs count=\"5\">" +
        "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
        "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
        "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>" +
        "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>" +
        "<xf numFmtId=\"3\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>" +
        "</cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>";

    private static void Escribir(ZipArchive zip, string ruta, string contenido)
    {
        var entrada = zip.CreateEntry(ruta, CompressionLevel.Optimal);
        using var w = new StreamWriter(entrada.Open(), new UTF8Encoding(false));
        w.Write(contenido);
    }

    private static string Texto(object? v, bool invariante) => v switch
    {
        null => "",
        double d => invariante ? d.ToString("0.##", CultureInfo.InvariantCulture) : d.ToString("N2", CultureInfo.GetCultureInfo("es-PE")),
        int or long => invariante ? Convert.ToString(v, CultureInfo.InvariantCulture)! : Convert.ToInt64(v).ToString("N0", CultureInfo.GetCultureInfo("es-PE")),
        IFormattable f => f.ToString(null, CultureInfo.InvariantCulture),
        _ => v.ToString() ?? ""
    };
}
