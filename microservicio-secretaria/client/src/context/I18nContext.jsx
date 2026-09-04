import { createContext, useContext, useState, useEffect } from "react";

const translations = {
  es: {
    // Top Bar & Menú
    "app.title": "Secretaría Académica",
    "nav.home": "Inicio",
    "nav.dashboard": "Panel de Secretaría",
    "nav.portals": "Cambiar de Portal",
    "nav.grades": "Grados y Cursos",
    "nav.students": "Estudiantes",
    "nav.attendance": "Consulta de Asistencias",
    "nav.subjects": "Asignaturas",
    "nav.enrollment": "Matrículas",
    "nav.schedules": "Horarios",
    "nav.audit": "Auditoría",
    "nav.reports": "Reportes",
    "nav.settings": "Configuración",
    "nav.about": "Acerca de",
    "nav.logout": "Cerrar sesión",
    "nav.change_password": "Cambiar contraseña",
    "nav.no_period": "Sin período",
    "nav.current_period": "Período Lectivo Activo",
    "nav.notifications": "Notificaciones",
    "nav.no_notifications": "No hay notificaciones pendientes",
    "nav.mark_all_read": "Marcar todas como leídas",
    
    // Tema & Idioma
    "theme.light": "Tema Claro",
    "theme.dark": "Tema Oscuro",
    "lang.es": "Español",
    "lang.en": "English",
    
    // Botones & Acciones
    "btn.save": "Guardar",
    "btn.cancel": "Cancelar",
    "btn.search": "Buscar",
    "btn.new": "Nuevo",
    "btn.edit": "Editar",
    "btn.delete": "Eliminar",
    "btn.export": "Exportar",
    "btn.back": "Regresar",
    
    // Módulos específicos de secretaría
    "grados.title": "Grados y Cursos",
    "grados.subtitle": "grados configurados — Escuela Provincias Unidas",
    "grados.new": "Nuevo Grado",
    "grados.new_parallel": "Nuevo Paralelo",
    "grados.back": "Volver a grados",
    "grados.back_parallels": "Volver a paralelos",
    "grados.no_parallels": "No hay paralelos configurados para este grado.",
    "grados.students_enrolled": "estudiantes matriculados",
    "grados.search_student": "Buscar estudiante por nombre, cédula o código...",
    "grados.active": "ACTIVO",
    "grados.inactive": "INACTIVO",
    "grados.capacity": "Capacidad máx",
    "grados.parallels_count": "PARALELOS",
    "grados.active_count": "ACTIVOS",
    "grados.status": "ESTADO",
    "grados.open_parallels": "Abrir paralelos",

    // Historial Académico
    "historial.title": "Historial Académico",
    "historial.subtitle": "Años cursados, calificaciones anuales, trayectoria escolar y certificaciones",
    "historial.tab_estudiante": "Historial por Estudiante",
    "historial.tab_ano": "Consulta por Año y Grado",
    "historial.search_placeholder": "Buscar por nombres, apellidos, cédula o código...",
    "historial.select_student": "Selecciona o busca un estudiante para consultar su historial",
    "historial.years_enrolled": "Años Cursados",
    "historial.overall_avg": "Promedio Acumulado",
    "historial.promoted_years": "Años Aprobados",
    "historial.academic_status": "Estado Académico",
    "historial.btn_ficha": "Ver Ficha Completa",
    "historial.btn_pdf": "Ficha Estudiantil PDF",
    "historial.btn_print": "Imprimir Historial",
    "historial.view_timeline": "Línea de Tiempo",
    "historial.view_table": "Tabla Detallada",
    "historial.year": "Año Lectivo",
    "historial.grade": "Grado y Paralelo",
    "historial.avg": "Promedio Anual",
    "historial.result": "Resultado",
    "historial.observations": "Observaciones",
    "historial.registered_by": "Registrado Por",
    "historial.certificate": "Certificado Matrícula",
    "historial.report_card": "Libreta de Notas",
    "historial.no_history": "Este estudiante no tiene registros históricos o promociones asentadas.",
    "historial.export_csv": "Exportar Nómina (CSV)",
  },
  en: {
    // Top Bar & Menu
    "app.title": "Academic Secretariat",
    "nav.home": "Home",
    "nav.dashboard": "Secretariat Dashboard",
    "nav.portals": "Change Portal",
    "nav.grades": "Grades & Courses",
    "nav.students": "Students",
    "nav.attendance": "Attendance Query",
    "nav.subjects": "Subjects",
    "nav.enrollment": "Enrollments",
    "nav.schedules": "Schedules",
    "nav.audit": "Audit Log",
    "nav.reports": "Reports",
    "nav.settings": "Settings",
    "nav.about": "About",
    "nav.logout": "Logout",
    "nav.change_password": "Change Password",
    "nav.no_period": "No period",
    "nav.current_period": "Active School Year",
    "nav.notifications": "Notifications",
    "nav.no_notifications": "No pending notifications",
    "nav.mark_all_read": "Mark all as read",
    
    // Theme & Language
    "theme.light": "Light Mode",
    "theme.dark": "Dark Mode",
    "lang.es": "Spanish",
    "lang.en": "English",
    
    // Buttons & Actions
    "btn.save": "Save",
    "btn.cancel": "Cancel",
    "btn.search": "Search",
    "btn.new": "New",
    "btn.edit": "Edit",
    "btn.delete": "Delete",
    "btn.export": "Export",
    "btn.back": "Back",
    
    // Secretariat specific modules
    "grados.title": "Grades & Courses",
    "grados.subtitle": "configured grades — Provincias Unidas School",
    "grados.new": "New Grade",
    "grados.new_parallel": "New Section",
    "grados.back": "Back to grades",
    "grados.back_parallels": "Back to sections",
    "grados.no_parallels": "No sections configured for this grade yet.",
    "grados.students_enrolled": "enrolled students",
    "grados.search_student": "Search student by name, ID or code...",
    "grados.active": "ACTIVE",
    "grados.inactive": "INACTIVE",
    "grados.capacity": "Max capacity",
    "grados.parallels_count": "SECTIONS",
    "grados.active_count": "ACTIVE",
    "grados.status": "STATUS",
    "grados.open_parallels": "Open sections",

    // Academic History
    "historial.title": "Academic History",
    "historial.subtitle": "Enrolled years, annual grades, student trajectory, and official certificates",
    "historial.tab_estudiante": "Student History",
    "historial.tab_ano": "Query by Year & Grade",
    "historial.search_placeholder": "Search by name, surname, national ID or code...",
    "historial.select_student": "Select or search for a student to view academic history",
    "historial.years_enrolled": "Years Enrolled",
    "historial.overall_avg": "Cumulative GPA",
    "historial.promoted_years": "Years Passed",
    "historial.academic_status": "Academic Status",
    "historial.btn_ficha": "View Full Profile",
    "historial.btn_pdf": "Student File PDF",
    "historial.btn_print": "Print Record",
    "historial.view_timeline": "Timeline View",
    "historial.view_table": "Detailed Table",
    "historial.year": "School Year",
    "historial.grade": "Grade & Section",
    "historial.avg": "Annual GPA",
    "historial.result": "Result",
    "historial.observations": "Observations",
    "historial.registered_by": "Registered By",
    "historial.certificate": "Enrollment Certificate",
    "historial.report_card": "Report Card",
    "historial.no_history": "This student has no historical records or promotion results yet.",
    "historial.export_csv": "Export Roster (CSV)",
  }
};

const I18nContext = createContext();

export function I18nProvider({ children }) {
  const [lang, setLang] = useState(() => {
    return localStorage.getItem("sga_lang") || "es";
  });

  useEffect(() => {
    localStorage.setItem("sga_lang", lang);
    document.documentElement.lang = lang;
  }, [lang]);

  const t = (key) => {
    return translations[lang]?.[key] || translations["es"]?.[key] || key;
  };

  return (
    <I18nContext.Provider value={{ lang, setLang, t }}>
      {children}
    </I18nContext.Provider>
  );
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error("useI18n must be used within an I18nProvider");
  }
  return context;
}
