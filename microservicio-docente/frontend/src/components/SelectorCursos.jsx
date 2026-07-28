// Selector de curso por tarjetas — mismo diseño del módulo "Grados" del SGA principal.
// Nivel 1: tarjetas de grado (solo los grados que el docente tiene asignados).
// Nivel 2: tarjetas de curso dentro del grado (una por asignatura + paralelo,
//          aunque compartan el mismo paralelo, porque son materias distintas).
const HEADER = "#2b3c66";

export default function SelectorCursos({ asignaciones, gradoSel, setGradoSel, onSeleccionar, accion = "Ingresar" }) {
  // Agrupa por grado
  const grados = Object.values(
    asignaciones.reduce((acc, a) => {
      const id = a.grado?.id;
      if (id == null) return acc;
      if (!acc[id]) acc[id] = { id, nombre: a.grado?.nombre, cursos: 0 };
      acc[id].cursos++;
      return acc;
    }, {})
  );
  const cursosDelGrado = asignaciones.filter((a) => a.grado?.id === gradoSel);
  const gradoNombre = grados.find((g) => g.id === gradoSel)?.nombre || "";

  const Tarjeta = ({ badge, titulo, subtitulo, stats, footer, onClick }) => (
    <button
      type="button"
      onClick={onClick}
      className="group text-left bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm hover:shadow-lg hover:border-[#243A76] transition-all duration-200 flex flex-col min-h-[190px]"
    >
      {/* CABECERA OSCURA */}
      <div className="p-4 text-white relative" style={{ backgroundColor: HEADER }}>
        <div className="flex items-center justify-between gap-2 mb-2">
          <span className="bg-white/20 text-white text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded">
            {badge}
          </span>
          <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded bg-emerald-600 text-white">
            ACTIVO
          </span>
        </div>
        <h3 className="text-base font-bold uppercase tracking-wide leading-snug line-clamp-2">{titulo}</h3>
        <p className="text-[11px] text-slate-200 font-medium mt-0.5">{subtitulo}</p>
      </div>

      {/* CUERPO */}
      <div className="p-4 bg-white flex-1 flex flex-col justify-between space-y-3">
        <div className="grid grid-cols-3 gap-2">
          {stats.map((s) => (
            <div key={s.label} className="bg-slate-50 border border-slate-100 rounded-lg p-2 text-center">
              <span className="block text-[10px] font-semibold text-slate-400 uppercase">{s.label}</span>
              <span className={`block text-xs font-bold mt-0.5 ${s.color || "text-slate-700"}`}>{s.valor}</span>
            </div>
          ))}
        </div>
        <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
          <span className="text-xs font-medium text-slate-600 group-hover:text-[#243A76] transition-colors">{footer}</span>
          <span className="text-xs font-bold text-slate-400 group-hover:text-[#243A76] transition-colors">→</span>
        </div>
      </div>
    </button>
  );

  return (
    <div className="bg-slate-50 border border-slate-200 rounded-2xl p-6 shadow-sm mb-4">
      {!gradoSel ? (
        <>
          <div className="flex items-center justify-between mb-5">
            <div>
              <h2 className="text-base font-bold text-slate-800">Mis grados</h2>
              <p className="text-xs text-slate-400 mt-0.5">Selecciona un grado para ver tus cursos asignados</p>
            </div>
            {grados.length > 0 && (
              <span className="text-xs font-semibold px-3 py-1 rounded-full bg-white border border-slate-200 text-slate-600 shadow-2xs">
                {grados.length} grado(s)
              </span>
            )}
          </div>
          {grados.length === 0 ? (
            <div className="p-8 text-center text-slate-400 text-sm">No tienes cursos asignados.</div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {grados.map((g) => (
                <Tarjeta
                  key={g.id}
                  badge="GRADO"
                  titulo={g.nombre}
                  subtitulo="Escuela Provincias Unidas"
                  stats={[
                    { label: "CURSOS", valor: g.cursos },
                    { label: "ACTIVOS", valor: g.cursos },
                    { label: "ESTADO", valor: "Vigente", color: "text-emerald-600" },
                  ]}
                  footer="Abrir cursos"
                  onClick={() => setGradoSel(g.id)}
                />
              ))}
            </div>
          )}
        </>
      ) : (
        <>
          <div className="flex items-center gap-3 mb-5">
            <button
              onClick={() => setGradoSel(null)}
              className="w-8 h-8 rounded-lg border border-slate-200 bg-white text-slate-500 hover:text-[#243A76] hover:border-[#243A76] flex items-center justify-center transition"
              title="Volver a grados"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
            </button>
            <div>
              <h2 className="text-base font-bold text-slate-800">{gradoNombre}</h2>
              <p className="text-xs text-slate-400 mt-0.5">Elige el curso (materia y paralelo)</p>
            </div>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {cursosDelGrado.map((a) => (
              <Tarjeta
                key={a.idAsignacion}
                badge={`Paralelo ${a.paralelo?.letra || "—"}`}
                titulo={a.asignatura?.nombre || "Asignatura"}
                subtitulo={a.grado?.nombre}
                stats={[
                  { label: "PARALELO", valor: a.paralelo?.letra || "—" },
                  { label: "ESTUD.", valor: a.cantidadEstudiantes ?? "—" },
                  { label: "ESTADO", valor: "Activa", color: "text-emerald-600" },
                ]}
                footer={accion}
                onClick={() => onSeleccionar(String(a.idAsignacion))}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
