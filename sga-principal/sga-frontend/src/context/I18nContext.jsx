import { createContext, useContext, useState, useEffect } from "react";

const translations = {
  es: {
    // Top Bar & Menú
    "app.title": "Sistema de Gestión Académica",
    "nav.home": "Inicio",
    "nav.dashboard": "Panel Principal",
    "nav.portals": "Portales",
    "nav.grades": "Calificaciones",
    "nav.students": "Estudiantes",
    "nav.attendance": "Asistencias",
    "nav.subjects": "Asignaturas",
    "nav.enrollment": "Matrículas",
    "nav.schedules": "Horarios",
    "nav.audit": "Auditoría",
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
    
    // Acerca de
    "about.title": "Acerca del Sistema de Gestión Académica",
    "about.subtitle": "Plataforma Distribuida de Gestión Escolar v4.0.0",
    "about.institution": "Universidad Técnica Estatal de Quevedo (UTEQ)",
    "about.faculty": "Facultad de Ciencias de la Computación",
    "about.course": "Aplicaciones Distribuidas [20701] - 7mo Semestre",
    "about.client": "Escuela de Educación Básica 'Provincias Unidas'",
    "about.architecture_title": "Arquitectura Distribuida y Tecnologías",
    "about.arch_desc": "Arquitectura políglota de microservicios con gRPC, PostgreSQL Multi-Schema, etcd Raft, HAProxy 2.9, Prometheus y Grafana.",
    "about.team_title": "Equipo de Desarrollo (Equipo BCEL)",
    "about.teacher_title": "Docente Evaluador",
    "about.teacher_name": "Prof. Ing. Gleiston Cicerón Guerrero Ulloa, M.Sc.",
    "about.version": "Versión 4.0.0 (Entrega 4 Final)",
    "about.rights": "Todos los derechos reservados. UTEQ 2026.",
    
    // Configuración
    "settings.title": "Configuración del Sistema",
    "settings.subtitle": "Preferencias de usuario, interfaz y parámetros institucionales",
    "settings.interface": "Preferencias de Interfaz",
    "settings.theme_label": "Tema de la aplicación",
    "settings.lang_label": "Idioma del sistema",
    "settings.account": "Cuenta y Seguridad"
  },
  en: {
    // Top Bar & Menu
    "app.title": "Academic Management System",
    "nav.home": "Home",
    "nav.dashboard": "Dashboard",
    "nav.portals": "Portals",
    "nav.grades": "Grades",
    "nav.students": "Students",
    "nav.attendance": "Attendance",
    "nav.subjects": "Subjects",
    "nav.enrollment": "Enrollments",
    "nav.schedules": "Schedules",
    "nav.audit": "Audit Log",
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
    
    // About
    "about.title": "About Academic Management System",
    "about.subtitle": "Distributed School Management Platform v4.0.0",
    "about.institution": "Technical State University of Quevedo (UTEQ)",
    "about.faculty": "Faculty of Computer Science",
    "about.course": "Distributed Applications [20701] - 7th Semester",
    "about.client": "Elementary School 'Provincias Unidas'",
    "about.architecture_title": "Distributed Architecture & Technologies",
    "about.arch_desc": "Polyglot microservices architecture with gRPC, Multi-Schema PostgreSQL, etcd Raft, HAProxy 2.9, Prometheus and Grafana.",
    "about.team_title": "Development Team (BCEL Team)",
    "about.teacher_title": "Evaluating Professor",
    "about.teacher_name": "Prof. Eng. Gleiston Cicerón Guerrero Ulloa, M.Sc.",
    "about.version": "Version 4.0.0 (Release 4 Final)",
    "about.rights": "All rights reserved. UTEQ 2026.",
    
    // Settings
    "settings.title": "System Settings",
    "settings.subtitle": "User preferences, UI options and institutional parameters",
    "settings.interface": "Interface Preferences",
    "settings.theme_label": "Application Theme",
    "settings.lang_label": "System Language",
    "settings.account": "Account & Security"
  }
};

const I18nContext = createContext();

export function I18nProvider({ children }) {
  const [lang, setLangState] = useState(() => {
    return localStorage.getItem("lang") || "es";
  });

  const setLang = (newLang) => {
    if (translations[newLang]) {
      setLangState(newLang);
      localStorage.setItem("lang", newLang);
    }
  };

  const t = (key, fallback = "") => {
    return translations[lang]?.[key] || translations["es"]?.[key] || fallback || key;
  };

  return (
    <I18nContext.Provider value={{ lang, setLang, t, isEn: lang === "en" }}>
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
