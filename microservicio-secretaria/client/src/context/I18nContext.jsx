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
