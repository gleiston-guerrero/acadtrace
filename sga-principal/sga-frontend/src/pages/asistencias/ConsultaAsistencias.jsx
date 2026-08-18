import { useState, useEffect } from "react";
import Layout from "../../components/Layout";
import api from "../../config/axios";

const INSTITUTIONAL_GREEN = "#2E7D32";
const PRIMARY = "#243A76";
const DJANGO_REST = `http://${window.location.hostname}:8081/api/docente`;

export default function ConsultaAsistencias() {
  const [grados, setGrados] = useState([]);
  const [gradoSel, setGradoSel] = useState("1"); // Grado ID
  const [estudiantes, setEstudiantes] = useState([]);
  const [estudianteSel, setEstudianteSel] = useState("TODOS");
  const [loading, setLoading] = useState(false);
  const [tooltipSesion, setTooltipSesion] = useState(null);
  const [modalSesion, setModalSesion] = useState(null);
  const [showScrollTop, setShowScrollTop] = useState(false);

  // CARGAR GRADOS DE LA ESCUELA
  useEffect(() => {
    api.get("/api/grados")
      .then((r) => {
        const lista = r.data || [];
        setGrados(lista);
        if (lista.length > 0) setGradoSel(String(lista[0].idGrado));
      })
      .catch(() => {
        setGrados([
          { idGrado: 1, nombre: "Primero EGB" },
          { idGrado: 2, nombre: "Segundo EGB" },
          { idGrado: 3, nombre: "Tercero EGB" },
          { idGrado: 4, nombre: "Cuarto EGB" },
          { idGrado: 5, nombre: "Quinto EGB" },
          { idGrado: 6, nombre: "Sexto EGB" },
          { idGrado: 7, nombre: "Séptimo EGB" },
          { idGrado: 8, nombre: "Octavo EGB" },
          { idGrado: 9, nombre: "Noveno EGB" },
          { idGrado: 10, nombre: "Décimo EGB" },
        ]);
      });
  }, []);

  // CARGAR ESTUDIANTES DEL GRADO SELECCIONADO
  useEffect(() => {
    if (!gradoSel) return;
    setLoading(true);
    api.get(`/api/estudiantes/grado/${gradoSel}`)
      .then((r) => setEstudiantes(r.data || []))
      .catch(() => setEstudiantes([]))
      .finally(() => setLoading(false));
  }, [gradoSel]);

  useEffect(() => {
    const handleScroll = () => setShowScrollTop(window.scrollY > 200);
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const scrollToTop = () => window.scrollTo({ top: 0, behavior: "smooth" });

  const gradoActual = grados.find((g) => String(g.idGrado) === String(gradoSel)) || { nombre: "Primero EGB" };

  // MATERIAS CON DATOS DE ASISTENCIA Y REGISTRO DIARIO DE CLASES (FIEL A LA CAPTURA DE PANTALLA UTEQ)
  const materiasAsistencia = [
    {
      id: 1,
      codigo: "EGB-101",
      materia: `MATEMÁTICA - [EGB-101] - A - ${gradoActual.nombre.toUpperCase()}`,
      docente: "GUERRERO ULLOA GLEISTON CICERON",
      totalClases: 72,
      presentes: 64,
      faltas: 8,
      justificadas: 0,
      porcentaje: 89,
      sesiones: Array.from({ length: 45 }, (_, i) => ({
        id: i + 1,
        fecha: `2026-05-${String((i % 28) + 1).padStart(2, "0")}`,
        hora: "08:00 a.m.",
        estado: i === 7 || i === 15 || i === 22 || i === 31 || i === 39 ? "FALTA" : "PRESENTE",
        tema: `Sesión ${i + 1}: Operaciones y Resolución de Problemas`,
      })),
    },
    {
      id: 2,
      codigo: "EGB-102",
      materia: `LENGUA Y LITERATURA - [EGB-102] - A - ${gradoActual.nombre.toUpperCase()}`,
      docente: "CRUZ LAZ SONIA TATIANA",
      totalClases: 72,
      presentes: 62,
      faltas: 10,
      justificadas: 1,
      porcentaje: 86,
      sesiones: Array.from({ length: 45 }, (_, i) => ({
        id: i + 1,
        fecha: `2026-05-${String((i % 28) + 1).padStart(2, "0")}`,
        hora: "09:30 a.m.",
        estado: i === 4 || i === 12 || i === 18 || i === 25 || i === 34 || i === 41 ? "FALTA" : (i === 15 ? "JUSTIFICADA" : "PRESENTE"),
        tema: `Sesión ${i + 1}: Lectura Comprensiva y Gramática`,
      })),
    },
    {
      id: 3,
      codigo: "EGB-103",
      materia: `CIENCIAS NATURALES - [EGB-103] - A - ${gradoActual.nombre.toUpperCase()}`,
      docente: "GUANIN FAJARDO JORGE HUMBERTO",
      totalClases: 57,
      presentes: 49,
      faltas: 8,
      justificadas: 0,
      porcentaje: 86,
      sesiones: Array.from({ length: 45 }, (_, i) => ({
        id: i + 1,
        fecha: `2026-05-${String((i % 28) + 1).padStart(2, "0")}`,
        hora: "11:00 a.m.",
        estado: i === 9 || i === 14 || i === 27 || i === 38 ? "FALTA" : "PRESENTE",
        tema: `Sesión ${i + 1}: Ecosistemas y Biodiversidad del Ecuador`,
      })),
    },
    {
      id: 4,
      codigo: "EGB-104",
      materia: `ESTUDIOS SOCIALES - [EGB-104] - A - ${gradoActual.nombre.toUpperCase()}`,
      docente: "CARREÑO SANDOYA STALIN DANIEL",
      totalClases: 60,
      presentes: 47,
      faltas: 13,
      justificadas: 2,
      porcentaje: 78,
      sesiones: Array.from({ length: 45 }, (_, i) => ({
        id: i + 1,
        fecha: `2026-05-${String((i % 28) + 1).padStart(2, "0")}`,
        hora: "11:45 a.m.",
        estado: (i >= 0 && i <= 6) || i === 19 || i === 28 ? "FALTA" : "PRESENTE",
        tema: `Sesión ${i + 1}: Historia Patria y Geografía de las Provincias`,
      })),
    },
    {
      id: 5,
      codigo: "EGB-105",
      materia: `INGLÉS - [EGB-105] - A - ${gradoActual.nombre.toUpperCase()}`,
      docente: "LCDA. PATRICIA MONCAYO",
      totalClases: 50,
      presentes: 47,
      faltas: 3,
      justificadas: 0,
      porcentaje: 94,
      sesiones: Array.from({ length: 45 }, (_, i) => ({
        id: i + 1,
        fecha: `2026-05-${String((i % 28) + 1).padStart(2, "0")}`,
        hora: "07:30 a.m.",
        estado: i === 11 || i === 29 ? "FALTA" : "PRESENTE",
        tema: `Sesión ${i + 1}: English Vocabulary & Grammar Practices`,
      })),
    },
  ];

  return (
    <Layout breadcrumb={["Inicio", "Consulta de Asistencias"]}>
      {/* BANNER OFICIAL CON DISEÑO SGA UTEQ */}
      <div style={{ backgroundColor: INSTITUTIONAL_GREEN }} className="rounded-xl text-white p-4 mb-4 shadow-md flex items-center justify-between flex-wrap gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-white/20 flex items-center justify-center font-black text-white text-lg">
            SGA
          </div>
          <div>
            <h1 className="font-extrabold text-base tracking-wide uppercase">
              SGA | Consulta de Asistencias por Curso y Materia
            </h1>
            <p className="text-xs text-white/80 font-medium">
              Escuela de Educación Básica Provincias Unidas · Período Lectivo 2026 - 2027 PPA
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 bg-black/20 px-3 py-1.5 rounded-lg border border-white/20">
          <span className="text-xs font-bold text-amber-300">REGULAR 2026-2027 PPA</span>
        </div>
      </div>

      {/* BARRA DE FILTROS POR CURSO / PARALELO A Y ESTUDIANTE */}
      <div className="bg-white border border-slate-200 rounded-xl p-4 mb-5 shadow-xs flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-4">
          <div>
            <label className="block text-[10px] font-extrabold text-slate-400 uppercase tracking-wider mb-1">
              Curso / Grado Escolar *
            </label>
            <select
              value={gradoSel}
              onChange={(e) => {
                setGradoSel(e.target.value);
                setEstudianteSel("TODOS");
              }}
              className="border border-slate-300 rounded-lg px-3 py-2 text-xs font-bold text-slate-800 bg-slate-50 focus:outline-none focus:ring-2 focus:ring-emerald-600"
            >
              {grados.map((g) => (
                <option key={g.idGrado} value={g.idGrado}>
                  {g.nombre} — Paralelo A
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[10px] font-extrabold text-slate-400 uppercase tracking-wider mb-1">
              Estudiante Específico (Opcional)
            </label>
            <select
              value={estudianteSel}
              onChange={(e) => setEstudianteSel(e.target.value)}
              className="border border-slate-300 rounded-lg px-3 py-2 text-xs font-semibold text-slate-800 bg-slate-50 focus:outline-none focus:ring-2 focus:ring-emerald-600"
            >
              <option value="TODOS">-- Todos los Estudiantes del Curso --</option>
              {estudiantes.map((est) => (
                <option key={est.idEstudiante} value={est.idEstudiante}>
                  {est.apellidos} {est.nombres} ({est.cedula || "S/C"})
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="text-right">
          <span className="text-xs font-bold text-slate-700 block">
            {gradoActual.nombre} · Paralelo A
          </span>
          <span className="text-[11px] text-slate-400 font-medium">
            {estudiantes.length} estudiantes matriculados activos
          </span>
        </div>
      </div>

      {/* GRILLA OFICIAL DE ASISTENCIA FIEL A LA CAPTURA DE PANTALLA */}
      <div className="bg-white border border-slate-300 rounded-xl overflow-hidden shadow-sm mb-6">
        {/* CABECERA DE TABLA DE MATERIAS */}
        <div className="bg-slate-100 border-b border-slate-300 px-4 py-3 grid grid-cols-12 gap-3 font-extrabold text-[11px] text-slate-700 uppercase tracking-wider">
          <div className="col-span-12 md:col-span-4 flex items-center gap-2">
            <span>MATERIA</span>
          </div>
          <div className="col-span-6 md:col-span-2 text-center">
            <span>% ASISTENCIA</span>
          </div>
          <div className="col-span-6 md:col-span-6 text-right flex items-center justify-end gap-2">
            <svg className="w-4 h-4 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <span>CLASES</span>
          </div>
        </div>

        {/* FILAS POR MATERIA */}
        <div className="divide-y divide-slate-200">
          {materiasAsistencia.map((m) => (
            <div key={m.id} className="p-4 grid grid-cols-12 gap-4 items-center hover:bg-slate-50/80 transition">
              
              {/* COLUMNA 1: MATERIA & DATOS DEL DOCENTE */}
              <div className="col-span-12 md:col-span-4 space-y-2">
                <h3 className="text-xs font-extrabold text-slate-800 leading-snug">
                  {m.materia}
                </h3>

                <p className="text-[11px] text-slate-500 font-semibold flex items-center gap-1">
                  <svg className="w-3.5 h-3.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                  {m.docente}
                </p>

                <p className="text-[10px] text-slate-400 font-medium">
                  de asistencia justificada <span className="font-bold text-slate-600">0%</span>
                </p>

                {/* MINI BOTONES / TARJETAS TOTAL | PRESENTES | FALTAS */}
                <div className="flex items-center gap-1.5 pt-1">
                  <div className="bg-slate-100 border border-slate-200 rounded px-2 py-1 text-center min-w-[42px]">
                    <span className="block text-[10px] font-bold text-slate-800 leading-none">{m.totalClases}</span>
                    <span className="text-[8px] font-extrabold text-slate-400 uppercase tracking-tighter">TOTAL</span>
                  </div>
                  <div className="bg-slate-100 border border-slate-200 rounded px-2 py-1 text-center min-w-[50px]">
                    <span className="block text-[10px] font-bold text-slate-800 leading-none">{m.presentes}</span>
                    <span className="text-[8px] font-extrabold text-slate-400 uppercase tracking-tighter">PRESENTES</span>
                  </div>
                  <div className="bg-slate-100 border border-slate-200 rounded px-2 py-1 text-center min-w-[42px]">
                    <span className="block text-[10px] font-bold text-slate-800 leading-none">{m.faltas}</span>
                    <span className="text-[8px] font-extrabold text-slate-400 uppercase tracking-tighter">FALTAS</span>
                  </div>
                </div>
              </div>

              {/* COLUMNA 2: % ASISTENCIA */}
              <div className="col-span-6 md:col-span-2 text-center flex flex-col items-center justify-center">
                <span className="text-lg font-black text-slate-800 leading-none mb-1">
                  {m.porcentaje}%
                </span>
                <div className="w-12 h-1.5 bg-slate-200 rounded-full overflow-hidden">
                  <div
                    style={{ width: `${m.porcentaje}%`, backgroundColor: m.porcentaje >= 85 ? INSTITUTIONAL_GREEN : (m.porcentaje >= 80 ? "#D97706" : "#DC2626") }}
                    className="h-full rounded-full"
                  />
                </div>
              </div>

              {/* COLUMNA 3: GRILLA CONTINUA DE ICONOS DE CLASES (CIRCULOS VERDES Y ROJOS) */}
              <div className="col-span-12 md:col-span-6">
                <div className="flex flex-wrap gap-1 items-center justify-start max-h-28 overflow-y-auto p-1">
                  {m.sesiones.map((s) => {
                    const esFalta = s.estado === "FALTA";
                    const esJust = s.estado === "JUSTIFICADA";

                    return (
                      <div key={s.id} className="relative group">
                        <button
                          type="button"
                          onClick={() => setModalSesion({ materia: m.materia, docente: m.docente, sesion: s })}
                          onMouseEnter={() => setTooltipSesion({ materia: m.materia, sesion: s })}
                          onMouseLeave={() => setTooltipSesion(null)}
                          className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold text-white transition transform hover:scale-125 ${
                            esFalta
                              ? "bg-rose-600 hover:bg-rose-700"
                              : (esJust ? "bg-amber-500 hover:bg-amber-600" : "bg-emerald-600 hover:bg-emerald-700")
                          }`}
                        >
                          {esFalta ? "✖" : (esJust ? "ℹ" : "✔")}
                        </button>
                      </div>
                    );
                  })}
                </div>
              </div>

            </div>
          ))}
        </div>
      </div>

      {/* MODAL DETALLE DE CLASE */}
      {modalSesion && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full overflow-hidden shadow-2xl border border-slate-200">
            <div style={{ backgroundColor: INSTITUTIONAL_GREEN }} className="p-4 text-white flex items-center justify-between">
              <div>
                <h3 className="font-bold text-sm">Sesión N° {modalSesion.sesion.id}</h3>
                <p className="text-xs text-white/80">{modalSesion.materia}</p>
              </div>
              <button
                onClick={() => setModalSesion(null)}
                className="w-7 h-7 rounded-full bg-white/20 hover:bg-white/30 flex items-center justify-center font-bold text-xs text-white"
              >
                ✕
              </button>
            </div>

            <div className="p-5 space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-2 bg-slate-50 p-3 rounded-lg border border-slate-200 font-mono">
                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-sans">Fecha</span>
                  <span className="font-bold text-slate-700">{modalSesion.sesion.fecha}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-sans">Hora</span>
                  <span className="font-bold text-slate-700">{modalSesion.sesion.hora}</span>
                </div>
              </div>

              <div>
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">Docente</span>
                <span className="font-semibold text-slate-800">{modalSesion.docente}</span>
              </div>

              <div>
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">Contenido de la Sesión</span>
                <p className="text-slate-700 bg-slate-50 p-2.5 rounded-lg border border-slate-200">
                  {modalSesion.sesion.tema}
                </p>
              </div>

              <div className="flex items-center justify-between pt-2">
                <span className="font-bold text-slate-600">Estado de Asistencia:</span>
                <span className={`px-3 py-1 rounded-full text-white font-extrabold ${modalSesion.sesion.estado === "FALTA" ? "bg-rose-600" : "bg-emerald-600"}`}>
                  {modalSesion.sesion.estado === "FALTA" ? "✖ FALTA REGISTRADA" : "✔ PRESENTE"}
                </span>
              </div>
            </div>

            <div className="p-3 bg-slate-50 border-t border-slate-200 text-right">
              <button
                onClick={() => setModalSesion(null)}
                className="bg-slate-700 hover:bg-slate-800 text-white text-xs font-bold px-4 py-1.5 rounded-lg transition"
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* SCROLL TO TOP */}
      {showScrollTop && (
        <button
          onClick={scrollToTop}
          style={{ backgroundColor: INSTITUTIONAL_GREEN }}
          className="fixed bottom-6 right-6 w-10 h-10 rounded-full text-white shadow-xl flex items-center justify-center hover:scale-110 transition z-40"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 10l7-7 7 7M12 3v18" />
          </svg>
        </button>
      )}
    </Layout>
  );
}
